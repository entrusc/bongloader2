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

import de.darkblue.bongloader2.utils.ToolBox;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Florian Frankenberger
 */
public class FileCellRenderer implements TableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected, 
        boolean hasFocus, int row, int column) {
        File file = (File) value;

        String path = "";
        try {
            path = file.getParentFile().getCanonicalPath();
        } catch (IOException e) {
            path = "[Ungülitger Pfad]";
        }
        
        String[] lines = new String[] {
            file.getName(),
            path
        };
        final MultilineCell multilineCell = new MultilineCell(false, isSelected, table, new MultilineCellContent(lines));
        multilineCell.setShortenStyle(0, ToolBox.ShortenStyle.CENTER);
        multilineCell.setShortenStyle(1, ToolBox.ShortenStyle.CENTER);
        return multilineCell;
    }
    
}
