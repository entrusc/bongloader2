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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Florian Frankenberger
 */
public class DownloadStateCellRenderer implements TableCellRenderer {

    public static enum DownloadState {
        WAITING(ToolBox.loadGraphic("television_off")),
        DOWNLOADING(ToolBox.loadGraphic("television_dl")),
        FINISHED(ToolBox.loadGraphic("television"));

        private final BufferedImage icon;
        
        private DownloadState(BufferedImage icon) {
            this.icon = icon;
        }
    }
    
    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, 
        boolean hasFocus, int row, int column) {
        
        return new JPanel() {

            @Override
            public void paint(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;

                ToolBox.setDefaultRendering(g2d);

                if (isSelected) {
                    this.setForeground(table.getSelectionForeground());
                    this.setBackground(table.getSelectionBackground());
                } else {
                    this.setForeground(table.getForeground());
                    this.setBackground(table.getBackground());
                }
                g2d.setColor(this.getBackground());
                g2d.fillRect(0, 0, this.getWidth(), this.getHeight());
                
                final DownloadState downloadState = (DownloadState) value;
                final BufferedImage image = downloadState.icon;
                
                g2d.drawImage(image, (this.getWidth() - image.getWidth()) / 2, (this.getHeight() - image.getHeight()) / 2, null);
            }
        };
        
    }
}
