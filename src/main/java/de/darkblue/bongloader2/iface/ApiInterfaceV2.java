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

import de.darkblue.bongloader2.exception.ReportableException;
import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.utils.StringTemplate;
import de.darkblue.bongloader2.utils.Utils;
import de.darkblue.bongloader2.utils.XmlUtils;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The old V2 interface of bong.tv
 *
 * @author Florian Frankenberger
 */
public class ApiInterfaceV2 implements ApiInterface {

    private static final Logger LOGGER = Logger.getLogger(ApiInterfaceV2.class.getCanonicalName());

    private static final StringTemplate API_URL_TEMPLATE =
            new StringTemplate("http://www.bong.tv/api/recordings.xml?username={username}&password={password}");

    private static final StringTemplate API_CHECK_CREDENTIALS_URL_TEMPLATE =
            new StringTemplate("http://www.bong.tv/api/users.xml?username={username}&password={password}");

    private static final StringTemplate API_DELETE_RECORDING_URL_TEMPLATE =
            new StringTemplate("http://www.bong.tv/api/recordings/{recordingid}.xml?username={username}&password={password}");

    private static final SimpleDateFormat API_DATE_TIME_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm");

    static {
        API_DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("CET"));
    }

    @Override
    public String getVersion() {
        return "V2";
    }

    @Override
    public List<Recording> downloadRecordingsList(String username, String password) throws ApiInterfaceException, ReportableException {
        try {
            final Map<Object, Object> credentials = getCredentialsMap(username, password);

            List<Recording> newRecordings = new ArrayList<Recording>();
            final URL apiUrl = new URL(API_URL_TEMPLATE.apply(credentials));

            Document document = null;
            try {
                document = downloadDocument(apiUrl, "GET");
            } catch (BongTvInterfaceException e) {
                if (checkCredentials(username, password)) {
                    //credentials are still ok - so we just ignore it for now as there might
                    //be an error on server side
                    throw new ReportableException("Problem beim download der Aufnahmeliste", e.getMessage());
                } else {
                    throw new ReportableException("Authentifizierungsfehler",
                            "Der angegebene Benutzername und/oder Passwort stimmen nicht. Bitte korrigieren Sie die Zugangsdaten in den Einstellungen");
                }
            }

            Node recordingsElement = XmlUtils.getRequiredTag(document, "recordings");
            final NodeList recordingElements = recordingsElement.getChildNodes();
            for (int i = 0; i < recordingElements.getLength(); ++i) {
                final Node recordingElement = recordingElements.item(i);

                if (recordingElement.getNodeName().equalsIgnoreCase("recording")) {
                    final Recording recording = new Recording();
                    recording.setId(Integer.valueOf(XmlUtils.getRequiredTag(recordingElement, "id").getTextContent()));

                    recording.setTitle(XmlUtils.getRequiredTag(recordingElement, "title").getTextContent());
                    recording.setSubtitle(XmlUtils.getRequiredTag(recordingElement, "subtitle").getTextContent());

                    recording.setSeriesSeason(Utils.toIntOrNull(XmlUtils.getRequiredTag(recordingElement, "series_season").getTextContent()));
                    recording.setSeriesCount(Utils.toIntOrNull(XmlUtils.getRequiredTag(recordingElement, "series_count").getTextContent()));
                    recording.setSeriesNumber(Utils.toIntOrNull(XmlUtils.getRequiredTag(recordingElement, "series_number").getTextContent()));

                    recording.setDescription(XmlUtils.getRequiredTag(recordingElement, "description").getTextContent());
                    recording.setChannel(XmlUtils.getRequiredTag(recordingElement, "channel").getTextContent());
                    recording.setGenre(XmlUtils.getRequiredTag(recordingElement, "genre").getTextContent());
                    recording.setStart(API_DATE_TIME_FORMAT.parse(XmlUtils.getRequiredTag(recordingElement, "start").getTextContent()));

                    recording.setThumbUrl(Utils.toURLOrNull(XmlUtils.getRequiredTag(recordingElement, "image").getTextContent()));

                    //parse duration
                    final String durationRaw = XmlUtils.getRequiredTag(recordingElement, "duration").getTextContent();
                    final String [] durationParts = durationRaw.split("\\:");
                    if (durationParts.length < 2) {
                        throw new IllegalArgumentException("Duration can't be split by \":\"");
                    }
                    final int duration = Integer.parseInt(durationParts[0]) * 60 + Integer.parseInt(durationParts[1]);
                    recording.setDuration(duration);

                    //files
                    final Node filesElement = XmlUtils.getRequiredTag(recordingElement, "files");
                    final NodeList fileElements = filesElement.getChildNodes();

                    for (int j = 0; j < fileElements.getLength(); ++j) {
                        final Node fileElement = fileElements.item(j);
                        if (fileElement.getNodeName().equalsIgnoreCase("file")) {

                            //only downloads please!
                            if (XmlUtils.getRequiredTag(fileElement, "type").getTextContent().equalsIgnoreCase("download")) {
                                final URL url = new URL(XmlUtils.getRequiredTag(fileElement, "url").getTextContent());
                                final String qualityRaw = XmlUtils.getRequiredTag(fileElement, "quality").getTextContent();
                                Recording.MovieFile.Quality quality = Recording.MovieFile.Quality.parse(qualityRaw);
                                recording.addFileURL(quality, new Recording.MovieFile(url, quality));
                            }

                        }
                    }

                    newRecordings.add(recording);
                }
            }
            return newRecordings;
        } catch (DOMException e) {
            throw new ApiInterfaceException(e);
        } catch (IllegalArgumentException e) {
            throw new ApiInterfaceException(e);
        } catch (MalformedURLException e) {
            throw new ApiInterfaceException(e);
        } catch (ParseException e) {
            throw new ApiInterfaceException(e);
        } catch (UnsupportedEncodingException e) {
            throw new ApiInterfaceException(e);
        } catch (IOException e) {
            throw new ApiInterfaceException(e);
        } catch (BongTvInterfaceException e) {
            throw new ApiInterfaceException(e);
        }
    }

    @Override
    public void deleteRecording(String username, String password, Recording recording) throws ApiInterfaceException, ReportableException {
        try {
            LOGGER.log(Level.INFO, "Removing {0}", recording);

            Map<Object, Object> credentials = getCredentialsMap(username, password);
            credentials.put("recordingid", recording.getId());
            final URL apiUrl = new URL(API_DELETE_RECORDING_URL_TEMPLATE.apply(credentials));

            try {
                downloadDocument(apiUrl, "DELETE");
            } catch (BongTvInterfaceException e) {
                if (checkCredentials(username, password)) {
                    //credentials are still ok - so we just ignore it for now as there might
                    //be an error on server side
                    throw new ReportableException("Problem beim löschen der Aufnahme \"" + recording.getTitle() + "\"", e.getMessage());
                } else {
                    throw new ReportableException("Authentifizierungsfehler",
                            "Der angegebene Benutzername und/oder Passwort stimmen nicht. Bitte korrigieren Sie die Zugangsdaten in den Einstellungen");
                }
            }
        } catch (IOException e) {
            throw new ApiInterfaceException(e);
        } catch (BongTvInterfaceException e) {
            throw new ApiInterfaceException(e);
        }
    }

    /**
     * Downloads a xml document from bong.tv and returns it on success. If an
     * error happens on server side a BongTvInterfaceException is thrown otherwise
     * a IOException is thrown
     *
     * @param url
     * @param requestMethod
     * @return
     * @throws BongTvInterfaceException
     * @throws IOException
     */
    private Document downloadDocument(URL url, String requestMethod) throws BongTvInterfaceException, IOException {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "BongLoader2");
            connection.setRequestMethod(requestMethod);
            connection.setReadTimeout(5000);

            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(connection.getInputStream());

            //check for error message
            if (document.getElementsByTagName("status").getLength() != 1) {
                throw new BongTvInterfaceException("No status-tag found in return message from bong.tv");
            }

            String rawStatus = document.getElementsByTagName("status").item(0).getTextContent();
            if (Boolean.valueOf(rawStatus)) {
                return document;
            } else {
                String errorMessage = "Keine Fehlerbeschreibung vorhanden.";
                NodeList list = document.getElementsByTagName("messages");
                for (int i = 0; i < list.getLength(); ++i) {
                    Node node = list.item(i);
                    if (node.getAttributes().getNamedItem("lang").getTextContent().equals("de")) {
                        if (node.getChildNodes().getLength() == 0) {
                            break;
                        }
                        NodeList messageNodes = node.getChildNodes();
                        for (int j = 0; j < messageNodes.getLength(); ++j) {
                            Node messageNode = messageNodes.item(j);
                            if (messageNode.getNodeType() == Node.ELEMENT_NODE) {
                                errorMessage = messageNode.getTextContent();
                                break;
                            }
                        }
                    }
                }

                throw new BongTvInterfaceException(errorMessage);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (BongTvInterfaceException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    private boolean checkCredentials(String username, String password) throws MalformedURLException, BongTvInterfaceException,
            UnsupportedEncodingException, IOException {
        final Map<Object, Object> credentials = getCredentialsMap(username, password);

        final URL credentialsUrl = new URL(API_CHECK_CREDENTIALS_URL_TEMPLATE.apply(credentials));
        try {
            downloadDocument(credentialsUrl, "GET");
            return true;
        } catch (BongTvInterfaceException e) {
            return false;
        }
    }

    private static Map<Object, Object> getCredentialsMap(String username, String password) throws UnsupportedEncodingException {
        Map<Object, Object> credentials = new HashMap<Object, Object>();
        credentials.put("username", URLEncoder.encode(username, "UTF-8"));
        credentials.put("password", URLEncoder.encode(password, "UTF-8"));
        return credentials;
    }

    public static class BongTvInterfaceException extends Exception {

        public BongTvInterfaceException(Throwable cause) {
            super(cause);
        }

        public BongTvInterfaceException(String message, Throwable cause) {
            super(message, cause);
        }

        public BongTvInterfaceException(String message) {
            super(message);
        }

        public BongTvInterfaceException() {
        }

    }

}
