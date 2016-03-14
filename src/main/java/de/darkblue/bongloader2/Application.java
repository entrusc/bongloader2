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
package de.darkblue.bongloader2;

import de.darkblue.bongloader2.controller.DownloadController;
import de.darkblue.bongloader2.model.AutodownloadLevel;
import de.darkblue.bongloader2.model.Download;
import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.model.Recording.MovieFile.Quality;
import de.darkblue.bongloader2.model.data.AbstractUpdateable;
import de.darkblue.bongloader2.model.data.AfterLoadingHook;
import de.darkblue.bongloader2.model.data.StorableList;
import de.darkblue.bongloader2.server.WebServiceServer;
import de.darkblue.bongloader2.utils.ToolBox;
import de.darkblue.bongloader2.view.MainFrame;
import de.darkblue.bongloader2.view.VisualErrorHandler;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.*;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Main Class to start the Bongloader
 */
public class Application extends AbstractUpdateable<Application> {

    private static final Logger LOGGER = Logger.getLogger(Application.class.getCanonicalName());

    private static final File WORKING_DIRECTORY;
    private static final Properties APPLICATION_PROPERTIES = new Properties();
    private static final Properties DEFAULT_PROPERTIES = new Properties();

    public static final String SYSTEM_OS_NAME = System.getProperty("os.name", "[unknown]");
    public static final String SYSTEM_JAVA_VERSION = System.getProperty("java.version", "[unknown]");

    private UpdateState updateState = UpdateState.NO_UPDATE_AVAILABLE;

    public static enum UpdateState {
        NO_UPDATE_AVAILABLE,
        UPDATE_AVAILABLE
    }

    private static final Comparator<Download> PRIORITY_COMPARATOR = new Comparator<Download>() {

        @Override
        public int compare(Download o1, Download o2) {
            return Integer.valueOf(o1.getPriority()).compareTo(o2.getPriority());
        }

    };

    private static Recording UNKNOWN_RECORDING = new Recording() {{
        this.setTitle("[Unbekannt]");
        this.setSubtitle("[Unbekannt]");
        this.setChannel("[Unbekannt]");
        this.setDuration(0);
        this.setDescription("[Unbekannt]");
        this.setId(0);
        this.setStart(new Date(0L));
        this.setThumbUrl(null);
    }};

    private boolean headless = false;

    private MainFrame mainFrame;
    private WebServiceServer webServer;

    private Configuration config;
    private DownloadController downloadController;
    private StorableList<Recording> recordingList;
    private StorableList<Download> downloadList;

    private ErrorLogHandler errorLogHandler = new ErrorLogHandler();
    private final Thread shutdownHook = new Thread("shutdown hook") {
            @Override
            public void run() {
                LOGGER.info("Shutdown hook activated.");
                Application.this.shutdownApplication(false);
            }
        };

    static {
        if (System.getenv("APPDATA") == null || System.getenv("APPDATA").trim().isEmpty()) {
            WORKING_DIRECTORY = new File(System.getProperty("user.home"), "bongloader2");
        } else {
            WORKING_DIRECTORY = new File(System.getenv("APPDATA"), "bongloader2");
        }

        try {
            APPLICATION_PROPERTIES.load(
                    Application.class.getResourceAsStream("application.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Could not load application.properties file.");
        }

        try {
            DEFAULT_PROPERTIES.load(
                    Application.class.getResourceAsStream("default.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Could not load default.properties file.");
        }
    }

    private static void activateLookAndFeel() throws ClassNotFoundException, InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(
                UIManager.getSystemLookAndFeelClassName());
    }

    public static void main(String[] args) {
        final Application application = new Application();
        if (args.length > 0) {
            application.setHeadless(args[0].equalsIgnoreCase("-headless"));
        }
        application.start();
    }

    public void start() {
        try {
            initLogging();

            WORKING_DIRECTORY.mkdirs();

            initConfig();
            LOGGER.log(Level.INFO, "BongLoader version {0} (using java {1} on {2})", new Object[] {
                APPLICATION_PROPERTIES.getProperty("version"),
                SYSTEM_JAVA_VERSION,
                SYSTEM_OS_NAME
            });
            LOGGER.log(Level.INFO, "Working directory is {0}", getConfig().get(ConfigurationKey.CURRENT_DIR));
            LOGGER.log(Level.INFO, "Encoding is " + Charset.defaultCharset().name());

            convertOldConfigValues();

            initShutdownHook();

            final ErrorHandler errorHandler = isHeadless()
                    ? new DummyErrorHandler()
                    : new VisualErrorHandler();
            addErrorHandler(errorHandler);

            ToolBox.init(config);

            recordingList = new StorableList<Recording>(new File(WORKING_DIRECTORY, "recordings.xdata"), new Recording.RecordingMarshaller());
            downloadList = new StorableList<Download>(PRIORITY_COMPARATOR, new File(WORKING_DIRECTORY, "downloads.xdata"), new AfterLoadingHook<Download>() {

                @Override
                public void doAfterLoading(Download item) {
                    final Recording recording = Application.this.getRecordingList().getById(item.getRecordingId());
                    if (recording == null) {
                        item.setRecording(UNKNOWN_RECORDING);
                    } else {
                        item.setRecording(recording);
                    }
                }

            }, new Download.DownloadMarshaller());
            downloadController = new DownloadController(this, recordingList, downloadList);

            if (!isHeadless()) {
                activateLookAndFeel();
                LOGGER.fine("Activated look and feel");

                mainFrame = new MainFrame(this);
                mainFrame.setVisible(!config.getAsBoolean(ConfigurationKey.WINDOW_HIDDEN));
                ((VisualErrorHandler) errorHandler).setParent(mainFrame);
                LOGGER.fine("Mainframe visible");
            } else {
                webServer = new WebServiceServer(this, downloadController);
            }

            downloadController.start();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Problem while starting the application", e);
            shutdownApplication(true); //<- error
        }
    }

    public boolean isHeadless() {
        return headless;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public File getWorkingDirectory() {
        return WORKING_DIRECTORY;
    }

    public UpdateState getUpdateState() {
        return updateState;
    }

    public void terminateAndCleanup() {
        LOGGER.info("Getting termination request");
        removeShutdownHook();
        if (downloadController != null) {
            downloadController.shutdown();
        }

        if (mainFrame != null) {
            mainFrame.setVisible(false);
            mainFrame.dispose();
        }

        if (webServer != null) {
            webServer.shutdown();
        }

        LOGGER.log(Level.INFO, "Finished BongLoader {0}", config.get(ConfigurationKey.VERSION));
    }

    public void shutdownApplication(boolean abnormal) {
        if (abnormal) {
            LOGGER.log(Level.WARNING, "Abnormal termination of BongLoader");
        }
        this.terminateAndCleanup();
    }

    public Configuration getConfig() {
        return config;
    }

    public StorableList<Recording> getRecordingList() {
        return recordingList;
    }

    public StorableList<Download> getDownloadList() {
        return downloadList;
    }

    public DownloadController getDownloadController() {
        return downloadController;
    }

    private void initConfig() throws IOException {
        File configFile = new File(WORKING_DIRECTORY, "bongloader2.properties");
        config = parseConfig(configFile);
        config.setFile(ConfigurationKey.CURRENT_DIR, WORKING_DIRECTORY);
        config.set(ConfigurationKey.VERSION, APPLICATION_PROPERTIES.getProperty("version"));
    }

    private static Configuration parseConfig(File configFile) throws IOException {
        return new Configuration(DEFAULT_PROPERTIES, configFile);
    }

    private void initLogging() throws IOException {
        final File logDir = new File(WORKING_DIRECTORY, "logs");
        final String logFileName = "bongloader2_%g.log";

        logDir.mkdirs();
        File logFile = new File(logDir, logFileName);
        FileHandler fh = new FileHandler(logFile.getAbsolutePath(), 1 * 1024 * 1024, 10);
        fh.setFormatter(new SimpleLogFormatter());
        fh.setLevel(Level.ALL);
        final Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        rootLogger.addHandler(fh);

        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new SimpleLogFormatter());
        ch.setLevel(Level.ALL);
        rootLogger.addHandler(ch);

        errorLogHandler.setLevel(Level.ALL);
        rootLogger.addHandler(errorLogHandler);

        Logger.getLogger("de.darkblue.bongloader2").setLevel(Level.ALL);
    }

    private void initShutdownHook() {
        LOGGER.info("Registering shutdown hook");
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    private void removeShutdownHook() {
        LOGGER.info("Unregistering shutdown hook");
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }

    public void addErrorHandler(ErrorHandler errorHandler) {
        errorLogHandler.addErrorHandler(errorHandler);
    }

    public void removeErrorHandler(ErrorHandler errorHandler) {
        errorLogHandler.removeErrorHandler(errorHandler);
    }

    public String getVersion() {
        return config.get(ConfigurationKey.VERSION);
    }

    private void convertOldConfigValues() {
        //convert from old HQ, NQ download list to new AutodownloadLevel config
        if (config.contains(ConfigurationKey.QUALITY_LEVELS_TO_DOWNLOAD)) {
            final List<String> levelsToDownload = config.getAsList(ConfigurationKey.QUALITY_LEVELS_TO_DOWNLOAD, ",");
            final Set<Quality> qualitiesToDownload = EnumSet.noneOf(Quality.class);
            for (String rawQuality : levelsToDownload) {
                qualitiesToDownload.add(Quality.parse(rawQuality));
            }

            final AutodownloadLevel autodownloadLevel = AutodownloadLevel.convert(qualitiesToDownload);
            config.set(ConfigurationKey.AUTODOWNLOAD_LEVEL, autodownloadLevel.name());
            LOGGER.log(Level.INFO, "Updated old quality download level {0} to new autodownload level {1}",
                    new Object[] {levelsToDownload.toString(), autodownloadLevel.name()});
        }
    }


}
