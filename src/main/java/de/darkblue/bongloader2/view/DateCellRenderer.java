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

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Florian Frankenberger
 */
public class DateCellRenderer implements TableCellRenderer {

    private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd. MMM yyyy");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public DateCellRenderer() {
        dayFormat.setTimeZone(TimeZone.getTimeZone("CET"));
        dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
        timeFormat.setTimeZone(TimeZone.getTimeZone("CET"));
    }


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        final Date date = (Date) value;
        final MultilineCellContent content = new MultilineCellContent();
        content.addLine(dayFormat.format(date));
        content.addLine(dateFormat.format(date));
        content.addLine(timeFormat.format(date));

        return new MultilineCell(false, isSelected, table, content);
    }

}
