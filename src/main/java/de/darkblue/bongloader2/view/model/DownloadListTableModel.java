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
package de.darkblue.bongloader2.view.model;

import de.darkblue.bongloader2.model.Download;
import de.darkblue.bongloader2.model.data.StorableList;
import de.darkblue.bongloader2.utils.ToolBox;
import de.darkblue.bongloader2.view.DownloadStateCellRenderer;
import de.darkblue.bongloader2.view.MultilineCellContent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author Florian Frankenberger
 */
public class DownloadListTableModel extends StorableListTableModel<Download> {

    private static final SimpleDateFormat ETA_DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public DownloadListTableModel(StorableList<Download> list) {
        super(list);
    }

    @Override
    public int getColumnCount() {
        return 7;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
            case 1:
                return "Kanal";
            case 2:
                return "Titel";
            case 3:
                return "Aufnahmezeit";
            case 4:
                return "Datei";
            case 5:
                return "ETA";
            case 6:
                return "Fortschritt";
            case 0:
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final Download download = list.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return (download.isDownloaded()
                        ? DownloadStateCellRenderer.DownloadState.FINISHED
                        : (download.isDownloading()
                            ? DownloadStateCellRenderer.DownloadState.DOWNLOADING
                            : DownloadStateCellRenderer.DownloadState.WAITING));
            case 1:
                return download.getRecording().getChannel();
            case 2:
                return new MultilineCellContent(new String[] { download.getRecording().getTitle(), download.getMovieFile().getQuality().toString() });
            case 3:
                return download.getRecording().getStart();
            case 4:
                return download.getTargetFile();
            case 5:
                final Long eta = download.getEta();
                if (eta == null) {
                    if (download.isDownloaded()) {
                        return new MultilineCellContent(new String[] {
                            "Fertig",
                            ETA_DATE_FORMAT.format(download.getDownloadedAt())
                        });
                    } else {
                        return new MultilineCellContent(new String[] {
                            "Unendlich",
                            "Nie"
                        });
                    }
                } else {
                    String downloadedAt = "";
                    if (eta == Long.MAX_VALUE) {
                        downloadedAt = "Nie";
                    } else {
                        final Date targetDate = new Date(new Date().getTime() + eta * 1000L);
                        downloadedAt = ETA_DATE_FORMAT.format(targetDate);
                    }

                    return new MultilineCellContent(new String[] {
                        ToolBox.toHumanReadableTime(eta),
                        downloadedAt
                    });
                }
            case 6:
                return download;
            default:
                return null;
        }
    }

}
