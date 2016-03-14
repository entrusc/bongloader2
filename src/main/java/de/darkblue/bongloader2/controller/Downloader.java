/* 
 * Copyright (C) 2016 Florian Frankenberger.
 *
 * This library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License 
 * along with this library; if not, see <http://www.gnu.org/licenses/>.
 */
package de.darkblue.bongloader2.controller;

import de.darkblue.bongloader2.Configuration;
import de.darkblue.bongloader2.ConfigurationKey;
import de.darkblue.bongloader2.model.Download;
import de.darkblue.bongloader2.model.Download.Part;
import de.darkblue.bongloader2.utils.Utils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Florian Frankenberger
 */
public class Downloader extends Thread {

    private static final Logger LOGGER = Logger.getLogger(DownloadController.class.getCanonicalName());
    private final Download download;
    private final CruiseController cruiseController;
    private RandomAccessFile randomAccessFile;

    private volatile boolean done = false;
    private volatile Exception error = null;

    private volatile boolean shutdown = false;
    private final Configuration config;

    private final List<PartDownloader> partDownloaders = new ArrayList<PartDownloader>();

    private class PartDownloader extends Thread {
        private final Part part;
        private volatile boolean running = false;
        private volatile boolean finished = false;

        public PartDownloader(Part part) {
            this.part = part;
            this.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LOGGER.log(Level.SEVERE, "Problem in downloader", e);
                }

            });
        }

        @Override
        public void run() {
            while (!shutdown && part.getLoaded() < part.getLength()) {
                final URL downloadUrl = download.getMovieFile().getDownloadUrl();
                //try to open a connection
                try {
                    HttpURLConnection connection = null;
                    boolean registeredDownloadWithCruiseController = false;
                    try {
                        connection = (HttpURLConnection) downloadUrl.openConnection();
                        connection.setRequestProperty("User-Agent", "BongLoader2 " + config.get(ConfigurationKey.VERSION));
                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(10000);
                        final long fromBytes = part.getOffset() + part.getLoaded();
                        final long toBytes = part.getOffset() + part.getLength() - 1;

                        connection.setRequestProperty("Range", "bytes=" + fromBytes + "-" + toBytes);

                        connection.connect();
                        if (connection.getResponseCode() == 206) {
                            this.running = true;

                            boolean allRunning = true;
                            for (PartDownloader downloader : partDownloaders) {
                                allRunning &= (downloader.running || downloader.finished);
                            }

                            if (allRunning) {
                                download.setDownloadProblems(false);
                            }

                            //now we register this download with the cruise controller
                            cruiseController.addDownload();
                            registeredDownloadWithCruiseController = true;

                            LOGGER.log(Level.FINE, "{0}  receiving partial: {1}", new Object[] {this.toString(), connection.getHeaderField("Content-Range")});

                            final byte[] buffer = new byte[2048];
                            final int twentyTimesBuffer = 20 * buffer.length;

                            InputStream in = connection.getInputStream();

                            long time = System.currentTimeMillis();

                            long sleepTime = 0L;

                            int read = 0;
                            int readInTime = 0;

                            long blockTime = System.currentTimeMillis();
                            long readInBlock = 0;

                            do {
                                if (sleepTime == 0) {
                                    read = in.read(buffer);
                                    if (read > 0) {
                                        synchronized (randomAccessFile) {
                                            randomAccessFile.seek(part.getOffset() + part.getLoaded());
                                            randomAccessFile.write(buffer, 0, read);
                                        }
                                        part.setLoaded(part.getLoaded() + read);
                                        readInTime += read;
                                        readInBlock += read;
                                    }
                                    if (readInBlock >= twentyTimesBuffer) {
                                        long aTime = System.currentTimeMillis() - blockTime;

                                        //so it took "aTime" time to load 20 times 2048 bytes
                                        //but it should have taken us
                                        if (cruiseController.hasSpeedLimit()) {
                                            float milliSecondsPerByte = 1000f / cruiseController.getMeanSpeedLimit();
                                            float timeItShouldHaveTaken = milliSecondsPerByte * 20 * buffer.length;

                                            float timeDifference = timeItShouldHaveTaken - aTime;

                                            if (timeDifference > 0) {
                                                sleepTime = (long) Math.round(timeDifference);
                                            }
                                        }

                                        blockTime = System.currentTimeMillis();
                                        readInBlock = 0;
                                    }

                                } else {
                                    try {
                                        final long timeToSleep = sleepTime > 100 ? 100 : sleepTime;
                                        Thread.sleep(timeToSleep);
                                        sleepTime -= timeToSleep;
                                        if (sleepTime == 0) {
                                            blockTime = System.currentTimeMillis();
                                        }
                                    } catch (InterruptedException e) {

                                    }
                                }

                                //measurement for cruise controller
                                final long timePassed = System.currentTimeMillis() - time;
                                if (timePassed >= 300) {
                                    final float currentSpeed = readInTime / ((float) timePassed / 1000f);
                                    time = System.currentTimeMillis();
                                    cruiseController.addSpeed(currentSpeed);
                                    readInTime = 0;
                                }

                            } while (part.getLoaded() < part.getLength() && read > -1 && !shutdown);
                        } else {
                            throw new IOException("Probelm retrieving a part - response code was: " + connection.getResponseCode());
                        }
                    } finally {
                        this.running = false;
                        if (registeredDownloadWithCruiseController) {
                            cruiseController.removeDownload();
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }

                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, this.toString() + " could not connect to " + downloadUrl, e);
                    download.setDownloadProblems(true);
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e2) {
                        //ignore
                    }
                }
            }
            finished = true;

            LOGGER.log(Level.FINE, "{0} stopped loading part {1}", new Object[] {this.toString(), part});
        }

        @Override
        public String toString() {
            return Downloader.this.toString() + " > " + "PartDownloader{partoffset=" + this.part.getOffset() + "}";
        }

    }

    public Downloader(CruiseController cruiseController, Download download, Configuration config) {
        this.download = download;
        this.cruiseController = cruiseController;
        this.config = config;
    }

    @Override
    public void run() {
        try {
            if (!this.download.startedDownloading() || !this.download.getDownloadFile().exists()) {
                this.initDownload();
            }
            try {
                randomAccessFile = new RandomAccessFile(this.download.getDownloadFile(), "rw");

                //enqueue all parts
                for (Part part : this.download.getParts()) {
                    final PartDownloader partDownloader = new PartDownloader(part);
                    partDownloaders.add(partDownloader);
                }

                //start all parts
                for (PartDownloader downloader : this.partDownloaders) {
                    downloader.start();
                }

                //and now wait for them to finish
                for (PartDownloader partDownloader : partDownloaders) {
                    try {
                        partDownloader.join();
                    } catch (InterruptedException e) {
                        //ignore
                    }
                }
            } finally {
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
            }
        } catch (Exception e) {
            download.setDownloadProblems(true);
            LOGGER.log(Level.WARNING, "Problem while downloading file " + download, e);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            this.error = e;
        }

        //if the download stopped before it is finished we assume some error / or
        //a controlled shutdown ...
        if (this.error == null
                && this.download.getDownloadedBytes() < this.download.getTotalBytes()) {
            this.error = new IllegalStateException("Shutdown before file was finished.");
        }

        this.done = true;
    }

    public void shutdown() {
        this.shutdown = true;
    }

    /**
     * if this returns true the downloader is done - either because
     * an error occured or because the download has finished.
     * @return
     */
    public boolean isDone() {
        return done;
    }

    public boolean hasError() {
        return this.error != null;
    }

    public Exception getError() {
        return this.error;
    }

    private void initDownload() throws IOException {
        final int partsAmount = config.getAsInt(ConfigurationKey.DOWNLOAD_PARTS);

        //1. retrieve total file size
        final long totalFileSize = download.getTotalBytes();
        final long partSize = totalFileSize / partsAmount;

        //2. split filesize in parts
        download.clearParts();
        long position = 0L;
        for (int i = 1; i <= partsAmount; ++i) {
            //the floored part size or the rest of the file
            final long currentPartSize = i < partsAmount
                    ? partSize : totalFileSize - position;
            final Part part = new Part(position, currentPartSize);
            position += partSize;
            download.addPart(part);
        }

        //3. create folders (if inexistent)
        File targetFile = this.download.getTargetFile();
        targetFile.getParentFile().mkdirs();

        //4. rename file if it already exists (or the download file)
        File downloadFile = this.download.getDownloadFile();
        final String originalFileName = targetFile.getName();
        int counter = 0;
        while (targetFile.exists() || downloadFile.exists()) {
            targetFile = new File(targetFile.getParentFile(),
                originalFileName.substring(0, originalFileName.lastIndexOf('.')) + "(" + ++counter + ")"
                        + Utils.getFileNameExt(originalFileName));
            this.download.setTargetFile(targetFile);
            downloadFile = this.download.getDownloadFile();
        }
    }

    public Download getDownload() {
        return this.download;
    }

    @Override
    public String toString() {
        return "Downloader{downloadid=" + this.download.getId() + "}";
    }



}
