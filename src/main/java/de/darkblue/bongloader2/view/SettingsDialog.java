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
import de.darkblue.bongloader2.Configuration;
import de.darkblue.bongloader2.ConfigurationKey;
import de.darkblue.bongloader2.model.AutodownloadLevel;
import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.utils.ToolBox;
import static de.darkblue.bongloader2.utils.ToolBox.DEMO_RECORDING;
import static de.darkblue.bongloader2.utils.ToolBox.DEMO_RECORDING_SERIES;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;


/**
 *
 * @author Florian Frankenberger
 */
public class SettingsDialog extends javax.swing.JDialog {

    private static final Logger logger = Logger.getLogger(SettingsDialog.class.getCanonicalName());
    private static final String FOOBAR_PASSWORD = "XXXXXX";
    private final Application application;

    /**
     * Creates new form SettingsDialog
     */
    public SettingsDialog(Application application, java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        this.application = application;
        initComponents();

        filenamePatternDemo.setFont(filenamePattern.getFont());

        quality.setModel(new DefaultComboBoxModel(AutodownloadLevel.values()));
        loadConfig();

        this.setLocationRelativeTo(parent);

        final Configuration config = application.getConfig();
        final List<String> replacementPatternsRaw = config.getAsList(ConfigurationKey.DIRECTORY_TEMPLATES, "\\|");
        final String[] replacementPatterns = new String[replacementPatternsRaw.size()];

        for (int i = 0; i < replacementPatternsRaw.size(); ++i) {
            replacementPatterns[i] = replacementPatternsRaw.get(i).replaceAll("\\[.+?\\]", "").trim();
        }

        filenamePatternTemplates.setModel(
                new DefaultComboBoxModel(replacementPatterns));

    }

    private void loadConfig() {
        final Configuration config = application.getConfig();
        String usernameRaw = config.get(ConfigurationKey.USERNAME, "");
        usernameRaw = (usernameRaw == null ? "" : usernameRaw);
        this.username.setText(usernameRaw);

        if (config.contains(ConfigurationKey.PASSWORD)) {
            this.password.setText(FOOBAR_PASSWORD);
        } else {
            this.password.setText("");
        }

        String filenamePatternRaw = config.get(ConfigurationKey.FILE_NAME_PATTERN);
        this.filenamePattern.setText(filenamePatternRaw);

        AutodownloadLevel autodownloadLevel = AutodownloadLevel.parse(config.get(ConfigurationKey.AUTODOWNLOAD_LEVEL));
        this.quality.setSelectedIndex(autodownloadLevel.ordinal());

        boolean deleteAfterDownload = config.getAsBoolean(ConfigurationKey.DELETE_AFTER_DOWNLOAD);
        delete.setSelectedIndex(deleteAfterDownload ? 1 : 0);

        maxSimultaneousDownloads.setValue(config.getAsInt(ConfigurationKey.MAX_SIMULTANEOUS_DOWNLOADS));
        updateStrategy.setSelectedIndex(config.getAsBoolean(ConfigurationKey.ALLOW_AUTO_UPDATE) ? 0 : 1);

        updateFilenameDemo();
    }

    private void saveConfig() {
        final Configuration config = application.getConfig();
        config.set(ConfigurationKey.USERNAME, username.getText().trim());
        String passwordRaw = new String(password.getPassword());
        if (!passwordRaw.equals(FOOBAR_PASSWORD)) {
            config.set(ConfigurationKey.PASSWORD, passwordRaw);
        }

        config.set(ConfigurationKey.FILE_NAME_PATTERN, filenamePattern.getText());
        config.set(ConfigurationKey.AUTODOWNLOAD_LEVEL, AutodownloadLevel.values()[quality.getSelectedIndex()].name());

        config.setBoolean(ConfigurationKey.DELETE_AFTER_DOWNLOAD, delete.getSelectedIndex() == 1);

        config.setInt(ConfigurationKey.MAX_SIMULTANEOUS_DOWNLOADS, maxSimultaneousDownloads.getValue());
        config.setBoolean(ConfigurationKey.ALLOW_AUTO_UPDATE, updateStrategy.getSelectedIndex() == 0);
        try {
            config.save();
        } catch (IOException ex) {
            Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateFilenameDemo() {
        final String pattern = this.filenamePattern.getText();
        final boolean ok = ToolBox.checkFilenamePattern(pattern, this.application.getWorkingDirectory());

        String[] finalFileNames = new String[] {"[UNGÜLTIGER DOWNLOADNAME]", "[UNGÜLTIGER DOWNLOADNAME]"};

        if (ok) {
            try {
                finalFileNames = new String[] {
                    new File(ToolBox.getTargetFilename(this.application.getWorkingDirectory(), pattern, DEMO_RECORDING, Recording.MovieFile.Quality.NQ)).getCanonicalPath(),
                    new File(ToolBox.getTargetFilename(this.application.getWorkingDirectory(), pattern, DEMO_RECORDING_SERIES, Recording.MovieFile.Quality.HQ)).getCanonicalPath()
                };
            } catch (IOException ex) {
                Logger.getLogger(SettingsDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        this.filenamePatternDemo.setText("Normal:\n" + finalFileNames[0] + "\nSerie:\n" + finalFileNames[1]);
        this.filenamePatternDemo.setForeground(ok ? Color.BLACK : Color.RED);
        this.saveButton.setEnabled(ok);
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                "Fehler", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        bongLoaderPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        filenamePattern = new javax.swing.JTextPane();
        filenamePatternTemplates = new javax.swing.JComboBox();
        insertButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        quality = new javax.swing.JComboBox();
        deleteLabel = new javax.swing.JLabel();
        delete = new javax.swing.JComboBox();
        jLabel4 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        username = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        password = new javax.swing.JPasswordField();
        jScrollPane2 = new javax.swing.JScrollPane();
        filenamePatternDemo = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        maxSimultaneousDownloads = new javax.swing.JSlider();
        jLabel5 = new javax.swing.JLabel();
        updateStrategy = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        saveButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        imagePanel = new ImagePanel(ToolBox.loadGraphic("settings"));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Einstellungen");
        setResizable(false);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                onDialogShown(evt);
            }
        });

        jLabel1.setText("Downloadverzeichnis- / name:");

        filenamePattern.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                SettingsDialog.this.keyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(filenamePattern);

        filenamePatternTemplates.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filenamePatternTemplatesActionPerformed(evt);
            }
        });

        insertButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/de/darkblue/bongloader2/icons/bullet_arrow_up.png"))); // NOI18N
        insertButton.setText("Einfügen");
        insertButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        insertButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                insertButtonActionPerformed(evt);
            }
        });

        jLabel2.setText("Nur Aufnahmen in folgender Qualitätsstufe automatisch downloaden:");

        quality.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "High Quality und Normal Quality", "nur High Quality", "nur Normal Quality", "keine automatisch herunterladen" }));
        quality.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                qualityActionPerformed(evt);
            }
        });

        deleteLabel.setText("Nachdem die Aufnahme in den gewählten Qualitätsstufen automatisch heruntergeladen wurden, folgendes tun:");

        delete.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "die Aufnahme auf dem bong.tv-Server belassen", "die Aufnahme auf dem bong.tv-Server zum Löschen markieren" }));

        jLabel4.setText("Beispiele:");

        jLabel8.setText("Bong.tv-Benutzername:");

        jLabel9.setText("Bong.tv-Passwort:");

        jScrollPane2.setPreferredSize(new java.awt.Dimension(164, 50));

        filenamePatternDemo.setBackground(javax.swing.UIManager.getDefaults().getColor("Label.background"));
        filenamePatternDemo.setColumns(20);
        filenamePatternDemo.setEditable(false);
        filenamePatternDemo.setLineWrap(true);
        filenamePatternDemo.setRows(2);
        jScrollPane2.setViewportView(filenamePatternDemo);

        jLabel3.setText("Maximal gleichzeitige Downloads:");

        maxSimultaneousDownloads.setMajorTickSpacing(1);
        maxSimultaneousDownloads.setMaximum(9);
        maxSimultaneousDownloads.setMinimum(1);
        maxSimultaneousDownloads.setPaintLabels(true);
        maxSimultaneousDownloads.setSnapToTicks(true);

        jLabel5.setText("Updatestrategie:");

        updateStrategy.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Updates für BongLoader automatisch herunterladen und BongLoader updaten, wenn keine Downloads laufen", "Nur Hinweis zeigen, wenn Updates für BongLoader verfügbar sind und auf manuell Aufforderung installieren" }));
        updateStrategy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                updateStrategyActionPerformed(evt);
            }
        });

        jLabel6.setText("Feld");

        javax.swing.GroupLayout bongLoaderPanelLayout = new javax.swing.GroupLayout(bongLoaderPanel);
        bongLoaderPanel.setLayout(bongLoaderPanelLayout);
        bongLoaderPanelLayout.setHorizontalGroup(
            bongLoaderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bongLoaderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(bongLoaderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(bongLoaderPanelLayout.createSequentialGroup()
                        .addComponent(updateStrategy, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(bongLoaderPanelLayout.createSequentialGroup()
                        .addGroup(bongLoaderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(quality, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(delete, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(bongLoaderPanelLayout.createSequentialGroup()
                                .addComponent(deleteLabel)
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addGap(14, 14, 14))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bongLoaderPanelLayout.createSequentialGroup()
                        .addGroup(bongLoaderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(maxSimultaneousDownloads, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, bongLoaderPanelLayout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, 302, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(password))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(bongLoaderPanelLayout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(filenamePatternTemplates, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(insertButton))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, bongLoaderPanelLayout.createSequentialGroup()
                                .addGroup(bongLoaderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.LEADING))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addContainerGap())
                    .addGroup(bongLoaderPanelLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(bongLoaderPanelLayout.createSequentialGroup()
                        .addGroup(bongLoaderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel5))
                        .addGap(444, 475, Short.MAX_VALUE))))
        );
        bongLoaderPanelLayout.setVerticalGroup(
            bongLoaderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bongLoaderPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(bongLoaderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(username, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(password, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(updateStrategy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(bongLoaderPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(insertButton)
                    .addComponent(filenamePatternTemplates, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(maxSimultaneousDownloads, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(quality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(deleteLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(delete, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        saveButton.setText("Speichern");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Abbrechen");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        imagePanel.setPreferredSize(new java.awt.Dimension(52, 52));

        javax.swing.GroupLayout imagePanelLayout = new javax.swing.GroupLayout(imagePanel);
        imagePanel.setLayout(imagePanelLayout);
        imagePanelLayout.setHorizontalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 52, Short.MAX_VALUE)
        );
        imagePanelLayout.setVerticalGroup(
            imagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 52, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(saveButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(imagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(bongLoaderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(imagePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(bongLoaderPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(saveButton))))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void filenamePatternTemplatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filenamePatternTemplatesActionPerformed
        filenamePattern.requestFocus();
    }//GEN-LAST:event_filenamePatternTemplatesActionPerformed
    private static final Pattern TEMPLATE_ITEM_PATTERN = Pattern.compile("\\[(.*?)\\]");
    private void insertButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_insertButtonActionPerformed
        final Configuration config = application.getConfig();
        final List<String> replacementPatternsRaw = config.getAsList(ConfigurationKey.DIRECTORY_TEMPLATES, "\\|");
        final String item = replacementPatternsRaw.get(filenamePatternTemplates.getSelectedIndex());

        Matcher matcher = TEMPLATE_ITEM_PATTERN.matcher(item);
        if (!matcher.find()) {
            return;
        }
        String template = matcher.group(1);

        int start = filenamePattern.getSelectionStart();
        int end = filenamePattern.getSelectionEnd();

        try {
            filenamePattern.getDocument().remove(start, end - start);
            filenamePattern.getDocument().insertString(start, template, null);
        } catch (BadLocationException ex) {
            logger.log(Level.WARNING, "Problem when inserting text", ex);
        }
        filenamePattern.requestFocus();

        updateFilenameDemo();
    }//GEN-LAST:event_insertButtonActionPerformed

    private void qualityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_qualityActionPerformed
//        deleteLabel.setText("Nachdem die Aufnahme in [template] heruntergeladen wurde, folgendes tun:".replace("[template]", quality.getSelectedItem().toString()));
        if (quality.getSelectedItem().equals(AutodownloadLevel.NONE)) {
            deleteLabel.setEnabled(false);
            delete.setEnabled(false);
        } else {
            deleteLabel.setEnabled(true);
            delete.setEnabled(true);
        }
    }//GEN-LAST:event_qualityActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        this.saveConfig();
        this.setVisible(false);
    }//GEN-LAST:event_saveButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        this.setVisible(false);
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void onDialogShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_onDialogShown
        username.requestFocus();
        username.selectAll();
    }//GEN-LAST:event_onDialogShown

    private void keyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_keyReleased
        String replacement = filenamePattern.getText().replace("\n", "").replace("\r", "");
        if (!replacement.equals(filenamePattern.getText())) {
            filenamePattern.setText(replacement);
        }

        updateFilenameDemo();
    }//GEN-LAST:event_keyReleased

    private void updateStrategyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_updateStrategyActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_updateStrategyActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bongLoaderPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox delete;
    private javax.swing.JLabel deleteLabel;
    private javax.swing.JTextPane filenamePattern;
    private javax.swing.JTextArea filenamePatternDemo;
    private javax.swing.JComboBox filenamePatternTemplates;
    private javax.swing.JPanel imagePanel;
    private javax.swing.JButton insertButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSlider maxSimultaneousDownloads;
    private javax.swing.JPasswordField password;
    private javax.swing.JComboBox quality;
    private javax.swing.JButton saveButton;
    private javax.swing.JComboBox updateStrategy;
    private javax.swing.JTextField username;
    // End of variables declaration//GEN-END:variables
}
