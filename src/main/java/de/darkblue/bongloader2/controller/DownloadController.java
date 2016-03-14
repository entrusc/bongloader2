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

import de.darkblue.bongloader2.exception.ReportableException;
import de.darkblue.bongloader2.*;
import de.darkblue.bongloader2.iface.ApiInterface;
import de.darkblue.bongloader2.iface.ApiInterfaceException;
import de.darkblue.bongloader2.iface.ApiInterfaceV3;
import de.darkblue.bongloader2.model.AutodownloadLevel;
import de.darkblue.bongloader2.model.Download;
import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.model.Recording.MovieFile;
import de.darkblue.bongloader2.model.Recording.MovieFile.Quality;
import de.darkblue.bongloader2.model.data.Callback;
import de.darkblue.bongloader2.model.data.StorableList;
import de.darkblue.bongloader2.model.data.Updateable;
import de.darkblue.bongloader2.model.data.UpdateableListener;
import de.darkblue.bongloader2.utils.ToolBox;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages the download of all recordings
 *
 * @author Florian Frankenberger
 */
public class DownloadController extends Thread implements Updateable<DownloadController>, ConfigurationUpdateListener {

    private static final Logger LOGGER = Logger.getLogger(DownloadController.class.getCanonicalName());

    private final Application application;
    private final ApiInterface api = new ApiInterfaceV3();

    private final StorableList<Recording> recordingList;
    private final StorableList<Download> downloadList;

    private final List<Downloader> downloaders = new ArrayList<Downloader>();
    private final CruiseController cruiseController;

    private volatile boolean shutdown = false;
    private long lastRecordingsUpdate = 0L;
    private long lastDownloadsUpdate = 0L;

    private ControllerState controllerState = DownloadController.ControllerState.UNKNOWN;
    private final Set<UpdateableListener<DownloadController>> listeners = new HashSet<UpdateableListener<DownloadController>>();

    private static final Comparator<Download> DOWNLOADS_PRIORITY_COMPARATOR = new Comparator<Download>() {

        @Override
        public int compare(Download o1, Download o2) {
            int order = Integer.valueOf(o1.getPriority()).compareTo(o2.getPriority());
            if (order == 0) {
                order = Integer.valueOf(o1.getId()).compareTo(o2.getId());
            }
            return order;
        }

    };

    public static enum ControllerState {
        UNKNOWN,
        NO_USERNAME_AND_OR_PASSWORD,
        CONNECTED,
        ERROR
    }

    private final Set<Recording> removedRecordings = new HashSet<Recording>();

    public DownloadController(Application application, StorableList<Recording> recordingsList, StorableList<Download> downloadList) {
        this.application = application;
        this.recordingList = recordingsList;
        this.downloadList = downloadList;

        this.cruiseController = new CruiseController(application);
        application.getConfig().addConfigurationUpdateListener(this);

        LOGGER.info("Using Api Interface: " + this.api.getVersion());
    }

    /**
     * shuts down the controller and waits
     * for it to actually stop.
     */
    public void shutdown() {
        if (!shutdown) {
            //first stop all downloads
            this.cruiseController.shutdown();

            this.shutdown = true;
            try {
                this.join();
            } catch (InterruptedException e) {
                //ignore
            }
        }
    }

    public CruiseController getCruiseController() {
        return cruiseController;
    }

    @Override
    public void run() {
        final Configuration config = application.getConfig();

        try {
            while (!shutdown) {
                try {
                    if (System.currentTimeMillis() - lastRecordingsUpdate > config.getAsInt(ConfigurationKey.RECORDLIST_UPDATE_TIME)) {
                        lastRecordingsUpdate = System.currentTimeMillis();
                        if (config.isConfigured(ConfigurationKey.USERNAME) && config.isConfigured(ConfigurationKey.PASSWORD)) {
                            try {
                                downloadRecordingsList();
                            } catch (Error e) {
                                throw e;
                            } catch (Exception e) {
                                this.setControllerState(ControllerState.ERROR);
                                throw e;
                            }

                            deleteMarkedRecordings();
                            enqueueAutomaticDownloads();
                        } else {
                            this.setControllerState(ControllerState.NO_USERNAME_AND_OR_PASSWORD);
                            LOGGER.info("Could not update recordings because username and/or password are not set yet.");
                        }
                    }
                } catch (Error e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Exception occured", e);
                }

                try {
                    updateDownloads();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Exception occured", e);
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    //ignore
                }
            }

            stopAllDownloaders();

        } catch (Exception e) {
            shutdown = true;
            LOGGER.log(Level.SEVERE, "Problem in download controller - thread now dead!", e);
            application.shutdownApplication(true);
        }
    }

    public ControllerState getControllerState() {
        return controllerState;
    }

    public List<Download> getPriorizisedDownloadList() {
        return this.downloadList.getAll(DOWNLOADS_PRIORITY_COMPARATOR);
    }

    /**
     * checks all recordings for a deletation marking. If this marking is found, it
     * is checked if there are no more incomplete downloads associated with this
     * recording. If this is also true, the recording gets deleted (as requested
     * by the user before).
     */
    private void deleteMarkedRecordings() throws ApiInterfaceException, ReportableException {
        final long gracePeriod = application.getConfig().getAsLong(ConfigurationKey.DELETE_AFTER_DOWNLOAD_GRACE_PERIOD) * 1000L;
        final Date now = new Date();

        for (Recording recording : recordingList.getAll()) {
            if (recording.markedDeleted()) {
                final long time = now.getTime() - recording.getFirstSeen().getTime();

                //now check if there is one download in the list associated with this recording
                if (!hasRunningDownloads(recording) && time >= gracePeriod) {
                    LOGGER.log(Level.INFO, "Deleting {0}", recording);
                    removeRecording(recording);
                }
            }
        }
    }

    public boolean hasRunningDownloads(Recording recording) {
        for (Download download : downloadList.getAll()) {
            if (download.getRecording().equals(recording) && !download.isDownloaded()) {
                return true;
            }
        }
        return false;
    }

    /**
     * checks what downloads are running / should be running. Schedules
     * new downloads.
     */
    private void updateDownloads() throws Exception {
        //sort the enqueued downloads by priority
        List<Download> sortedDownloads = getPriorizisedDownloadList();

        //some maintainance
        for (Download download : sortedDownloads) {
            if (!recordingList.contains(download.getRecordingId())) {
                this.downloadList.delete(download);
                LOGGER.log(Level.FINE, "Removed {0} because it was invalid", download);
            } else {
                if (download.getTotalBytes() <= 0) {
                    //not initialized - we do this now:
                    try {
                        download.setTotalBytes(ToolBox.getFileSize(application.getConfig(), download.getMovieFile().getDownloadUrl()));
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Could not get file size for {0}", download);
                    }
                }

                //check if a downloaded file could not be renamed for some reasons, and check
                //if it is possible now
                if (download.isDownloaded()
                        && download.getDownloadFile().exists()
                        && !download.getTargetFile().exists()) {
                    //rename download file to target file
                    download.getDownloadFile().renameTo(download.getTargetFile());
                }

            }

        }

        sortedDownloads = this.downloadList.getAll(DOWNLOADS_PRIORITY_COMPARATOR);

        boolean changes = false;

        //check if there are downloaders that have finished
        final int downloadSlots = cruiseController.isPaused()
                ? 0 // if paused we simulate 0 download slots
                : application.getConfig().getAsInt(ConfigurationKey.MAX_SIMULTANEOUS_DOWNLOADS);

        Set<Download> downloadingDownloads = new HashSet<Download>();
        int counter = 0;
        for (int i = 0; i < sortedDownloads.size(); ++i) {
            final Download sortedDownload = sortedDownloads.get(i);
            if (!sortedDownload.isDownloaded()) {
                if (++counter > downloadSlots) {
                    break;
                }
                downloadingDownloads.add(sortedDownload);
            }
        }

        final List<Downloader> toRemove = new ArrayList<Downloader>();
        for (Downloader downloader : downloaders) {
            if (downloader.isDone()) {
                if (downloader.hasError()) {
                    LOGGER.log(Level.INFO, "{0} -> Error ({1})", new Object[] {downloader.getDownload(), downloader.getError().getMessage()});
                    downloader.getDownload().setDownloaded(false);

                    if (downloader.getDownload().getRecording().markedDeleted()) {
                        //for security reasons we remove the delete marking if there is a problem
                        //with the download, because the user could delete that download and then
                        //the recording would get deleted as well
                        downloader.getDownload().getRecording().markDeleted(false);
                    }
                } else {
                    LOGGER.log(Level.INFO, "{0} finished.", downloader.getDownload());

                    if (!downloader.getDownload().isDownloaded()) {
                        final MovieFile movieFile = downloader.getDownload().getMovieFile();
                        if (movieFile.isAutoEnqueued()) {
                            movieFile.setAutoDownloaded(true);
                        }
                        downloader.getDownload().setDownloaded(true);

                        //rename download file to target file
                        downloader.getDownload().getDownloadFile().renameTo(downloader.getDownload().getTargetFile());
                        LOGGER.log(Level.INFO, "Renamed {0} to {1}.",
                                new Object[] { downloader.getDownload().getDownloadFile(), downloader.getDownload().getTargetFile() });

                        recordingList.save();
                    }

                }
                downloader.getDownload().setDownloading(false);
                toRemove.add(downloader);

                changes = true;
            } else
                //if a download is downloading that should not be downloading we stop it here
                if (!downloadingDownloads.contains(downloader.getDownload())) {
                    downloader.shutdown();
                } else
                    //if a recording was removed we shutdown the downloader so it gets removed
                    //on the next round
                    if (!recordingList.contains(downloader.getDownload().getRecordingId())) {
                        downloader.shutdown();
                    }
        }
        downloaders.removeAll(toRemove);

        //now pick the n ones from the list that are not downloading
        final int freeSlots = downloadSlots - downloaders.size();

        int index = 0;
        for (int i = 0; i < freeSlots; ++i) {
            while (index < sortedDownloads.size()
                    && (sortedDownloads.get(index).getTotalBytes() == 0 || sortedDownloads.get(index).isDownloading() || sortedDownloads.get(index).isDownloaded())) {
                index++;
            }
            if (index < sortedDownloads.size()) {
                final Download download = sortedDownloads.get(index);
                checkLegacyFilename(download);
                final Downloader downloader = new Downloader(cruiseController, download, application.getConfig());
                downloaders.add(downloader);
                download.setDownloading(true);
                downloader.start();
                LOGGER.log(Level.INFO, "Now downloading {0}", download);
                changes = true;
            } else {
                break; //no more downloads available to download
            }
        }

        if (changes
                || System.currentTimeMillis() - lastDownloadsUpdate > 10000) {
            this.downloadList.save();
            lastDownloadsUpdate = System.currentTimeMillis();
        }
    }

    /**
     * in earlier days we downloaded the data directly to
     * the target file (this has obviously some disadvantages),
     * so we check here if we have not downloaded the file yet
     * but still are already downloading to the target file
     * instead of the (temporary) download file. If so, we
     * rename the file before we start to download.
     *
     * @param download
     */
    private void checkLegacyFilename(Download download) {
        if (!download.isDownloaded() && download.getTargetFile().exists()) {
            download.getTargetFile().renameTo(download.getDownloadFile());

            LOGGER.log(Level.INFO, "Legacy file {0} renamed to {1}.",
                    new Object[] { download.getTargetFile(), download.getDownloadFile()});
        }
    }

    private void stopAllDownloaders() {
        LOGGER.info("Stopping all downloads");
        for (Downloader downloader : downloaders) {
            downloader.shutdown();
        }

        for (Downloader downloader : downloaders) {
            try {
                downloader.join();
            } catch (InterruptedException e) {
                //ignore
            }
        }
        LOGGER.info("Stopping all downloads ... done");
    }

    /**
     * checks if there are more recordings that can be automatically placed for downloading
     */
    private void enqueueAutomaticDownloads() {
        final Configuration config = application.getConfig();
        final AutodownloadLevel autodownloadLevel = AutodownloadLevel.parse(config.get(ConfigurationKey.AUTODOWNLOAD_LEVEL));
        boolean inserted = false;

        for (Recording recording : recordingList.getAll()) {
            boolean addedDownload = false;
            Set<Quality> levelsToDownload = autodownloadLevel.getQualitiesToDownload(recording);
            for (Quality quality : levelsToDownload) {
                if (recording.hasMovieFile(quality)) {
                    MovieFile movieFile = recording.getMovieFile(quality);
                    if (!movieFile.isAutoEnqueued()) {
                        addedDownload = enqueueDownload(recording, quality, false, true);
                        if (addedDownload) {
                            LOGGER.log(Level.WARNING, "Autodownloading: enqueued {0} in {1}", new Object[] {recording, quality});
                            movieFile.setAutoEnqueued(true);
                        }
                        inserted |= addedDownload;
                    }
                }
            }

            if (addedDownload && config.getAsBoolean(ConfigurationKey.DELETE_AFTER_DOWNLOAD)) {
                recording.markDeleted(true);
            }
        }

        if (inserted) {
            recordingList.save();
            downloadList.save();
        }
    }

    /**
     * enqueues a download for the recording of the given quality level. The recording
     * is not marked as autoenqueued.
     *
     * @param recording
     * @param quality
     * @return
     */
    public boolean enqueueDownload(Recording recording, Quality quality) {
        return enqueueDownload(recording, quality, true, false);
    }

    private synchronized void removeRecording(Recording recording) throws ApiInterfaceException, ReportableException {
        if (!this.removedRecordings.contains(recording)) {
            this.api.deleteRecording(application.getConfig().get(ConfigurationKey.USERNAME),
                    application.getConfig().get(ConfigurationKey.PASSWORD), recording);
            downloadRecordingsList();

            this.removedRecordings.add(recording);
        } else {
            LOGGER.log(Level.WARNING, "Tried to remove {0} again", recording);
        }

    }

    private synchronized boolean enqueueDownload(Recording recording, Quality quality, boolean saveList, boolean autoEnqueued) {
        final Configuration config = application.getConfig();
        final String targetFileNamePattern = config.get(ConfigurationKey.FILE_NAME_PATTERN);
        final MovieFile movieFile = recording.getMovieFile(quality);
        if (movieFile != null) {
            File file = ToolBox.getTargetFile(application.getWorkingDirectory(), targetFileNamePattern, recording, quality);

            final int id = downloadList.getNewPrimaryId();
            final int maxPrio = downloadList.getMax(new Callback<Integer, Download>() {
                @Override
                public Integer call(Download value) {
                    return value.getPriority();
                }
            }, 0);

            final Download download = new Download(id, maxPrio + 1, recording, quality, file);
            this.downloadList.add(download);

            movieFile.setAutoEnqueued(autoEnqueued);
            if (saveList) {
                recordingList.save();
                downloadList.save();
            }
            return true;
        }
        return false;
    }


    private synchronized void downloadRecordingsList() throws ApiInterfaceException, ReportableException {
        final List<Recording> newRecordings = this.api.downloadRecordingsList(application.getConfig().get(ConfigurationKey.USERNAME),
                    application.getConfig().get(ConfigurationKey.PASSWORD));
        recordingList.update(newRecordings, true);

        LOGGER.log(Level.FINE, "Updated/inserted {0} recordings", newRecordings.size());
        this.setControllerState(ControllerState.CONNECTED);

    }

    @Override
    public void onUpdate(ConfigurationKey key) {
        switch (key) {
            case USERNAME:
            case PASSWORD:
            case DOWNLOAD_PARTS:
            case DOWNLOAD_PAUSED:
            case MAX_DOWNLOAD_SPEED:
            case MAX_SIMULTANEOUS_DOWNLOADS:
            case QUALITY_LEVELS_TO_DOWNLOAD:
            case RECORDLIST_UPDATE_TIME:
                this.lastRecordingsUpdate = new Date().getTime() - (application.getConfig().getAsInt(ConfigurationKey.RECORDLIST_UPDATE_TIME) + 5);
                this.interrupt();
                break;
            default:
        }
    }

    @Override
    public synchronized void addListener(UpdateableListener<DownloadController> listener) {
        this.listeners.add(listener);
    }

    @Override
    public synchronized void removeListener(UpdateableListener<DownloadController> listener) {
        this.listeners.remove(listener);
    }

    private void setControllerState(ControllerState controllerState) {
        if (this.controllerState != controllerState) {
            LOGGER.log(Level.INFO, "State changed to {0}", controllerState);
        }
        this.controllerState = controllerState;
        notifyOnUpdate();
    }

    private void notifyOnUpdate() {
        for (UpdateableListener<DownloadController> listener : this.listeners) {
            listener.onUpdate(this);
        }
    }
}
