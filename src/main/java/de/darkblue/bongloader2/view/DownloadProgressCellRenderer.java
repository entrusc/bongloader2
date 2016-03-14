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

import de.darkblue.bongloader2.model.Download;
import de.darkblue.bongloader2.model.Download.Part;
import de.darkblue.bongloader2.utils.ToolBox;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Florian Frankenberger
 */
public class DownloadProgressCellRenderer implements TableCellRenderer {
    
    private static final BufferedImage PATTERN_IMAGE = ToolBox.loadGraphic("progress_pattern");
    private static final BufferedImage PATTERN_IMAGE_DISABLED = ToolBox.loadGraphic("progress_pattern_disabled");
    private static final TexturePaint PATTERN_TEXTURE = new TexturePaint(PATTERN_IMAGE, 
            new Rectangle2D.Float(0, 0, PATTERN_IMAGE.getWidth(), PATTERN_IMAGE.getHeight()));
    private static final TexturePaint PATTERN_TEXTURE_DISABLED = new TexturePaint(PATTERN_IMAGE_DISABLED, 
            new Rectangle2D.Float(0, 0, PATTERN_IMAGE_DISABLED.getWidth(), PATTERN_IMAGE_DISABLED.getHeight()));
    private static final String HUMAN_READABLE_PATTERN = "%.0f%s";
    
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
                
                final Download download = (Download) value;
                final long totalDownloaded = download.getDownloadedBytes();
                final long totalBytes = download.getTotalBytes();
                
                g2d.setColor(Color.BLACK);
                g2d.drawRect(5, 2, this.getWidth() - 10, 12);
                
                if (totalBytes > 0) {
                    final int totalWidth = this.getWidth() - 11;
                    
                    int counter = 0;
                    for (Part part : download.getParts()) {
                        final int startPosition = (int) Math.round((part.getOffset() / (float)totalBytes) * totalWidth);
                        final int length = (int) Math.round((part.getLoaded() / (float)totalBytes) * totalWidth);
                        
                        if (download.isDownloading()) {
                            g2d.setPaint(PATTERN_TEXTURE);
                        } else {
                            g2d.setPaint(PATTERN_TEXTURE_DISABLED);
                        }
                        g2d.fillRect(6 + startPosition, 3, length, 11);
                        
                        if (counter > 0) {
                            g2d.setColor(Color.BLACK);
                            g2d.drawLine(6 + startPosition, 3, 6+startPosition, 14);
                        }
                        counter++;
                    }
                }
                
                g2d.setColor(Color.DARK_GRAY);
                g2d.setFont(table.getFont());
                final FontMetrics fontMetrics = g2d.getFontMetrics();
                final int fontY = fontMetrics.getAscent() + 18;
                
                final String progressTxt = String.format("%.2f%%", (totalDownloaded / (float) totalBytes) * 100f);
                final Dimension fontExtent = ToolBox.getFontExtent(progressTxt, g2d);
                g2d.drawString(progressTxt, this.getWidth() - 5 - fontExtent.width, fontY);

                if (totalBytes > 0) {
                    final String progressTxt2 = String.format("%s/%s", 
                            ToolBox.toHumanReadableSize(HUMAN_READABLE_PATTERN, totalDownloaded), 
                            ToolBox.toHumanReadableSize(HUMAN_READABLE_PATTERN, totalBytes));
                    g2d.drawString(progressTxt2, 5, fontY);
                    
                    if (download.hasDownloadProblems()) {
                        final int width = g2d.getFontMetrics().bytesWidth(progressTxt2.getBytes(), 0, progressTxt2.length());

                        g2d.setColor(Color.RED);
                        g2d.setFont(table.getFont().deriveFont(Font.BOLD));
                        g2d.drawString("!", 5 + width + 5, fontY);
                    }
                }
                
            }
        };
        
    }
}
