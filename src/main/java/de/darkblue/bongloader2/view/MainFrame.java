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
package de.darkblue.bongloader2.view;

import de.darkblue.bongloader2.Application;
import de.darkblue.bongloader2.Application.UpdateState;
import de.darkblue.bongloader2.Configuration;
import de.darkblue.bongloader2.ConfigurationKey;
import de.darkblue.bongloader2.exception.ReportableException;
import de.darkblue.bongloader2.controller.CruiseController;
import de.darkblue.bongloader2.controller.DownloadController;
import de.darkblue.bongloader2.model.Download;
import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.model.data.Callback;
import de.darkblue.bongloader2.view.model.DownloadListTableModel;
import de.darkblue.bongloader2.view.model.RecordingListTableModel;
import de.darkblue.bongloader2.model.data.ListListener;
import de.darkblue.bongloader2.model.data.StorableList;
import de.darkblue.bongloader2.model.data.UpdateableListener;
import de.darkblue.bongloader2.utils.ToolBox;
import de.darkblue.bongloader2.utils.Utils;
import de.darkblue.bongloader2.view.model.StorableListTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author Florian Frankenberger
 */
public class MainFrame extends javax.swing.JFrame implements UpdateableListener<DownloadController> {
    public static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());


    public static final BufferedImage TRAY_ICON_ONLINE = ToolBox.resizeImageToTray(ToolBox.loadGraphic("bongloader_tray"));
    public static final BufferedImage TRAY_ICON_OFFLINE = ToolBox.resizeImageToTray(ToolBox.loadGraphic("bongloader_tray_off"));
    public static final BufferedImage MENU_ICON = ToolBox.loadGraphic("bongloader");

    public static final BufferedImage MENU_SPEED_UNLIMITED_ICON = ToolBox.resizeImageToTray(ToolBox.loadGraphic("tag_green"));
    public static final BufferedImage MENU_SPEED_LIMITED_ICON = ToolBox.resizeImageToTray(ToolBox.loadGraphic("tag_orange"));
    public static final BufferedImage MENU_SPEED_LIMITED_DOWNLOADS = ToolBox.resizeImageToTray(ToolBox.loadGraphic("downloads"));

    private static final Font MAIN_FONT;

    private TrayIcon trayIcon;
    private JPopupMenu speedPopup;

    final JSlider concurrentDownloadsSlider = new JSlider(1, 9, 1);

    private final Application application;
    private final Configuration config;

    static {
        try {
            MAIN_FONT = Font.createFont(Font.TRUETYPE_FONT,
                    MainFrame.class.getResourceAsStream("/de/darkblue/bongloader2/other/SVBasicManual.ttf")).deriveFont(11.0f);
        } catch (FontFormatException ex) {
            throw new RuntimeException("Could not load main font.", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Could not load main font.", ex);
        }
    }

    /** Creates new form MainFrame */
    public MainFrame(Application application) {
        this.application = application;
        application.getDownloadController().addListener(this);

        createTrayIcon();

        config = application.getConfig();
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        initComponents();
        onUpdate(application.getDownloadController());

        updateButton.setVisible(false);
        application.addListener(new UpdateableListener<Application>() {

            @Override
            public void onUpdate(Application item) {
                updateButton.setVisible(item.getUpdateState() == UpdateState.UPDATE_AVAILABLE);
            }

        });

        this.setSize(
                    config.getAsInt(ConfigurationKey.WINDOW_WIDTH),
                    config.getAsInt(ConfigurationKey.WINDOW_HEIGHT)
                );

        this.setLocation(
                    config.getAsInt(ConfigurationKey.WINDOW_X, (dim.width - this.getWidth()) / 2),
                    config.getAsInt(ConfigurationKey.WINDOW_Y, (dim.height - this.getHeight()) / 2)
                );

        int windowState = config.getAsInt(ConfigurationKey.WINDOW_MODE, JFrame.NORMAL);
        this.setExtendedState(windowState);

        this.addComponentListener(new ComponentListener() {

            @Override
            public void componentResized(ComponentEvent e) {
                if (MainFrame.this.getExtendedState() == JFrame.NORMAL) {
                    config.set(ConfigurationKey.WINDOW_WIDTH, String.valueOf(MainFrame.this.getWidth()));
                    config.set(ConfigurationKey.WINDOW_HEIGHT, String.valueOf(MainFrame.this.getHeight()));
                }
                config.set(ConfigurationKey.WINDOW_MODE, String.valueOf(MainFrame.this.getExtendedState()));
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (MainFrame.this.getExtendedState()  == JFrame.NORMAL) {
                    config.set(ConfigurationKey.WINDOW_X, String.valueOf(MainFrame.this.getX()));
                    config.set(ConfigurationKey.WINDOW_Y, String.valueOf(MainFrame.this.getY()));
                }
            }

            @Override
            public void componentShown(ComponentEvent e) {
                config.setBoolean(ConfigurationKey.WINDOW_HIDDEN, false);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                if (!config.getAsBoolean(ConfigurationKey.DISPLAY_TRAY_HINT, false)) {
                    MainFrame.this.trayIcon.displayMessage("BongLoader",
                            "BongLoader2 und auch ihre Downloads laufen im Hintergrund weiter. Klicken Sie hier doppelt um BongLoader2 zu öffnen.", TrayIcon.MessageType.INFO);
                    config.set(ConfigurationKey.DISPLAY_TRAY_HINT, Boolean.toString(true));
                }
                config.setBoolean(ConfigurationKey.WINDOW_HIDDEN, true);
            }

        });

        application.getDownloadList().addListener(new ListListener() {

            @Override
            public void onInserted(int index, int indexTo) {
                updateDownloadsCounter();
            }

            @Override
            public void onUpdated(int index, int indexTo) {
                updateDownloadsCounter();
            }

            @Override
            public void onDeleted(int index, int indexTo) {
                updateDownloadsCounter();
            }

            @Override
            public void onDataChanged() {
                updateDownloadsCounter();
            }

        });

        layoutRecordingsTable();
        layoutDownloadsTable();
        setupMeanSpeedLabel();

        setupSpeedPopupMenu();
    }

    private void setupSpeedPopupMenu() {
        speedPopup = new JPopupMenu();
        final CruiseController cruiseController = application.getDownloadController().getCruiseController();

        final List<Long> speedSteps = new ArrayList<Long>();
        speedSteps.add(20L * 1024L);
        speedSteps.add(50L * 1024L);
        speedSteps.add(100L * 1024L);
        speedSteps.add(200L * 1024L);
        speedSteps.add(500L * 1024L);
        speedSteps.add(800L * 1024L);
        speedSteps.add(1024L * 1024L);
        speedSteps.add(2L * 1024L * 1024L);
        speedSteps.add(3L * 1024L * 1024L);
        speedSteps.add(5L * 1024L * 1024L);
        speedSteps.add(8L * 1024L * 1024L);

        final JMenuItem simultanDownloadMenuItem = new JMenuItem("Downloads: ?");
        simultanDownloadMenuItem.setIcon(new ImageIcon(MENU_SPEED_LIMITED_DOWNLOADS));
        simultanDownloadMenuItem.setPreferredSize(new Dimension(230, 28));
        simultanDownloadMenuItem.setLayout( new BorderLayout() );
        concurrentDownloadsSlider.setSnapToTicks(true);
        concurrentDownloadsSlider.setMajorTickSpacing(1);
        concurrentDownloadsSlider.setPaintTicks(true);
        concurrentDownloadsSlider.setPreferredSize(new Dimension(120, 28));
        concurrentDownloadsSlider.addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                simultanDownloadMenuItem.setText("Downloads: " + concurrentDownloadsSlider.getValue());
            }

        });

        concurrentDownloadsSlider.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                speedPopup.setVisible(false);
                application.getConfig().setInt(ConfigurationKey.MAX_SIMULTANEOUS_DOWNLOADS, concurrentDownloadsSlider.getValue());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

        });

        simultanDownloadMenuItem.add(concurrentDownloadsSlider, BorderLayout.EAST);
        speedPopup.add(simultanDownloadMenuItem);

        speedPopup.add(new JSeparator());

        for (final long speedStep : speedSteps) {
            JMenuItem speedStepItem = new JMenuItem("Geschwindigkeit: max. " + ToolBox.toHumanReadableSize(speedStep) + "/s");
            speedStepItem.setIcon(new ImageIcon(MENU_SPEED_LIMITED_ICON));
            speedStepItem.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    cruiseController.setSpeedLimit(speedStep);
                }

            });
            speedPopup.add(speedStepItem);
        }

        speedPopup.add(new JSeparator());
        JMenuItem fullSpeedItem = new JMenuItem("Geschwindigkeit: unbegrenzt");
        fullSpeedItem.setIcon(new ImageIcon(MENU_SPEED_UNLIMITED_ICON));
        fullSpeedItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                cruiseController.setSpeedLimit(0L);
            }

        });
        speedPopup.add(fullSpeedItem);


    }

    private void setupMeanSpeedLabel() {
        final CruiseController cruiseController = application.getDownloadController().getCruiseController();
        final UpdateableListener<CruiseController> updateableListener = new UpdateableListener<CruiseController>() {
            @Override
            public void onUpdate(CruiseController item) {
                final String max = !item.hasSpeedLimit()
                        ? ""
                        : " [max: " + ToolBox.toHumanReadableSize((long) item.getSpeedLimit()) + "/s]";
                meanSpeedLabel.setText(ToolBox.toHumanReadableSize((long) item.getMeanSpeed())
                        + "/s" + max);

                startDownloadButton.setSelected(!item.isPaused());
                pauseDownloadButton.setSelected(item.isPaused());
            }

        };
        cruiseController.addListener(updateableListener);
        updateableListener.onUpdate(cruiseController);
    }

    private void updateDownloadsCounter() {
        final StorableList<Download> downloads = application.getDownloadList();

        int total = 0;
        for (Download download : downloads.getAll()) {
            total += (download.isDownloading() ? 1 : 0);
        }

        final String newTitle = "Downloads" + (total > 0 ? " [" + total + " aktiv]" : "");
        boolean changed = !newTitle.equals(this.tab.getTitleAt(1));

        if (changed) {
            this.tab.setTitleAt(1, newTitle);
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    tab.repaint();
                }

            });
        }
    }

    private void layoutRecordingsTable() {
        final TableColumnModel columnModel = recordingsTable.getColumnModel();
        recordingsTable.setFont(MAIN_FONT);
        final TableRowSorter<RecordingListTableModel> tableRowSorter =
                new TableRowSorter<RecordingListTableModel>((RecordingListTableModel) recordingsTable.getModel());
        recordingsTable.setRowSorter(tableRowSorter);

        columnModel.getColumn(0).setMinWidth(102);
        columnModel.getColumn(0).setPreferredWidth(102);
        columnModel.getColumn(0).setMaxWidth(102);
        columnModel.getColumn(0).setCellRenderer(new ThumbnailCellRenderer());

        columnModel.getColumn(1).setMinWidth(110);
        columnModel.getColumn(1).setPreferredWidth(110);
        columnModel.getColumn(1).setMaxWidth(110);
        columnModel.getColumn(1).setCellRenderer(new ChannelCellRenderer(false));

        columnModel.getColumn(2).setMinWidth(200);
        columnModel.getColumn(2).setPreferredWidth(200);
        columnModel.getColumn(2).setCellRenderer(new MultilineCellRenderer());
        tableRowSorter.setComparator(2, new StringArrayComparator());

        columnModel.getColumn(3).setMinWidth(110);
        columnModel.getColumn(3).setPreferredWidth(110);
        columnModel.getColumn(3).setMaxWidth(110);
        columnModel.getColumn(3).setCellRenderer(new DateCellRenderer());

        columnModel.getColumn(4).setMinWidth(100);
        columnModel.getColumn(4).setPreferredWidth(100);
        columnModel.getColumn(4).setMaxWidth(100);
        columnModel.getColumn(4).setCellRenderer(new MultilineCellRenderer());
        tableRowSorter.setComparator(4, new StringArrayComparator());

        columnModel.getColumn(5).setMinWidth(80);
        columnModel.getColumn(5).setPreferredWidth(80);
        columnModel.getColumn(5).setMaxWidth(80);
        columnModel.getColumn(5).setCellRenderer(new MultilineCellRenderer(true));
        tableRowSorter.setComparator(5, new StringArrayComparator());

        recordingsTable.setRowHeight(61);
        MouseListener mouseListener = new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                showRecordingsTablePopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showRecordingsTablePopup(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

        };

        recordingsTable.addMouseListener(mouseListener);
    }

    private void showRecordingsTablePopup(MouseEvent e) {
        if (e.isPopupTrigger() && recordingsTable.isEnabled()) {
            selectRecordingBelow(e.getPoint());
            final Recording recording = getSelectedRecording();
            if (recording != null) {
                enqueueHdMenuItem.setText("\"" + recording.getTitle() + "\" in HD herunterladen");
                enqueueHdMenuItem.setEnabled(recording.hasMovieFile(Recording.MovieFile.Quality.HD));

                enqueueHqMenuItem.setText("\"" + recording.getTitle() + "\" in HQ herunterladen");
                enqueueHqMenuItem.setEnabled(recording.hasMovieFile(Recording.MovieFile.Quality.HQ));

                enqueueNqMenuItem.setText("\"" + recording.getTitle() + "\" in NQ herunterladen");
                enqueueNqMenuItem.setEnabled(recording.hasMovieFile(Recording.MovieFile.Quality.NQ));

                if (recording.markedDeleted()) {
                    markDeleteMenuItem.setText("\"" + recording.getTitle() + "\" nicht mehr zum Löschen markieren");
                } else {
                    markDeleteMenuItem.setText("\"" + recording.getTitle() + "\" zum Löschen markieren");
                }
                markDeleteMenuItem.setEnabled(true);
                recordingMenu.show(recordingsTable, e.getX(), e.getY());
            } else {
                enqueueHqMenuItem.setText("HQ herunterladen");
                enqueueHqMenuItem.setEnabled(false);

                enqueueNqMenuItem.setText("NQ herunterladen");
                enqueueNqMenuItem.setEnabled(false);

                markDeleteMenuItem.setText("zum Löschen markieren");
                markDeleteMenuItem.setEnabled(false);
            }
        }
    }

    private void selectRecordingBelow(Point point) {
        int selected = recordingsTable.rowAtPoint(
                new Point(
                    (int)(point.getX() - recordingsTable.getX()),
                    (int)(point.getY() - recordingsTable.getY() - recordingTableScrollPane.getViewport().getViewPosition().y)
                )
        );

        if (selected < 0) return;

        recordingsTable.getSelectionModel().setSelectionInterval(selected, selected);
    }

    private Recording getSelectedRecording() {
        int selectedRow = recordingsTable.getSelectedRow();
        if (selectedRow < 0) return null;

        int modelSelectedRow = recordingsTable.convertRowIndexToModel(selectedRow);
        RecordingListTableModel model = (RecordingListTableModel) recordingsTable.getModel();
        Recording recording = model.get(modelSelectedRow);

        return recording;
    }

    private void layoutDownloadsTable() {
        final TableColumnModel columnModel = downloadsTable.getColumnModel();
        downloadsTable.setFont(MAIN_FONT);
        downloadsTable.setRowHeight(34);

        columnModel.getColumn(0).setMinWidth(40);
        columnModel.getColumn(0).setPreferredWidth(40);
        columnModel.getColumn(0).setMaxWidth(40);
        columnModel.getColumn(0).setCellRenderer(new DownloadStateCellRenderer());

        columnModel.getColumn(1).setMinWidth(90);
        columnModel.getColumn(1).setPreferredWidth(90);
        columnModel.getColumn(1).setMaxWidth(90);
        columnModel.getColumn(1).setCellRenderer(new ChannelCellRenderer(true));

        columnModel.getColumn(2).setMinWidth(200);
        columnModel.getColumn(2).setPreferredWidth(200);
        columnModel.getColumn(2).setCellRenderer(new MultilineCellRenderer());

        columnModel.getColumn(3).setMinWidth(110);
        columnModel.getColumn(3).setPreferredWidth(110);
        columnModel.getColumn(3).setMaxWidth(110);
        columnModel.getColumn(3).setCellRenderer(new DateCellRenderer());

        columnModel.getColumn(4).setMinWidth(200);
        columnModel.getColumn(4).setPreferredWidth(200);
        columnModel.getColumn(4).setCellRenderer(new FileCellRenderer());

        columnModel.getColumn(5).setMinWidth(130);
        columnModel.getColumn(5).setPreferredWidth(130);
        columnModel.getColumn(5).setMaxWidth(130);
        columnModel.getColumn(5).setCellRenderer(new MultilineCellRenderer(true));

        columnModel.getColumn(6).setMinWidth(100);
        columnModel.getColumn(6).setPreferredWidth(100);
        columnModel.getColumn(6).setCellRenderer(new DownloadProgressCellRenderer());

        MouseListener mouseListener = new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mousePressed(MouseEvent e) {
                showDownloadsTablePopup(e);
                showVideoFile(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showDownloadsTablePopup(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

        };

        downloadsTable.addMouseListener(mouseListener);
    }

    private void selectDownloadBelow(Point point) {
        int selected = downloadsTable.rowAtPoint(
                new Point(
                    (int)(point.getX() - downloadsTable.getX()),
                    (int)(point.getY() - downloadsTable.getY() - downloadTableScrollPane.getViewport().getViewPosition().y)
                )
        );

        if (selected < 0) return;

        downloadsTable.getSelectionModel().setSelectionInterval(selected, selected);
    }

    private Download getDownloadBelow(Point point) {
        int selected = downloadsTable.rowAtPoint(
                new Point(
                    (int)(point.getX() - downloadsTable.getX()),
                    (int)(point.getY() - downloadsTable.getY() - downloadTableScrollPane.getViewport().getViewPosition().y)
                )
        );

        if (selected < 0) {
            return null;
        }

        final DownloadListTableModel model = (DownloadListTableModel) downloadsTable.getModel();
        return model.get(downloadsTable.convertRowIndexToModel(selected));
    }

    private Download getSelectedDownload() {
        int selectedRow = downloadsTable.getSelectedRow();
        if (selectedRow < 0) return null;

        return getDownloadForViewIndex(selectedRow);
    }

    private Download[] getSelectedDownloads() {
        final int[] selectedRows = downloadsTable.getSelectedRows();
        Download[] downloads = new Download[selectedRows.length];
        int counter = 0;
        for (int viewIndex : selectedRows) {
            downloads[counter++] = getDownloadForViewIndex(viewIndex);
        }

        return downloads;
    }

    private Download getDownloadForViewIndex(int index) {
        int modelSelectedRow = downloadsTable.convertRowIndexToModel(index);
        DownloadListTableModel model = (DownloadListTableModel) downloadsTable.getModel();
        Download download = model.get(modelSelectedRow);

        return download;
    }

    private void showVideoFile(MouseEvent e) {
        //double click with primary button = play video file
        if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
            selectDownloadBelow(e.getPoint());
            final Download pointDownload = getDownloadBelow(e.getPoint());
            if (pointDownload.isDownloaded()) {
                openFileMenuItemActionPerformed(null);
            }
        }
    }

    private void showDownloadsTablePopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            //selectDownloadBelow(e.getPoint());

            Download pointDownload = getDownloadBelow(e.getPoint());
            Download[] downloads = getSelectedDownloads();

            if (pointDownload != null) {
                boolean inSelectedDownloads = false;
                for (Download download : downloads) {
                    if (download.equals(pointDownload)) {
                        inSelectedDownloads = true;
                    }
                }

                if (!inSelectedDownloads) {
                    selectDownloadBelow(e.getPoint());
                }
                downloads = getSelectedDownloads();
            }

            if (downloads.length > 0) {
                final StorableList<Download> downloadList = application.getDownloadList();
                final Callback<Integer, Download> callback = new Callback<Integer, Download>() {
                    @Override
                    public Integer call(Download value) {
                        return value.getPriority();
                    }
                };
                final int minPrio = downloadList.getMin(callback, 0);
                boolean allGreaterMinPrio = true;

                final int maxPrio = downloadList.getMax(callback, 0);
                boolean allLowerMaxPrio = true;

                for (Download download : downloads) {
                    allGreaterMinPrio &= download.getPriority() > minPrio;
                    allLowerMaxPrio &= download.getPriority() < maxPrio;
                }

                prioDownMenuItem.setEnabled(allLowerMaxPrio);
                prioUpMenuItem.setEnabled(allGreaterMinPrio);
                topPriorityMenuItem.setEnabled(allGreaterMinPrio);
                openFileMenuItem.setEnabled(downloads.length == 1 && downloads[0].isDownloaded());
                openFolderMenuItem.setEnabled(true);
                deleteDownloadMenuItem.setEnabled(true);
                deleteFinishedMenuItem.setEnabled(true);

                showRecodingMenuItem.setEnabled(downloads.length == 1);
                downloadMenu.show(downloadsTable, e.getX(), e.getY());
            } else {
                prioDownMenuItem.setEnabled(false);
                prioUpMenuItem.setEnabled(false);
                topPriorityMenuItem.setEnabled(false);
                openFileMenuItem.setEnabled(false);
                openFolderMenuItem.setEnabled(false);
                deleteDownloadMenuItem.setEnabled(false);
                deleteFinishedMenuItem.setEnabled(false);
            }
        }
    }

    @Override
    public void dispose() {
        if (trayIcon != null) {
            SystemTray tray = SystemTray.getSystemTray();
            tray.remove(trayIcon);
            LOGGER.fine("removed trayicon");
        }
        super.dispose();
    }

    private void createTrayIcon() {
        if (!SystemTray.isSupported()) {
            throw new IllegalStateException("SystemTray is not supported but is necessary for this application to work.");
        }

        try {
            SystemTray tray = SystemTray.getSystemTray();

            final PopupMenu trayPopup = new PopupMenu();

            MenuItem showMainWindow = new MenuItem("BongLoader anzeigen ...");
            ActionListener showMainWindowAction = new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    MainFrame.this.setVisible(true);
                }
            };
            showMainWindow.addActionListener(showMainWindowAction);
            trayPopup.add(showMainWindow);
            trayPopup.add(new MenuItem("-"));
            MenuItem exit = new MenuItem("BongLoader beenden");
            exit.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    application.shutdownApplication(false);
                }

            });
            trayPopup.add(exit);

            trayIcon = new TrayIcon(TRAY_ICON_OFFLINE, "BongLoader", trayPopup);
            trayIcon.addActionListener(showMainWindowAction);

            tray.add(trayIcon);
            LOGGER.fine("created trayicon");
        } catch (AWTException e) {
            throw new IllegalStateException(e);
        } catch (HeadlessException e) {
            throw new IllegalStateException(e);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel5 = new javax.swing.JPanel();
        recordingMenu = new javax.swing.JPopupMenu();
        enqueueHdMenuItem = new javax.swing.JMenuItem();
        enqueueHqMenuItem = new javax.swing.JMenuItem();
        enqueueNqMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        markDeleteMenuItem = new javax.swing.JMenuItem();
        downloadMenu = new javax.swing.JPopupMenu();
        topPriorityMenuItem = new javax.swing.JMenuItem();
        prioUpMenuItem = new javax.swing.JMenuItem();
        prioDownMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        showRecodingMenuItem = new javax.swing.JMenuItem();
        openFileMenuItem = new javax.swing.JMenuItem();
        openFolderMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        deleteFinishedMenuItem = new javax.swing.JMenuItem();
        deleteDownloadMenuItem = new javax.swing.JMenuItem();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jToolBar2 = new javax.swing.JToolBar();
        settingsButton = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        aboutButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        meanSpeedCaption = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        startDownloadButton = new javax.swing.JButton();
        pauseDownloadButton = new javax.swing.JButton();
        setDownloadSettingsButton = new javax.swing.JButton();
        statusLabel = new javax.swing.JLabel();
        meanSpeedLabel = new javax.swing.JLabel();
        updateButton = new javax.swing.JButton();
        tab = new javax.swing.JTabbedPane();
        recordingsPanel = new javax.swing.JPanel();
        recordingTableScrollPane = new javax.swing.JScrollPane();
        recordingsTable = new javax.swing.JTable();
        downloadsPanel = new javax.swing.JPanel();
        downloadTableScrollPane = new javax.swing.JScrollPane();
        downloadsTable = new javax.swing.JTable();

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );

        enqueueHdMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/television_dl.png"))); // NOI18N
        enqueueHdMenuItem.setText("jMenuItem1");
        enqueueHdMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enqueueHdMenuItemActionPerformed(evt);
            }
        });
        recordingMenu.add(enqueueHdMenuItem);

        enqueueHqMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/television_dl.png"))); // NOI18N
        enqueueHqMenuItem.setText("HQ herunterladen");
        enqueueHqMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enqueueHqMenuItemActionPerformed(evt);
            }
        });
        recordingMenu.add(enqueueHqMenuItem);

        enqueueNqMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/television_dl.png"))); // NOI18N
        enqueueNqMenuItem.setText("NQ herunterladen");
        enqueueNqMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enqueueNqMenuItemActionPerformed(evt);
            }
        });
        recordingMenu.add(enqueueNqMenuItem);
        recordingMenu.add(jSeparator1);

        markDeleteMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/delete.png"))); // NOI18N
        markDeleteMenuItem.setText("Löschen");
        markDeleteMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                markDeleteMenuItemActionPerformed(evt);
            }
        });
        recordingMenu.add(markDeleteMenuItem);

        topPriorityMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/bullet_arrow_up_top.png"))); // NOI18N
        topPriorityMenuItem.setText("Am höchsten Priorisieren");
        topPriorityMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                topPriorityMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(topPriorityMenuItem);

        prioUpMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/bullet_arrow_up.png"))); // NOI18N
        prioUpMenuItem.setText("Hochpriorisieren");
        prioUpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prioUpMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(prioUpMenuItem);

        prioDownMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/bullet_arrow_down.png"))); // NOI18N
        prioDownMenuItem.setText("Runterpriorisieren");
        prioDownMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prioDownMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(prioDownMenuItem);
        downloadMenu.add(jSeparator3);

        showRecodingMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/television.png"))); // NOI18N
        showRecodingMenuItem.setText("Zugehörige Aufnahme anzeigen");
        showRecodingMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showRecodingMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(showRecodingMenuItem);

        openFileMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/film.png"))); // NOI18N
        openFileMenuItem.setText("Videodatei öffnen ...");
        openFileMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(openFileMenuItem);

        openFolderMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/folder.png"))); // NOI18N
        openFolderMenuItem.setText("Downloadverzeichnis öffnen ...");
        openFolderMenuItem.setToolTipText("");
        openFolderMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFolderMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(openFolderMenuItem);
        downloadMenu.add(jSeparator4);

        deleteFinishedMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/delete.png"))); // NOI18N
        deleteFinishedMenuItem.setText("Alle fertigen Downloads enfernen");
        deleteFinishedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteFinishedMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(deleteFinishedMenuItem);

        deleteDownloadMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/delete.png"))); // NOI18N
        deleteDownloadMenuItem.setText("Entfernen");
        deleteDownloadMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteDownloadMenuItemActionPerformed(evt);
            }
        });
        downloadMenu.add(deleteDownloadMenuItem);

        setTitle("BongLoader2 Beta");
        setIconImage(MENU_ICON);

        jPanel3.setLayout(new java.awt.BorderLayout());

        jPanel4.setPreferredSize(new java.awt.Dimension(979, 50));
        jPanel4.setLayout(new java.awt.BorderLayout());

        jPanel6.setPreferredSize(new java.awt.Dimension(4, 50));

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 4, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 50, Short.MAX_VALUE)
        );

        jPanel4.add(jPanel6, java.awt.BorderLayout.LINE_START);

        jToolBar2.setFloatable(false);
        jToolBar2.setRollover(true);

        settingsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/wrench.png"))); // NOI18N
        settingsButton.setText("Einstellungen");
        settingsButton.setFocusable(false);
        settingsButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        settingsButton.setMinimumSize(new java.awt.Dimension(70, 41));
        settingsButton.setPreferredSize(new java.awt.Dimension(70, 41));
        settingsButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        settingsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                settingsButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(settingsButton);
        jToolBar2.add(filler1);

        aboutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/bricks.png"))); // NOI18N
        aboutButton.setText("About");
        aboutButton.setFocusable(false);
        aboutButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        aboutButton.setMaximumSize(new java.awt.Dimension(70, 41));
        aboutButton.setMinimumSize(new java.awt.Dimension(70, 41));
        aboutButton.setPreferredSize(new java.awt.Dimension(70, 41));
        aboutButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });
        jToolBar2.add(aboutButton);

        jPanel4.add(jToolBar2, java.awt.BorderLayout.CENTER);

        jPanel3.add(jPanel4, java.awt.BorderLayout.PAGE_START);

        jPanel1.setPreferredSize(new java.awt.Dimension(979, 24));

        meanSpeedCaption.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        meanSpeedCaption.setText("Download Geschwindigkeit:");

        jLabel2.setText("Status:");

        startDownloadButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/control_play_blue.png"))); // NOI18N
        startDownloadButton.setToolTipText("Downloads starten");
        startDownloadButton.setFocusPainted(false);
        startDownloadButton.setMargin(new java.awt.Insets(0, 14, 0, 14));
        startDownloadButton.setSelected(true);
        startDownloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startDownloadButtonActionPerformed(evt);
            }
        });

        pauseDownloadButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/control_pause_blue.png"))); // NOI18N
        pauseDownloadButton.setToolTipText("Downloads anhalten");
        pauseDownloadButton.setFocusPainted(false);
        pauseDownloadButton.setMargin(new java.awt.Insets(0, 14, 0, 14));
        pauseDownloadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pauseDownloadButtonActionPerformed(evt);
            }
        });

        setDownloadSettingsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/control_equalizer_blue.png"))); // NOI18N
        setDownloadSettingsButton.setToolTipText("Geschwindigkeit anpassen");
        setDownloadSettingsButton.setFocusPainted(false);
        setDownloadSettingsButton.setMargin(new java.awt.Insets(0, 14, 0, 14));
        setDownloadSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                setDownloadSettingsButtonPressed(evt);
            }
        });

        statusLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        statusLabel.setText("Unbekannt");

        meanSpeedLabel.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        meanSpeedLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        meanSpeedLabel.setText("?");

        updateButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/world.png"))); // NOI18N
        updateButton.setText("Jetzt updaten");
        updateButton.setToolTipText("Geschwindigkeit anpassen");
        updateButton.setFocusPainted(false);
        updateButton.setMargin(new java.awt.Insets(0, 14, 0, 14));
        updateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLabel)
                .addGap(6, 6, 6)
                .addComponent(updateButton)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 505, Short.MAX_VALUE)
                .addComponent(meanSpeedCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(meanSpeedLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(setDownloadSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pauseDownloadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(startDownloadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(meanSpeedCaption)
                            .addComponent(meanSpeedLabel)))
                    .addComponent(startDownloadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pauseDownloadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(setDownloadSettingsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(4, 4, 4)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(statusLabel)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addComponent(updateButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.add(jPanel1, java.awt.BorderLayout.PAGE_END);

        recordingsPanel.setLayout(new java.awt.BorderLayout());

        recordingsTable.setModel(new RecordingListTableModel(application.getRecordingList()));
        recordingsTable.setRowHeight(61);
        recordingsTable.setSelectionBackground(new java.awt.Color(182, 201, 49));
        recordingsTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        recordingsTable.getTableHeader().setReorderingAllowed(false);
        recordingTableScrollPane.setViewportView(recordingsTable);

        recordingsPanel.add(recordingTableScrollPane, java.awt.BorderLayout.CENTER);

        tab.addTab("Aufnahmen", new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/television.png")), recordingsPanel); // NOI18N

        downloadsPanel.setLayout(new java.awt.BorderLayout());

        downloadsTable.setModel(new DownloadListTableModel(application.getDownloadList()));
        downloadsTable.setRowHeight(22);
        downloadsTable.setSelectionBackground(new java.awt.Color(182, 201, 49));
        downloadsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        downloadsTable.getTableHeader().setReorderingAllowed(false);
        downloadTableScrollPane.setViewportView(downloadsTable);

        downloadsPanel.add(downloadTableScrollPane, java.awt.BorderLayout.CENTER);

        tab.addTab("Downloads", new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/package_green.png")), downloadsPanel); // NOI18N

        jPanel3.add(tab, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 668, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void settingsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_settingsButtonActionPerformed
        final SettingsDialog settingsDialog = new SettingsDialog(this.application, this, true);
        settingsDialog.setVisible(true);
    }//GEN-LAST:event_settingsButtonActionPerformed

    private void setDownloadSettingsButtonPressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_setDownloadSettingsButtonPressed
        concurrentDownloadsSlider.setValue(application.getConfig().getAsInt(ConfigurationKey.MAX_SIMULTANEOUS_DOWNLOADS));
        concurrentDownloadsSlider.getChangeListeners()[0].stateChanged(null); //otherwise the stat won't change if simultaneous downloads is set to 1
        speedPopup.show(setDownloadSettingsButton, evt.getX(), evt.getY());
    }//GEN-LAST:event_setDownloadSettingsButtonPressed

    private void startDownloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startDownloadButtonActionPerformed
        application.getDownloadController().getCruiseController().setPaused(false);
    }//GEN-LAST:event_startDownloadButtonActionPerformed

    private void pauseDownloadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pauseDownloadButtonActionPerformed
        application.getDownloadController().getCruiseController().setPaused(true);
    }//GEN-LAST:event_pauseDownloadButtonActionPerformed

    private void enqueueNqMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enqueueNqMenuItemActionPerformed
        final Recording selectedRecording = getSelectedRecording();
        if (selectedRecording != null) {
            application.getDownloadController().enqueueDownload(selectedRecording, Recording.MovieFile.Quality.NQ);
            this.tab.setSelectedIndex(1);

            final int viewIndex = downloadsTable.getRowCount() - 1;
            downloadsTable.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
            scrollToVisible(downloadsTable, viewIndex, 0);
        }
    }//GEN-LAST:event_enqueueNqMenuItemActionPerformed

    private void prioUpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prioUpMenuItemActionPerformed
        final Download[] selectedDownloads = getSelectedDownloads();
        final StorableListTableModel<Download> model = (StorableListTableModel<Download>) downloadsTable.getModel();

        final List<Integer> changedRows = new ArrayList<Integer>();
        for (final Download currentDownload : selectedDownloads) {
            final int viewIndex = downloadsTable.convertRowIndexToView(model.getIndex(currentDownload));
            final int higherViewIndex = viewIndex - 1;

            final Download higherPriorizedDownload = getDownloadForViewIndex(higherViewIndex);

            if (higherPriorizedDownload != null) {
                final int currentPriority = currentDownload.getPriority();
                currentDownload.setPriority(higherPriorizedDownload.getPriority());
                higherPriorizedDownload.setPriority(currentPriority);
                changedRows.add(higherViewIndex);
            }
        }

        final ListSelectionModel selectionModel = downloadsTable.getSelectionModel();
        selectionModel.clearSelection();
        for (int row : changedRows) {
            selectionModel.addSelectionInterval(row, row);
        }
    }//GEN-LAST:event_prioUpMenuItemActionPerformed

    private void openFolderMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFolderMenuItemActionPerformed
        final Download[] downloads = getSelectedDownloads();
        for (Download download : downloads) {
            try {
                Desktop.getDesktop().open(download.getTargetFile().getParentFile().getCanonicalFile());
            } catch (Exception ex) {
                ReportableException e = new ReportableException("Downloadordner kann nicht geöffnet werden", "Leider kann der Downloadordner nicht geöffnet werden");
                LOGGER.log(Level.WARNING, "Can't open download folder", e);
            }
        }
    }//GEN-LAST:event_openFolderMenuItemActionPerformed

    private void openFileMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileMenuItemActionPerformed
        final Download download = getSelectedDownload();
        if (download != null) {
            try {
                Desktop.getDesktop().open(download.getTargetFile().getCanonicalFile());
            } catch (Exception ex) {
                ReportableException e = new ReportableException("Videodatei kann nicht geöffnet werden", "Leider kann die Videodatei nicht geöffnet werden");
                LOGGER.log(Level.WARNING, "Can't open the video", e);
            }
        }
    }//GEN-LAST:event_openFileMenuItemActionPerformed

    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        new AboutDialog(this, true, application.getConfig()).setVisible(true);
    }//GEN-LAST:event_aboutButtonActionPerformed

    private void deleteDownloadMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteDownloadMenuItemActionPerformed
        final Download[] downloads = getSelectedDownloads();

        List<Download> notFinishedDownloads = new ArrayList<Download>();
        for (final Download download : downloads) {
            final boolean startedDownloadingButNotFinished = !download.isDownloaded() && download.startedDownloading();
            if (startedDownloadingButNotFinished) {
                notFinishedDownloads.add(download);
            }
        }

        boolean ok = true;
        if (!notFinishedDownloads.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (final Download download : notFinishedDownloads) {
                sb.append("* ").append(download.getRecording().getTitle()).append(" (").append(download.getTargetFile().getName()).append(")\n");
            }

            ok = JOptionPane.showConfirmDialog(this,
                    "Möchten Sie folgende unvollständigen Downloads: \n" + sb.toString() + " wirklich löschen? Ein Fortsetzten der Downloads ist dann nicht mehr möglich.",
                    "Löschen bestätigen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }

        if (ok) {
            for (final Download download :downloads) {
                application.getDownloadList().delete(download);
            }

            downloadsTable.getSelectionModel().clearSelection();
        }

    }//GEN-LAST:event_deleteDownloadMenuItemActionPerformed

    private void updateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateButtonActionPerformed
        //update capabilites have been striped before making bl2 open source
    }//GEN-LAST:event_updateButtonActionPerformed

    private void showRecodingMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showRecodingMenuItemActionPerformed
        final Download download = getSelectedDownload();
        if (download != null) {
            final Recording recording = download.getRecording();

            final Integer index = this.application.getRecordingList().getIndex(recording);
            if (index != null) {
                this.tab.setSelectedIndex(0);
                final int viewIndex = recordingsTable.convertRowIndexToView(index);

                recordingsTable.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
                scrollToVisible(recordingsTable, viewIndex, 0);
            }
        }
    }//GEN-LAST:event_showRecodingMenuItemActionPerformed

    private void topPriorityMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_topPriorityMenuItemActionPerformed
        final StorableListTableModel<Download> model = (StorableListTableModel<Download>) downloadsTable.getModel();
        final Download[] currentDownloads = getSelectedDownloads();

        final Set<Integer> downloadIds = new HashSet<Integer>();
        final List<Integer> changedRows = new ArrayList<Integer>(currentDownloads.length);
        for (Download currentDownload : currentDownloads) {
            final List<Download> downloads = application.getDownloadList().getAll(new Comparator<Download>() {

                @Override
                public int compare(Download o1, Download o2) {
                    return Integer.valueOf(o1.getPriority()).compareTo(o2.getPriority());
                }

            });

            int currentPosition = -1;
            for (int i = 0; i < downloads.size(); ++i) {
                Download download = downloads.get(i);
                if (download.equals(currentDownload)) {
                    currentPosition = i;
                    break;
                }
            }

            for (int i = currentPosition - 1; i >= 0; --i) {
                final Download higherDownload = downloads.get(i);
                if (downloadIds.contains(higherDownload.getId())) {
                    break; //we reached our predecessor!
                }

                int savedPriority = currentDownload.getPriority();
                currentDownload.setPriority(higherDownload.getPriority());
                higherDownload.setPriority(savedPriority);
            }

            changedRows.add(downloadsTable.convertColumnIndexToView(model.getIndex(currentDownload)));
            downloadIds.add(currentDownload.getId());
        }

        final ListSelectionModel selectionModel = downloadsTable.getSelectionModel();
        selectionModel.clearSelection();
        for (int row : changedRows) {
            selectionModel.addSelectionInterval(row, row);
        }


    }//GEN-LAST:event_topPriorityMenuItemActionPerformed

    private void prioDownMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prioDownMenuItemActionPerformed
        final Download[] selectedDownloads = Utils.reverse(getSelectedDownloads());
        final StorableListTableModel<Download> model = (StorableListTableModel<Download>) downloadsTable.getModel();

        final List<Integer> changedRows = new ArrayList<Integer>();
        for (final Download currentDownload : selectedDownloads) {
            final int modelIndex = model.getIndex(currentDownload);
            final int viewIndex = downloadsTable.convertRowIndexToView(modelIndex);
            final int lowerViewIndex = viewIndex + 1;

            final Download lowerPriorizedDownload = getDownloadForViewIndex(lowerViewIndex);

            if (lowerPriorizedDownload != null) {
                final int currentPriority = currentDownload.getPriority();
                currentDownload.setPriority(lowerPriorizedDownload.getPriority());
                lowerPriorizedDownload.setPriority(currentPriority);
                changedRows.add(lowerViewIndex);
            }
        }

        final ListSelectionModel selectionModel = downloadsTable.getSelectionModel();
        selectionModel.clearSelection();
        for (int row : changedRows) {
            selectionModel.addSelectionInterval(row, row);
        }
    }//GEN-LAST:event_prioDownMenuItemActionPerformed

    private void deleteFinishedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteFinishedMenuItemActionPerformed
        final List<Download> finished = new ArrayList<Download>();
        for (Download download : application.getDownloadList().getAll()) {
            if (download.isDownloaded() && download.getDownloadedBytes() == download.getTotalBytes()) {
                finished.add(download);
            }
        }

        application.getDownloadList().delete(finished);
    }//GEN-LAST:event_deleteFinishedMenuItemActionPerformed

    private void enqueueHqMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enqueueHqMenuItemActionPerformed
        final Recording selectedRecording = getSelectedRecording();
        if (selectedRecording != null) {
            application.getDownloadController().enqueueDownload(selectedRecording, Recording.MovieFile.Quality.HQ);
            this.tab.setSelectedIndex(1);

            final int viewIndex = downloadsTable.getRowCount() - 1;
            downloadsTable.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
            scrollToVisible(downloadsTable, viewIndex, 0);
        }
    }//GEN-LAST:event_enqueueHqMenuItemActionPerformed

    private void enqueueHdMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enqueueHdMenuItemActionPerformed
        final Recording selectedRecording = getSelectedRecording();
        if (selectedRecording != null) {
            application.getDownloadController().enqueueDownload(selectedRecording, Recording.MovieFile.Quality.HD);
            this.tab.setSelectedIndex(1);

            final int viewIndex = downloadsTable.getRowCount() - 1;
            downloadsTable.getSelectionModel().setSelectionInterval(viewIndex, viewIndex);
            scrollToVisible(downloadsTable, viewIndex, 0);
        }
    }//GEN-LAST:event_enqueueHdMenuItemActionPerformed

    private void markDeleteMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_markDeleteMenuItemActionPerformed
        final Recording selectedRecording = getSelectedRecording();

        if (selectedRecording != null) {
            if (!selectedRecording.markedDeleted()) {
                final String gracePeriodHumanReadable = ToolBox.toHumanReadableTime(application.getConfig().getAsLong(ConfigurationKey.DELETE_AFTER_DOWNLOAD_GRACE_PERIOD), "m");
                if (JOptionPane.showConfirmDialog(this, "Durch das markieren zum Löschen wird die Aufnahme gelöscht, sobald keine\nweiteren Downloads von dieser Aufnahme mehr laufen und mindestens\n"
                        + gracePeriodHumanReadable + " seit der Verfügbarkeit der Aufnahme vergangen sind.\nAchtung: wenn keine Downloads mehr laufen wird die Aufnahme direkt gelöscht."
                        + "\n\nMöchten Sie wirkling die Aufnahme \"" + selectedRecording.getTitle()
                        + "\" zum Löschen markieren?", "Löschen bestätigen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {

                    selectedRecording.markDeleted(true);
                }
            } else {
                selectedRecording.markDeleted(false);
            }
        }
    }//GEN-LAST:event_markDeleteMenuItemActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JMenuItem deleteDownloadMenuItem;
    private javax.swing.JMenuItem deleteFinishedMenuItem;
    private javax.swing.JPopupMenu downloadMenu;
    private javax.swing.JScrollPane downloadTableScrollPane;
    private javax.swing.JPanel downloadsPanel;
    private javax.swing.JTable downloadsTable;
    private javax.swing.JMenuItem enqueueHdMenuItem;
    private javax.swing.JMenuItem enqueueHqMenuItem;
    private javax.swing.JMenuItem enqueueNqMenuItem;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JMenuItem markDeleteMenuItem;
    private javax.swing.JLabel meanSpeedCaption;
    private javax.swing.JLabel meanSpeedLabel;
    private javax.swing.JMenuItem openFileMenuItem;
    private javax.swing.JMenuItem openFolderMenuItem;
    private javax.swing.JButton pauseDownloadButton;
    private javax.swing.JMenuItem prioDownMenuItem;
    private javax.swing.JMenuItem prioUpMenuItem;
    private javax.swing.JPopupMenu recordingMenu;
    private javax.swing.JScrollPane recordingTableScrollPane;
    private javax.swing.JPanel recordingsPanel;
    private javax.swing.JTable recordingsTable;
    private javax.swing.JButton setDownloadSettingsButton;
    private javax.swing.JButton settingsButton;
    private javax.swing.JMenuItem showRecodingMenuItem;
    private javax.swing.JButton startDownloadButton;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JTabbedPane tab;
    private javax.swing.JMenuItem topPriorityMenuItem;
    private javax.swing.JButton updateButton;
    // End of variables declaration//GEN-END:variables

    @Override
    public void onUpdate(DownloadController item) {
        switch(item.getControllerState()) {
            case CONNECTED:
                statusLabel.setText("Verbunden mit bong.tv");
                this.setTitle("Bongloader2 (" + config.get(ConfigurationKey.USERNAME) + ")");
                recordingsTable.setEnabled(true);
                trayIcon.setImage(TRAY_ICON_ONLINE);
                break;
            case ERROR:
                statusLabel.setText("Fehler beim aktualisieren der Aufnahmeliste");
                recordingsTable.setEnabled(false);
                trayIcon.setImage(TRAY_ICON_OFFLINE);
                break;
            case NO_USERNAME_AND_OR_PASSWORD:
                statusLabel.setText("Benutzername und/oder Passwort nicht konfiguriert");
                this.setTitle("Bongloader2");
                recordingsTable.setEnabled(false);
                trayIcon.setImage(TRAY_ICON_OFFLINE);
                break;
            case UNKNOWN:
            default:
                statusLabel.setText("Unbekannt");
                recordingsTable.setEnabled(false);
                trayIcon.setImage(TRAY_ICON_OFFLINE);
        }
    }

    private static void scrollToVisible(JTable table, int rowIndex, int vColIndex) {
        if (!(table.getParent() instanceof JViewport)) {
            return;
        }
        JViewport viewport = (JViewport)table.getParent();

        // This rectangle is relative to the table where the
        // northwest corner of cell (0,0) is always (0,0).
        Rectangle rect = table.getCellRect(rowIndex, vColIndex, true);

        // The location of the viewport relative to the table
        Point pt = viewport.getViewPosition();

        // Translate the cell location so that it is relative
        // to the view, assuming the northwest corner of the
        // view is (0,0)
        rect.setLocation(rect.x-pt.x, rect.y-pt.y);

        // Scroll the area into view
        viewport.scrollRectToVisible(rect);
    }
}
