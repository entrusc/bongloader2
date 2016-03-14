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
import java.io.Serializable;
import de.darkblue.bongloader2.model.Recording.MovieFile.Quality;
import de.darkblue.bongloader2.model.data.AbstractUpdateable;
import de.darkblue.bongloader2.model.data.Storable;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

/**
 * Contains all information about a particular recoding
 *
 * @author Florian Frankenberger
 */
public class Recording extends AbstractUpdateable<Recording> implements Storable<Recording>, Serializable {

    public static class MovieFile implements Serializable {

        public static enum Quality {

            HD("HD - High Definition"),
            HQ("HQ - High Quality"),
            NQ("NQ - Normal Quality"),
            UNKNOWN("? - Unknown Quality");

            private String humanReadable;

            private Quality(String humanReadable) {
                this.humanReadable = humanReadable;
            }

            @Override
            public String toString() {
                return this.humanReadable;
            }

            public static Quality parse(String raw) {
                Quality result = Quality.UNKNOWN;
                for (Quality quality : values()) {
                    if (quality.name().equalsIgnoreCase(raw)) {
                        result = quality;
                        break;
                    }
                }

                return result;
            }
        }

        private URL downloadUrl;
        private Quality quality;
        private boolean autoEnqueued = false;
        private boolean autoDownloaded = false;

        public MovieFile() {
        }

        public MovieFile(URL downloadUrl, Quality quality) {
            this.downloadUrl = downloadUrl;
            this.quality = quality;
        }

        public URL getDownloadUrl() {
            return downloadUrl;
        }

        public void setDownloadUrl(URL downloadUrl) {
            this.downloadUrl = downloadUrl;
        }

        public boolean isAutoEnqueued() {
            return autoEnqueued;
        }

        public void setAutoEnqueued(boolean downloaded) {
            this.autoEnqueued = downloaded;
        }

        public boolean isAutoDownloaded() {
            return autoDownloaded;
        }

        public void setAutoDownloaded(boolean autoDownloaded) {
            this.autoDownloaded = autoDownloaded;
        }

        public Quality getQuality() {
            return quality;
        }

        public void setQuality(Quality quality) {
            this.quality = quality;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MovieFile other = (MovieFile) obj;
            if (this.downloadUrl != other.downloadUrl && (this.downloadUrl == null || !this.downloadUrl.equals(other.downloadUrl))) {
                return false;
            }
            if (this.quality != other.quality) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 53 * hash + (this.downloadUrl != null ? this.downloadUrl.hashCode() : 0);
            hash = 53 * hash + (this.quality != null ? this.quality.hashCode() : 0);
            return hash;
        }


    }
    private int id;
    private String title;
    private String subtitle;
    private String description;
    private String genre;
    private Integer seriesSeason;
    private Integer seriesCount;
    private Integer seriesNumber;
    private String channel;
    private Date start;
    private Date firstSeen = new Date();
    private long duration;
    private URL thumbUrl;
    private boolean markedDeleted = false;
    private Map<Quality, MovieFile> files = new EnumMap<Quality, MovieFile>(Quality.class);

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public void markDeleted(boolean delete) {
        this.markedDeleted = delete;
        notifyOnUpdate();
    }

    /**
     * returns if this recording has been marked for deletation
     * by the user. If all downloads are finished the recording will
     * be deleted.
     *
     * @return
     */
    public boolean markedDeleted() {
        return markedDeleted;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getSeriesCount() {
        return seriesCount;
    }

    public void setSeriesCount(Integer seriesCount) {
        this.seriesCount = seriesCount;
    }

    public Integer getSeriesNumber() {
        return seriesNumber;
    }

    public void setSeriesNumber(Integer seriesNumber) {
        this.seriesNumber = seriesNumber;
    }

    public Integer getSeriesSeason() {
        return seriesSeason;
    }

    public void setSeriesSeason(Integer seriesSeason) {
        this.seriesSeason = seriesSeason;
    }

    public boolean isSeries() {
        return this.getSeriesNumber() != null
                && this.getSeriesSeason() != null;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public URL getThumbUrl() {
        return thumbUrl;
    }

    public void setThumbUrl(URL thumbUrl) {
        this.thumbUrl = thumbUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Date getFirstSeen() {
        return firstSeen;
    }

    public void addFileURL(Quality quality, MovieFile movieFile) {
        this.files.put(quality, movieFile);
    }

    public Set<Quality> getMovieFileQualities() {
        return this.files.keySet();
    }

    public boolean hasMovieFile(Quality quality) {
        return this.files.containsKey(quality) && this.getMovieFile(quality) != null;
    }

    public MovieFile getMovieFile(Quality quality) {
        return this.files.get(quality);
    }

    public Collection<MovieFile> getAllMovieFiles() {
        return this.files.values();
    }

    @Override
    public void update(Recording newRecording) {
        this.title = newRecording.getTitle();
        this.subtitle = newRecording.getSubtitle();
        this.channel = newRecording.getChannel();
        this.description = newRecording.getDescription();
        this.start = newRecording.getStart();
        this.duration = newRecording.getDuration();
        this.thumbUrl = newRecording.getThumbUrl();

        this.seriesCount = newRecording.getSeriesCount();
        this.seriesNumber = newRecording.getSeriesNumber();
        this.seriesSeason = newRecording.getSeriesSeason();

        if (this.getFirstSeen() == null || this.getFirstSeen().after(newRecording.getFirstSeen())) {
            this.firstSeen = newRecording.getFirstSeen();
        }

        for (Entry<Quality, MovieFile> file : newRecording.files.entrySet()) {
            if (!this.hasMovieFile(file.getKey()) || this.getMovieFile(file.getKey()) == null) {
                this.files.put(file.getKey(), file.getValue());
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Recording other = (Recording) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 83 * hash + this.id;
        return hash;
    }

    @Override
    public String toString() {
        return "Recording{" + "id=" + id + ", title=" + title + ", channel=" + channel + ", start=" + start + '}';
    }

    public static class MovieFileMarhsaller implements DataMarshaller<MovieFile> {

        private static final DataKey<URL> KEY_DOWNLOAD_URL = DataKey.create("download url", URL.class);
        private static final DataKey<String> KEY_QUALITY = DataKey.create("quality", String.class);
        private static final DataKey<Boolean> KEY_AUTO_ENQUEUED = DataKey.create("auto enqueued", Boolean.class, (Boolean) false);
        private static final DataKey<Boolean> KEY_AUTO_DOWNLOADED = DataKey.create("auto downloaded", Boolean.class, (Boolean) false);

        @Override
        public String getDataClassName() {
            return "bl2.recording.moviefile";
        }

        @Override
        public Class<MovieFile> getDataClass() {
            return MovieFile.class;
        }

        @Override
        public List<DataMarshaller<?>> getRequiredMarshallers() {
            return Collections.EMPTY_LIST;
        }

        @Override
        public DataNode marshal(MovieFile movieFile) {
            DataNode dataNode = new DataNode();
            dataNode.setObject(KEY_DOWNLOAD_URL, movieFile.getDownloadUrl());
            dataNode.setObject(KEY_QUALITY, movieFile.getQuality().name());
            dataNode.setObject(KEY_AUTO_ENQUEUED, movieFile.autoEnqueued);
            dataNode.setObject(KEY_AUTO_DOWNLOADED, movieFile.autoDownloaded);
            return dataNode;
        }

        @Override
        public MovieFile unMarshal(DataNode dataNode) {
            final URL downloadURL = dataNode.getMandatoryObject(KEY_DOWNLOAD_URL);
            final Quality quality = Quality.valueOf(dataNode.getMandatoryObject(KEY_QUALITY));
            MovieFile movieFile = new MovieFile(downloadURL, quality);
            movieFile.autoEnqueued = dataNode.getObject(KEY_AUTO_ENQUEUED);
            movieFile.autoDownloaded = dataNode.getObject(KEY_AUTO_DOWNLOADED);
            return movieFile;
        }

    }

    public static class RecordingMarshaller implements DataMarshaller<Recording> {

        private static final DataKey<Integer> KEY_ID = DataKey.create("id", Integer.class);
        private static final DataKey<String> KEY_TITLE = DataKey.create("title", String.class, "");
        private static final DataKey<String> KEY_SUBTITLE = DataKey.create("subtitle", String.class, "");
        private static final DataKey<String> KEY_DESCRIPTION = DataKey.create("description", String.class, "");
        private static final DataKey<String> KEY_GENRE = DataKey.create("genre", String.class, "");
        private static final DataKey<Integer> KEY_SERIES_SEASON = DataKey.create("series season", Integer.class);
        private static final DataKey<Integer> KEY_SERIES_COUNT = DataKey.create("series count", Integer.class);
        private static final DataKey<Integer> KEY_SERIES_NUMBER = DataKey.create("series number", Integer.class);
        private static final DataKey<String> KEY_CHANNEL = DataKey.create("channel", String.class);
        private static final DataKey<Date> KEY_START = DataKey.create("start", Date.class);
        private static final DataKey<Date> KEY_FIRST_SEEN = DataKey.create("first seen", Date.class);
        private static final DataKey<Long> KEY_DURATION = DataKey.create("duration", Long.class, 0L);
        private static final DataKey<URL> KEY_THUMB_URL = DataKey.create("thumb url", URL.class);
        private static final DataKey<Boolean> KEY_MARKED_DELETED = DataKey.create("marked deleted", Boolean.class);
        private static final ListDataKey<MovieFile> KEY_MOVIE_FILES = ListDataKey.create("movie files", MovieFile.class);

        private static final List<DataMarshaller<?>> REQUIRED_MARSHALLERS = new ArrayList<DataMarshaller<?>>(
                Arrays.asList(new DataMarshaller<?>[] {
                    new MovieFileMarhsaller()
                })
        );

        @Override
        public String getDataClassName() {
            return "bl2.recording";
        }

        @Override
        public Class<Recording> getDataClass() {
            return Recording.class;
        }

        @Override
        public List<DataMarshaller<?>> getRequiredMarshallers() {
            return REQUIRED_MARSHALLERS;
        }

        @Override
        public DataNode marshal(Recording recording) {
            DataNode node = new DataNode();
            node.setObject(KEY_ID, recording.getId());
            node.setObject(KEY_TITLE, recording.getTitle());
            node.setObject(KEY_SUBTITLE, recording.getSubtitle());
            node.setObject(KEY_DESCRIPTION, recording.getDescription());
            node.setObject(KEY_GENRE, recording.getGenre());
            node.setObject(KEY_SERIES_SEASON, recording.getSeriesSeason());
            node.setObject(KEY_SERIES_COUNT, recording.getSeriesCount());
            node.setObject(KEY_SERIES_NUMBER, recording.getSeriesNumber());
            node.setObject(KEY_CHANNEL, recording.getChannel());
            node.setObject(KEY_START, recording.getStart());
            node.setObject(KEY_FIRST_SEEN, recording.getFirstSeen());
            node.setObject(KEY_DURATION, recording.getDuration());
            node.setObject(KEY_THUMB_URL, recording.getThumbUrl());
            node.setObject(KEY_MARKED_DELETED, recording.markedDeleted());
            node.setObjectList(KEY_MOVIE_FILES, new ArrayList<MovieFile>(recording.files.values()));
            return node;
        }

        @Override
        public Recording unMarshal(DataNode node) {
            Recording recording = new Recording();
            recording.setId(node.getMandatoryObject(KEY_ID));
            recording.setTitle(node.getObject(KEY_TITLE));
            recording.setSubtitle(node.getObject(KEY_SUBTITLE));
            recording.setDescription(node.getObject(KEY_DESCRIPTION));
            recording.setGenre(node.getObject(KEY_GENRE));
            recording.setSeriesSeason(node.getObject(KEY_SERIES_SEASON));
            recording.setSeriesCount(node.getObject(KEY_SERIES_COUNT));
            recording.setSeriesNumber(node.getObject(KEY_SERIES_NUMBER));
            recording.setChannel(node.getObject(KEY_CHANNEL));
            recording.setStart(node.getObject(KEY_START));
            recording.firstSeen = node.getObject(KEY_FIRST_SEEN);
            recording.setDuration(node.getObject(KEY_DURATION));
            recording.setThumbUrl(node.getObject(KEY_THUMB_URL));
            recording.markedDeleted = node.getObject(KEY_MARKED_DELETED);

            final List<MovieFile> movieFiles = node.getObjectList(KEY_MOVIE_FILES);
            for (MovieFile movieFile : movieFiles) {
                recording.addFileURL(movieFile.getQuality(), movieFile);
            }

            return recording;
        }

    }

}
