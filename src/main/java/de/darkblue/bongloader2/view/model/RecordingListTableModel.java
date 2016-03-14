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

import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.model.data.StorableList;
import de.darkblue.bongloader2.utils.ToolBox;
import de.darkblue.bongloader2.view.MultilineCellContent;
import java.awt.Color;

/**
 *
 * @author Florian Frankenberger
 */
public class RecordingListTableModel extends StorableListTableModel<Recording> {

    public RecordingListTableModel(StorableList<Recording> recordingList) {
        super(recordingList);
    }

    @Override
    public int getColumnCount() {
        return 6;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
            case 0:
                return "Vorschau";
            case 1:
                return "Kanal";
            case 2:
                return "Titel";
            case 3:
                return "Aufnahmezeitpunkt";
            case 4:
                return "Genre";
            case 5:
                return "Dauer";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final Recording recording = list.get(rowIndex);
        switch(columnIndex) {
            case 0:
                return recording;
            case 1:
                return recording.getChannel();
            case 2:
                MultilineCellContent content = new MultilineCellContent();
                content.addLine(recording.getTitle());

                if (recording.markedDeleted()) {
                    content.setColor(0, new Color(201, 66, 49));
                    content.setStyle(0, MultilineCellContent.LineStyle.STRIKED_OUT);
                }

                if (recording.isSeries()) {
                    final String seriesInfo = String.format("Staffel %02d, Episode %02d", recording.getSeriesSeason(), recording.getSeriesNumber());
                    content.addLine(seriesInfo);
                }
                content.addLine(recording.getSubtitle());
                content.addLine("");
                return content;
            case 3:
                return recording.getStart();
            case 4:
                if (recording.isSeries()) {
                    return new MultilineCellContent(new String[] { recording.getGenre(),
                        String.format("Staffel %2d", recording.getSeriesSeason()),
                        String.format("Episode %2d", recording.getSeriesNumber())
                    });
                } else {
                    return new MultilineCellContent(new String[] { recording.getGenre() });
                }
            case 5:
                final String toHumanReadableTime = ToolBox.toHumanReadableTime(recording.getDuration() * 60L, "m");

                if (toHumanReadableTime.equals(recording.getDuration() + "m")) {
                    return new MultilineCellContent(new String[] { recording.getDuration() + "m" });
                } else {
                    return new MultilineCellContent(new String[] { recording.getDuration() + "m", toHumanReadableTime});
                }
            default:
                return null;
        }
    }


}
