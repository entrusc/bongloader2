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
package de.darkblue.bongloader2.server;

import de.darkblue.bongloader2.Application;
import de.darkblue.bongloader2.ConfigurationKey;
import de.darkblue.bongloader2.controller.DownloadController;
import de.darkblue.bongloader2.controller.DownloadController.ControllerState;
import de.darkblue.bongloader2.model.Download;
import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.model.Recording.MovieFile;
import de.darkblue.bongloader2.model.Recording.MovieFile.Quality;
import de.darkblue.bongloader2.utils.ToolBox;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;

/**
 * A simple web service based on jetty
 * that enables third party apps to control
 * the headless bongloader.
 *
 * @author Florian Frankenberger
 */
public class WebServiceServer {

    private static final Logger LOGGER = Logger.getLogger(WebServiceServer.class.getCanonicalName());

    private static final String PATH_JSON_PREFIX = "/json/";

    private final Server server;

    public WebServiceServer(final Application application, final DownloadController downloadController) {
        this.server = new Server(5833);

        JsonHandler jsonHandler = new JsonHandler();

        jsonHandler.putMapping(PATH_JSON_PREFIX + "get_status", JsonEmpty.class, new JsonHandler.JsonRequestHandler<JsonEmpty>() {
            @Override
            public Object call(JsonEmpty value) {
                JsonStatusResult status = new JsonStatusResult();
                status.paused = downloadController.getCruiseController().isPaused();
                status.state = downloadController.getControllerState();
                status.downloads = application.getDownloadList().getSize();
                status.recordings = application.getRecordingList().getSize();
                status.currentDownloadSpeed = downloadController.getCruiseController().getMeanSpeed();
                return status;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "get_recordings", JsonEmpty.class, new JsonHandler.JsonRequestHandler<JsonEmpty>() {
            @Override
            public Object call(JsonEmpty value) {
                JsonRecordingsResult result = new JsonRecordingsResult();
                result.recordings = marshalRecordings(application.getRecordingList().getAll());
                return result;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "get_downloads", JsonEmpty.class, new JsonHandler.JsonRequestHandler<JsonEmpty>() {
            @Override
            public Object call(JsonEmpty value) {
                JsonDownloadsResult result = new JsonDownloadsResult();
                result.downloads = marshalDownloads(application.getDownloadList().getAll());
                return result;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "enqueue_download", JsonEnqueueDownloadRequest.class, new JsonHandler.JsonRequestHandler<JsonEnqueueDownloadRequest>() {
            @Override
            public Object call(JsonEnqueueDownloadRequest value) {
                Recording recording = application.getRecordingList().getById(value.recordingId);
                if (recording != null) {
                    downloadController.enqueueDownload(recording, Quality.parse(value.quality));
                } else {
                    throw new IllegalArgumentException("Recording with id " + value.recordingId + " is unknown");
                }
                return null;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "remove_download", JsonRemoveDownloadRequest.class, new JsonHandler.JsonRequestHandler<JsonRemoveDownloadRequest>() {
            @Override
            public Object call(JsonRemoveDownloadRequest value) {
                Download download = application.getDownloadList().getById(value.downloadId);
                if (download != null) {
                    application.getDownloadList().delete(download);
                } else {
                    throw new IllegalArgumentException("Download with id " + value.downloadId + " is unknown");
                }
                return null;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "set_pause", JsonSetPauseRequest.class, new JsonHandler.JsonRequestHandler<JsonSetPauseRequest>() {
            @Override
            public Object call(JsonSetPauseRequest value) {
                downloadController.getCruiseController().setPaused(value.pause);
                return null;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "set_filename_pattern", JsonSetFilenamePatternRequest.class, new JsonHandler.JsonRequestHandler<JsonSetFilenamePatternRequest>() {
            @Override
            public Object call(JsonSetFilenamePatternRequest value) {
                if (ToolBox.checkFilenamePattern(value.filenamePattern, application.getWorkingDirectory())) {
                    application.getConfig().set(ConfigurationKey.FILE_NAME_PATTERN, value.filenamePattern);
                } else {
                    throw new IllegalArgumentException("Filename pattern \"" + value.filenamePattern + "\" does not generate valid paths");
                }
                return null;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "test_filename_pattern", JsonSetFilenamePatternRequest.class, new JsonHandler.JsonRequestHandler<JsonSetFilenamePatternRequest>() {
            @Override
            public Object call(JsonSetFilenamePatternRequest value) {
                JsonTestFilenamePatternResult result = new JsonTestFilenamePatternResult();
                result.valid = false;
                if (ToolBox.checkFilenamePattern(value.filenamePattern, application.getWorkingDirectory())) {
                    result.fileName = ToolBox.getTargetFile(application.getWorkingDirectory(), value.filenamePattern, ToolBox.DEMO_RECORDING, Quality.NQ).getAbsolutePath();
                    result.fileNameSeries = ToolBox.getTargetFile(application.getWorkingDirectory(), value.filenamePattern, ToolBox.DEMO_RECORDING_SERIES, Quality.HQ).getAbsolutePath();
                    result.valid = true;
                }
                return result;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "set_speed_limit", JsonSetSpeedLimitRequest.class, new JsonHandler.JsonRequestHandler<JsonSetSpeedLimitRequest>() {
            @Override
            public Object call(JsonSetSpeedLimitRequest value) {
                downloadController.getCruiseController().setSpeedLimit(value.speedLimit);
                return null;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "mark_recording_deleted", JsonMarkRecordingDeletedRequest.class, new JsonHandler.JsonRequestHandler<JsonMarkRecordingDeletedRequest>() {
            @Override
            public Object call(JsonMarkRecordingDeletedRequest value) {
                Recording recording = application.getRecordingList().getById(value.recordingId);
                if (recording != null) {
                    recording.markDeleted(true);
                } else {
                    throw new IllegalArgumentException("Recording with id " + value.recordingId + " is unknown");
                }
                return null;
            }
        });

        jsonHandler.putMapping(PATH_JSON_PREFIX + "shutdown", JsonEmpty.class, new JsonHandler.JsonRequestHandler<JsonEmpty>() {
            @Override
            public Object call(JsonEmpty value) {
                LOGGER.info("Shutdown requested via web service - will occur in 2 seconds.");
                new Thread() {
                    @Override
                    public void run() {
                        try { Thread.sleep(2000); } catch (InterruptedException e) { /* ignore */ }
                        application.shutdownApplication(false);
                    }
                }.start();
                return null;
            }
        });

        HandlerList handlerList = new HandlerList();
        handlerList.setHandlers(new Handler[] { jsonHandler });
        server.setHandler(handlerList);

        try {
            server.start();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private List<JsonRecordingResult> marshalRecordings(List<Recording> recordings) {
        List<JsonRecordingResult> results = new ArrayList<JsonRecordingResult>();
        for (Recording recording : recordings) {
            results.add(marshal(recording));
        }
        return results;
    }

    private List<JsonDownloadResult> marshalDownloads(List<Download> downloads) {
        List<JsonDownloadResult> results = new ArrayList<JsonDownloadResult>();
        for (Download download : downloads) {
            results.add(marshal(download));
        }
        return results;
    }

    private JsonRecordingResult marshal(Recording recording) {
        JsonRecordingResult result = new JsonRecordingResult();
        result.id = recording.getId();
        result.start = recording.getStart();
        result.title = recording.getTitle();
        result.channel = recording.getChannel();
        result.description = recording.getDescription();
        result.genre = recording.getGenre();
        result.seriesCount = recording.getSeriesCount();
        result.seriesNumber = recording.getSeriesNumber();
        result.seriesSeason = recording.getSeriesSeason();

        result.qualities = new HashSet<Quality>();
        for (MovieFile file : recording.getAllMovieFiles()) {
            result.qualities.add(file.getQuality());
        }
        return result;
    }

    private JsonDownloadResult marshal(Download download) {
        JsonDownloadResult result = new JsonDownloadResult();
        result.downloadId = download.getId();
        result.recordingId = download.getRecordingId();
        result.downloadFile = download.getDownloadFile().getAbsolutePath();
        result.totalBytes = download.getTotalBytes();
        result.downloadedBytes = download.getDownloadedBytes();
        result.quality = download.getMovieFile().getQuality();

        Long eta = download.getEta();
        result.ETA = eta == null ? null : ToolBox.toHumanReadableTime(download.getEta());

        result.downloading = download.isDownloading();
        result.finished = download.isDownloaded();
        return result;
    }

    public void shutdown() {
        try {
            this.server.stop();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Problem when shutting down embedded jetty server", e);
        }
    }

    public static class JsonEmpty {
    }

    public static class JsonStatusResult {
        public boolean paused;
        public ControllerState state;
        public int recordings;
        public int downloads;
        public float currentDownloadSpeed;
    }

    public static class JsonRecordingsResult {
        public List<JsonRecordingResult> recordings;
    }

    public static class JsonDownloadsResult {
        public List<JsonDownloadResult> downloads;
    }

    public static class JsonRecordingResult {
        public int id;
        public String title;
        public String channel;
        public Set<Quality> qualities;
        public String description;
        public String genre;
        public long duration;
        public Date start;

        public Integer seriesCount;
        public Integer seriesNumber;
        public Integer seriesSeason;
    }

    public static class JsonDownloadResult {
        public int downloadId;
        public int recordingId;
        public Quality quality;
        public long downloadedBytes;
        public long totalBytes;
        public boolean downloading;
        public boolean finished;
        public String ETA;
        public String downloadFile;
    }

    public static class JsonEnqueueDownloadRequest {
        public int recordingId;
        public String quality;
    }

    public static class JsonMarkRecordingDeletedRequest {
        public int recordingId;
    }

    public static class JsonRemoveDownloadRequest {
        public int downloadId;
    }

    public static class JsonSetPauseRequest {
        public boolean pause;
    }

    public static class JsonSetFilenamePatternRequest {
        public String filenamePattern;
    }

    public static class JsonSetSpeedLimitRequest {
        public long speedLimit;
    }

    public static class JsonTestFilenamePatternResult {
        public boolean valid;
        public String fileName;
        public String fileNameSeries;
    }

}
