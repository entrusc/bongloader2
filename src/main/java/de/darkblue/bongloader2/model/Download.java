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
package de.darkblue.bongloader2.model;

import com.moebiusgames.xdata.DataKey;
import com.moebiusgames.xdata.DataMarshaller;
import com.moebiusgames.xdata.DataNode;
import com.moebiusgames.xdata.ListDataKey;
import de.darkblue.bongloader2.model.Download.Part;
import de.darkblue.bongloader2.model.Recording.MovieFile;
import de.darkblue.bongloader2.model.Recording.MovieFile.Quality;
import de.darkblue.bongloader2.model.data.AbstractUpdateable;
import de.darkblue.bongloader2.model.data.Storable;
import de.darkblue.bongloader2.model.data.UpdateableListener;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * One download of a recording with all information about
 * how much has already been downloaded, the priority,
 * quality, etc ..
 *
 * @author Florian Frankenberger
 */
public class Download extends AbstractUpdateable<Download> implements Storable<Download>, Serializable, UpdateableListener<Part> {

    private final int id;
    private final int recordingId;

    private long totalBytes;

    private int priority;
    private final Quality quality;
    private File targetFile;

    private List<Part> parts = Collections.synchronizedList(new ArrayList<Part>());
    private volatile boolean downloaded = false;
    private volatile Date downloadedAt = null;

    private transient volatile boolean downloading = false;
    private transient volatile boolean invalid = false;
    private transient Recording recording;

    private transient volatile Long eta = null;
    private transient volatile boolean downloadProblems = false;

    public static class Part extends AbstractUpdateable<Part> implements Storable<Part>, Serializable {
        private volatile long offset;
        private volatile long length;
        private volatile long loaded = 0;

        public Part(long offset, long length) {
            this.offset = offset;
            this.length = length;
        }

        public long getLength() {
            return length;
        }

        public void setLength(long length) {
            this.length = length;
            notifyOnUpdate();
        }

        public long getLoaded() {
            return loaded;
        }

        public void setLoaded(long loaded) {
            this.loaded = loaded;
            notifyOnUpdate();
        }

        public long getOffset() {
            return offset;
        }

        public void setOffset(long offset) {
            this.offset = offset;
            notifyOnUpdate();
        }

        @Override
        public int getId() {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public void update(Part other) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public String toString() {
            return "Part{" + "offset=" + offset + ", length=" + length + ", loaded=" + loaded + '}';
        }

    }

    public Download(int id, int priority, int recordingId, Quality quality, File targetFile) {
        this.id = id;
        this.priority = priority;
        this.quality = quality;
        this.recording = null;
        this.recordingId = recordingId;
        this.targetFile = targetFile;
    }

    public Download(int id, int priority, Recording recording, Quality quality, File targetFile) {
        this.id = id;
        this.priority = priority;
        this.quality = quality;
        this.recording = recording;
        this.recordingId = recording.getId();
        this.targetFile = targetFile;
    }

    /**
     * special deserialization treatment
     * @param in
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        for (Part part : this.parts) {
            part.addListener(this);
        }
    }

    public void setTargetFile(File targetFile) {
        this.targetFile = targetFile;
        this.notifyOnUpdate();
    }

    public void setRecording(Recording recording) {
        this.recording = recording;
    }

    public int getRecordingId() {
        return this.recordingId;
    }

    public boolean startedDownloading() {
        return this.parts.size() > 0;
    }

    public long getDownloadedBytes() {
        long loaded = 0;
        for (Part part : parts) {
            loaded += part.getLoaded();
        }
        return loaded;
    }

    public long getTotalBytes() {
        return this.totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
        notifyOnUpdate();
    }

    public MovieFile getMovieFile() {
        return recording.getMovieFile(this.quality);
    }

    public List<Part> getParts() {
        return parts;
    }

    public Recording getRecording() {
        return recording;
    }

    /**
     * the file which will contain the downloaded data once
     * the download is finished.
     *
     * @return
     */
    public File getTargetFile() {
        return targetFile;
    }

    /**
     * the file where to store the data in while downloading,
     * this is renamed to the target file after the download
     * is finished.
     *
     * @return
     */
    public File getDownloadFile() {
        return new File(targetFile.getParentFile(), targetFile.getName() + ".download");
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
        this.downloadedAt = new Date(); //= now!
        this.notifyOnUpdate();
    }

    public void setDownloadProblems(boolean downloadProblems) {
        this.downloadProblems = downloadProblems;
        this.notifyOnUpdate();
    }

    public boolean hasDownloadProblems() {
        return downloadProblems;
    }

    public Date getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloading(boolean downloading) {
        this.downloading = downloading;
        this.notifyOnUpdate();
    }

    public void clearParts() {
        for (Part part : this.parts) {
            part.removeListener(this);
        }
        this.parts.clear();
    }

    public void addPart(Part part) {
        this.parts.add(part);
        part.addListener(this);
    }

    public boolean isDownloading() {
        return this.downloading;
    }

    public boolean isDownloaded() {
        return this.downloaded;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
        this.notifyOnUpdate();
    }

    public Long getEta() {
        return eta;
    }

    public void setEta(Long eta) {
        this.eta = eta;
        this.notifyOnUpdate();
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void update(Download other) {
        //can't update a download
    }

    private transient volatile long lastUpdate = 0L;

    @Override
    public void onUpdate(Part part) {
        if (System.currentTimeMillis() - lastUpdate < 50) {
            return;
        }
        lastUpdate = System.currentTimeMillis();
        notifyOnUpdate();
    }

    @Override
    public String toString() {
        return "Download{" + "id=" + id + ", recordingId=" + recordingId + ", priority="
                + priority + ", quality=" + quality + ", targetFile=" + targetFile + ", parts="
                + parts.size() + ", downloaded=" + downloaded + ", downloading=" + downloading + ", invalid="
                + invalid + '}';
    }

    public static class PartMarshaller implements DataMarshaller<Part> {
        private static final DataKey<Long> KEY_OFFSET = DataKey.create("offset", Long.class);
        private static final DataKey<Long> KEY_LENGTH = DataKey.create("length", Long.class);
        private static final DataKey<Long> KEY_LOADED = DataKey.create("loaded", Long.class, 0L);

        @Override
        public String getDataClassName() {
            return "bl2.download.part";
        }

        @Override
        public Class<Part> getDataClass() {
            return Part.class;
        }

        @Override
        public List<DataMarshaller<?>> getRequiredMarshallers() {
            return Collections.EMPTY_LIST;
        }

        @Override
        public DataNode marshal(Part part) {
            DataNode dataNode = new DataNode();
            dataNode.setObject(KEY_OFFSET, part.offset);
            dataNode.setObject(KEY_LENGTH, part.length);
            dataNode.setObject(KEY_LOADED, part.loaded);
            return dataNode;
        }

        @Override
        public Part unMarshal(DataNode dataNode) {
            final long offset = dataNode.getMandatoryObject(KEY_OFFSET);
            final long length = dataNode.getMandatoryObject(KEY_LENGTH);
            final Part part = new Part(offset, length);
            final long loaded = dataNode.getObject(KEY_LOADED);
            part.loaded = loaded;
            return part;
        }

    }

    public static class DownloadMarshaller implements DataMarshaller<Download> {

        private static final DataKey<Integer> KEY_ID = DataKey.create("id", Integer.class);
        private static final DataKey<Integer> KEY_RECORDING_ID = DataKey.create("recording id", Integer.class);
        private static final DataKey<Long> KEY_TOTAL_BYTES = DataKey.create("total bytes", Long.class);
        private static final DataKey<Integer> KEY_PRIORITY = DataKey.create("priority", Integer.class);
        private static final DataKey<String> KEY_QUALITY = DataKey.create("quality", String.class);
        private static final ListDataKey<Part> KEY_PARTS = ListDataKey.create("parts", Part.class);
        private static final DataKey<Boolean> KEY_DOWNLOADED = DataKey.create("downloaded", Boolean.class);
        private static final DataKey<Date> KEY_DOWNLOADED_AT = DataKey.create("downloaded at", Date.class, null);
        private static final DataKey<String> KEY_TARGET_FILE = DataKey.create("target file", String.class);

        private static final List<DataMarshaller<?>> REQUIRED_MARSHALLERS = new ArrayList<DataMarshaller<?>>(
                Arrays.asList(new DataMarshaller<?>[] {
                    new PartMarshaller()
                })
        );

        @Override
        public String getDataClassName() {
            return "bl2.download";
        }

        @Override
        public Class<Download> getDataClass() {
            return Download.class;
        }

        @Override
        public List<DataMarshaller<?>> getRequiredMarshallers() {
            return REQUIRED_MARSHALLERS;
        }

        @Override
        public DataNode marshal(Download download) {
            DataNode dataNode = new DataNode();
            dataNode.setObject(KEY_ID, download.getId());
            dataNode.setObject(KEY_RECORDING_ID, download.getRecordingId());
            dataNode.setObject(KEY_TOTAL_BYTES, download.getTotalBytes());
            dataNode.setObject(KEY_PRIORITY, download.getPriority());
            dataNode.setObject(KEY_QUALITY, download.quality.name());
            dataNode.setObjectList(KEY_PARTS, download.parts);
            dataNode.setObject(KEY_DOWNLOADED, download.isDownloaded());
            dataNode.setObject(KEY_DOWNLOADED_AT, download.getDownloadedAt());
            dataNode.setObject(KEY_TARGET_FILE, download.getTargetFile().toString());
            return dataNode;
        }

        @Override
        public Download unMarshal(DataNode node) {
            final int id = node.getMandatoryObject(KEY_ID);
            final int recordingId = node.getMandatoryObject(KEY_RECORDING_ID);
            final long totalBytes = node.getMandatoryObject(KEY_TOTAL_BYTES);
            final int priority = node.getMandatoryObject(KEY_PRIORITY);
            final Quality quality = Quality.valueOf(node.getMandatoryObject(KEY_QUALITY));
            final List<Part> parts = node.getMandatoryObjectList(KEY_PARTS);
            final boolean downloaded = node.getMandatoryObject(KEY_DOWNLOADED);
            final Date downloadedAt = node.getObject(KEY_DOWNLOADED_AT);
            final File targetFile = new File(node.getMandatoryObject(KEY_TARGET_FILE));
            Download download = new Download(id, priority, recordingId, quality, targetFile);
            download.totalBytes = totalBytes;
            for (Part part : parts) {
                download.parts.add(part);
                part.addListener(download);
            }
            download.downloaded = downloaded;
            download.downloadedAt = downloadedAt;
            return download;
        }

    }

}
