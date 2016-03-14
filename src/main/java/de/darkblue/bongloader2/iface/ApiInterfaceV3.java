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
package de.darkblue.bongloader2.iface;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.darkblue.bongloader2.exception.ReportableException;
import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.model.Recording.MovieFile;
import de.darkblue.bongloader2.utils.ToolBox;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * The implementation of bong.tv's api V3
 * http://help.bong.tv/customer/portal/kb_article_attachments/22022/original.pdf?1382457049
 *
 * @author Florian Frankenberger
 */
public class ApiInterfaceV3 implements ApiInterface {

    private static final Logger LOGGER = Logger.getLogger(ApiInterfaceV3.class.getCanonicalName());

    private static final String URL_BASE_BONG_TV = "http://www.bong.tv";
    private static final String STATUS_RECORDED = "recorded";

    private static final String URL_USER_SESSION = URL_BASE_BONG_TV + "/api/v1/user_sessions.json";
    private static final String URL_LIST_RECORDINGS = URL_BASE_BONG_TV + "/api/v1/recordings.json";
    private static final String URL_DELETE_RECORDING = URL_BASE_BONG_TV + "/api/v1/recordings/[ID].json";

    private final ObjectMapper mapper = new ObjectMapper();
    private JSONResponse<UserData> lastUserDataResponse = null;

    public ApiInterfaceV3() {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public String getVersion() {
        return "V3";
    }

    @Override
    public List<Recording> downloadRecordingsList(String username, String password) throws ApiInterfaceException, ReportableException {
        return downloadRecordingsList(0, username, password);
    }

    public List<Recording> downloadRecordingsList(int iteration, String username, String password) throws ApiInterfaceException, ReportableException {
        try {
            if (lastUserDataResponse == null) {
                login(username, password);
            }
            JSONResponse<ApiRecordings> result = call(URL_LIST_RECORDINGS, lastUserDataResponse.getCookies(), RequestMethod.GET, ApiRecordings.class);
            return result.getPayload().toRecordings();
        } catch (IOException e) {
            throw new ApiInterfaceException(e);
        } catch (LoginException e) {
            throw new ReportableException("Authentifizierungsfehler",
                            "Der angegebene Benutzername und/oder Passwort stimmen nicht. Bitte korrigieren Sie die Zugangsdaten in den Einstellungen");
        } catch (UnexpectedResponseCodeException e) {
            //on code 401 we are not authorized anymore. We delete the credentials and try again.
            if (e.getResponseCode() / 100 == 4 && iteration < 3) {
                lastUserDataResponse = null;
                return downloadRecordingsList(iteration + 1, username, password);
            } else {
                throw new ApiInterfaceException(e);
            }
        }
    }

    @Override
    public void deleteRecording(String username, String password, Recording recording)
            throws ApiInterfaceException, ReportableException {
        deleteRecording(0, username, password, recording);
    }

    public void deleteRecording(int iteration, String username, String password, Recording recording)
            throws ApiInterfaceException, ReportableException {
        try {
            if (lastUserDataResponse == null) {
                login(username, password);
            }
            String url = URL_DELETE_RECORDING.replace("[ID]", String.valueOf(recording.getId()));
            call(url, lastUserDataResponse.getCookies(), RequestMethod.DELETE);
        } catch (IOException e) {
            throw new ApiInterfaceException(e);
        } catch (LoginException e) {
            throw new ReportableException("Authentifizierungsfehler",
                            "Der angegebene Benutzername und/oder Passwort stimmen nicht. Bitte korrigieren Sie die Zugangsdaten in den Einstellungen");
        } catch (UnexpectedResponseCodeException e) {
            //on code 401 we are not authorized anymore. We delete the credentials and try again.
            if (e.getResponseCode() / 100 == 4 && iteration < 3) {
                lastUserDataResponse = null;
                deleteRecording(iteration + 1, username, password, recording);
            } else {
                throw new ApiInterfaceException(e);
            }
        }
    }

    private void login(String username, String password) throws IOException, LoginException {
        Credentials credentials = new Credentials();
        credentials.login = username;
        credentials.password = password;

        try {
            lastUserDataResponse = call(URL_USER_SESSION, RequestMethod.POST, credentials, UserData.class);
        } catch (UnexpectedResponseCodeException e) {
            throw new LoginException();
        }
    }

    private <T> JSONResponse<T> call(String url, String cookies, RequestMethod requestMethod,
            Class<T> returnClass) throws IOException, UnexpectedResponseCodeException {
        return this.call(url, cookies, requestMethod, null, returnClass);
    }

    private <T> JSONResponse<T> call(String url, RequestMethod requestMethod, Object parameter,
            Class<T> returnClass) throws IOException, UnexpectedResponseCodeException {
        return call(url, null, requestMethod, parameter, returnClass);
    }

    private JSONResponse<?> call(String url, String cookies, RequestMethod requestMethod)
            throws IOException, UnexpectedResponseCodeException {
        return call(url, cookies, requestMethod, null, null);
    }

    /**
     * calls a api method
     *
     * @param <T>
     * @param url
     * @param parameter
     * @return
     * @throws Exception
     */
    private <T> JSONResponse<T> call(String url, String cookies, RequestMethod requestMethod, Object parameter,
            Class<T> returnClass) throws IOException, UnexpectedResponseCodeException {
        final URL target = new URL(url);

        final HttpURLConnection connection = (HttpURLConnection) target.openConnection();
        connection.setReadTimeout(5000);
        connection.setDefaultUseCaches(false);
        connection.setRequestMethod(requestMethod.name().toUpperCase());
        connection.setRequestProperty("User-Agent", "Bongloader2");
        if (cookies != null && cookies.length() > 0) {
            connection.setRequestProperty("Cookie", cookies);
        }
        connection.setDoInput(true);

        if (parameter != null) {
            final byte[] body = mapper.writeValueAsBytes(parameter);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Length", String.valueOf(body.length));
            connection.setDoOutput(true);

            OutputStream out = connection.getOutputStream();
            try {
                out.write(body);
                out.flush();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        }

        final int responseCode = connection.getResponseCode();
        if (responseCode / 100 != 2) {
            throw new UnexpectedResponseCodeException(responseCode);
        }


        final String encoding = connection.getHeaderField("Content-Encoding");
        System.out.println(">>> " + connection.getHeaderFields());

        if (returnClass != null) {
            InputStream in = connection.getInputStream();
//            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
            try {
                T result;
                result = mapper.readValue(in, returnClass);
                String newCookies = connection.getHeaderField("Set-Cookie") != null ? connection.getHeaderField("Set-Cookie") : "";
                JSONResponse<T> response = new JSONResponse<T>(result, newCookies);
                return response;
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } else {
            return null;
        }

    }

    private static enum RequestMethod {
        GET,
        POST,
        DELETE
    }

    private static class Credentials {
        public String login;
        public String password;
    }

    private static class UserData {
        public User user;
    }

    private static class User {
        public int id;
        public String login;
        public String uuid;

        @JsonProperty("first_name")
        public String firstName;

        @JsonProperty("last_name")
        public String lastName;

        @JsonProperty("email")
        public String eMail;
    }

    private static class ApiRecordings {
        public List<ApiRecording> recordings;

        public List<Recording> toRecordings() throws MalformedURLException {
            final List<Recording> result = new ArrayList<Recording>();
            for (ApiRecording recording : this.recordings) {
                //only add recorded recordings -.-
                if (recording.status.equalsIgnoreCase(STATUS_RECORDED)) {
                    result.add(recording.toRecording());
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return "Recordings{" + "recordings=" + recordings + '}';
        }
    }

    private static class ApiRecording {

        public int id;

        @JsonProperty("broadcast_id")
        public int broadcastId;
        public String title;

        // although this is called starts_at_ms this is actually in seconds
        @JsonProperty("starts_at_ms")
        public long startsAtS;

        @JsonProperty("starts_at_date")
        public String startAtDate;

        @JsonProperty("starts_at_time")
        public String startAtTime;

        @JsonProperty("ends_at_time")
        public String endsAtTime;

        @JsonProperty("channel_id")
        public int channelId;

        public String status;
        public int quality;
        public int version;

        @JsonProperty("broadcast_was_deleted")
        public boolean broadcastWasDeleted;

        public List<ApiRecordingFile> files;

        public ApiRecordingImage image;

        public List<ApiRecordingCategory> categories;

        public ApiRecordingBroadcast broadcast;

        public Recording toRecording() throws MalformedURLException {
            Recording recording = new Recording();
            recording.setId(id);
            recording.setChannel(ToolBox.cleanString(this.broadcast.channelName));
            recording.setDescription(this.broadcast.shortText == null
                    ? ""
                    : ToolBox.cleanString(this.broadcast.shortText));
            recording.setDuration(this.broadcast.duration);
            recording.setTitle(ToolBox.cleanString(title));
            recording.setGenre(this.categories.isEmpty()
                    ? ""
                    : ToolBox.cleanString(this.categories.get(0).name));
            recording.setSubtitle(this.broadcast.subtitle == null
                    ? ""
                    : ToolBox.cleanString(this.broadcast.subtitle));

            if (this.broadcast.serie != null) {
                recording.setSeriesNumber(this.broadcast.serie.episode);
                recording.setSeriesSeason(this.broadcast.serie.season);
            }

            if (this.image != null) {
                recording.setThumbUrl(new URL(URL_BASE_BONG_TV + this.image.href));
            }
            for (ApiRecordingFile file : files) {
                final Recording.MovieFile.Quality aQuality = Recording.MovieFile.Quality.parse(file.quality);
                MovieFile movieFile = new Recording.MovieFile(new URL(file.href), aQuality);
                recording.addFileURL(aQuality, movieFile);
            }
            recording.setStart(new Date(this.broadcast.startsAtS * 1000L));

            return recording;
        }

        @Override
        public String toString() {
            return "Recording{" + "id=" + id + ", broadcastId=" + broadcastId + ", title=" + title + ", startsAtMs=" + startsAtS + ", startAtDate=" + startAtDate + ", startAtTime=" + startAtTime + ", endsAtTime=" + endsAtTime + ", channelId=" + channelId + ", status=" + status + ", quality=" + quality + ", version=" + version + ", broadcastWasDeleted=" + broadcastWasDeleted + ", files=" + files + ", image=" + image + ", categories=" + categories + ", broadcast=" + broadcast + '}';
        }

    }

    private static class ApiRecordingBroadcast {
        public int id;
        public String title;

        // although this is called starts_at_ms this is actually in seconds
        @JsonProperty("starts_at_ms")
        public long startsAtS;

        @JsonProperty("starts_at_date")
        public String startAtDate;

        @JsonProperty("starts_at_time")
        public String startAtTime;

        @JsonProperty("ends_at_time")
        public String endsAtTime;

        @JsonProperty("ends_at_ms")
        public String endsAtMs;

        public String country;

        @JsonProperty("productionYear")
        public int productionYear;

        @JsonProperty("channel_id")
        public int channelId;

        @JsonProperty("serie_id")
        public int serieId;

        public List<ApiRecordingCategory> categories;

        @JsonProperty("short_text")
        public String shortText;

        public String subtitle;

        @JsonProperty("channel_name")
        public String channelName;

        public ApiRecordingSerie serie;

        public boolean hd;
        public int duration;

        public ApiRecordingImage image;

        @Override
        public String toString() {
            return "RecordingBroadcast{" + "id=" + id + ", title=" + title + ", startsAtMs=" + startsAtS + ", startAtDate=" + startAtDate + ", startAtTime=" + startAtTime + ", endsAtTime=" + endsAtTime + ", endsAtMs=" + endsAtMs + ", country=" + country + ", productionYear=" + productionYear + ", channelId=" + channelId + ", serieId=" + serieId + ", categories=" + categories + ", shortText=" + shortText + ", subtitle=" + subtitle + ", channelName=" + channelName + ", serie=" + serie + ", hd=" + hd + ", duration=" + duration + ", image=" + image + '}';
        }

    }

    private static class ApiRecordingSerie {
        public Integer season;
        public Integer episode;

        @JsonProperty("total_episodes")
        public Integer totalEpisodes;

        @Override
        public String toString() {
            return "RecordingSerie{" + "season=" + season + ", episode=" + episode + ", totalEpisodes=" + totalEpisodes + '}';
        }
    }

    private static class ApiRecordingCategory {
        public String name;

        @JsonProperty("parent_name")
        public String parentName;

        @Override
        public String toString() {
            return "RecordingCategory{" + "name=" + name + ", parentName=" + parentName + '}';
        }

    }

    private static class ApiRecordingImage {
        public int id;
        public String href;

        @Override
        public String toString() {
            return "RecordingImage{" + "id=" + id + ", href=" + href + '}';
        }
    }

    private static class ApiRecordingFile {
        public int id;
        public String href;
        public String quality;

        @Override
        public String toString() {
            return "RecordingFile{" + "id=" + id + ", href=" + href + ", quality=" + quality + '}';
        }
    }

    private static class JSONResponse<T> {
        private final T payload;
        private final String cookies;

        public JSONResponse(T payload, String cookies) {
            this.payload = payload;
            this.cookies = cookies;
        }

        public String getCookies() {
            return cookies;
        }

        public T getPayload() {
            return payload;
        }
    }

    /**
     * thrown when an error occured because of an
     * unexpected json code was returned
     */
    private static class UnexpectedResponseCodeException extends Exception {
        private final int responseCode;

        public UnexpectedResponseCodeException(int responseCode) {
            this.responseCode = responseCode;
        }

        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public String getMessage() {
            return "Response Code " + responseCode;
        }

    }

    private static class LoginException extends Exception {
    }

}
