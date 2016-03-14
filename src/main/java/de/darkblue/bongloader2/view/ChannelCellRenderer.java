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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author Florian Frankenberger
 */
public class ChannelCellRenderer implements TableCellRenderer {

    private static final Map<ChannelRenderInfo, BufferedImage> CHANNELS_CACHE_MAP = new HashMap<ChannelRenderInfo, BufferedImage>();

    private static final BufferedImage CHANNELS = ToolBox.loadGraphic("channels");
    private static final BufferedImage CHANNELS_GRAY = ToolBox.grayScaleAlpha(ToolBox.loadGraphic("channels"));
    private static final int IMG_MAP_WIDTH = 94;
    private static final int IMG_MAP_HEIGHT = 70;
    private static final int CHANNEL_UNKNOWN_ID = 45;

    private static final int SPACER = 8;

    private static final Map<String, Integer> CHANNEL_MAPPING = new HashMap<String, Integer>() {{
        this.put("ARD", 0);
        this.put("ZDF", 1);
        this.put("Pro 7", 2);
        this.put("RTL", 3);
        this.put("Kabel 1", 4);
        this.put("N24", 5);
        this.put("arte", 6);
        this.put("Disney Channel", 7);
        this.put("BR", 8);
        this.put("3sat", 9);
        this.put("MDR", 10);
        this.put("WDR", 11);
        this.put("NDR", 12);
        this.put("Phoenix", 13);
        this.put("HR", 14);
        this.put("rbb", 15);
        this.put("SWR", 16);
        this.put("Eurosport", 17);
        this.put("Tele 5", 18);
        this.put("Der Kinderkanal", 19);
        this.put("SAT.1", 20);
        this.put("VOX", 21);
        this.put("RTL 2", 22);
        this.put("ZDF neo", 23);
        this.put("Super RTL", 24);
        this.put("Tagesschau24", 25);
        this.put("DMAX", 26);
        this.put("VIVA", 27);
        this.put("MTV", 28);
        this.put("Sport 1", 29);
        this.put("ARD alpha", 30);
        this.put("Nick", 31);
        this.put("Einsfestival", 32);
        this.put("SF Info", 33);
        this.put("sixx", 34);
        this.put("ZDF kultur", 35);
        this.put("ServusTV", 36);
        this.put("Eins plus", 37);
        this.put("Anixe", 38);
        this.put("ZDF info", 39);
        this.put("RTL Nitro", 40);
        this.put("SAT.1 Gold", 41);
        this.put("Pro 7 Maxx", 42);
        this.put("NTV", 43);
        this.put("ComedyCentral", 44);
    }};

    private static class ChannelRenderInfo {
        private int channelId;
        private int maxWidth, maxHeight;
        private boolean grayScale;

        public ChannelRenderInfo(int channelId, int maxWidth, int maxHeight, boolean grayScale) {
            this.channelId = channelId;
            this.maxWidth = maxWidth;
            this.maxHeight = maxHeight;
            this.grayScale = grayScale;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + this.channelId;
            hash = 97 * hash + this.maxWidth;
            hash = 97 * hash + this.maxHeight;
            hash = 97 * hash + (this.grayScale ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ChannelRenderInfo other = (ChannelRenderInfo) obj;
            if (this.channelId != other.channelId) {
                return false;
            }
            if (this.maxWidth != other.maxWidth) {
                return false;
            }
            if (this.maxHeight != other.maxHeight) {
                return false;
            }
            if (this.grayScale != other.grayScale) {
                return false;
            }
            return true;
        }
    }

    private final boolean small;

    public ChannelCellRenderer(boolean small) {
        this.small = small;
    }

    @Override
    public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, boolean hasFocus, int row, int column) {
        return new JPanel() {

            @Override
            public void paint(Graphics g) {
                final Graphics2D g2d = (Graphics2D)g;
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
                final String channelString = (String) value;

                int channelId = CHANNEL_MAPPING.containsKey(channelString) ? CHANNEL_MAPPING.get(channelString) : CHANNEL_UNKNOWN_ID;

                if (table.isEnabled()) {
                    g2d.setColor(Color.DARK_GRAY);
                } else {
                    g2d.setColor(Color.GRAY);
                }
                g2d.setFont(table.getFont());
                final Dimension fontDim = ToolBox.getFontExtent(channelString, g2d);
                final float baseLine = ToolBox.getFontBaseline(g2d);

                final ChannelRenderInfo channelRenderInfo = new ChannelRenderInfo(channelId, this.getWidth(),
                        this.getHeight(), !table.isEnabled());
                BufferedImage channelImage = getChannelImage(channelRenderInfo);

                g2d.drawImage(channelImage, 0, 0, null);

                if (!small) {
                    g2d.drawString(channelString,
                            (float) (this.getWidth() - fontDim.getWidth()) / 2.0f,
                            (float) (this.getHeight() + IMG_MAP_HEIGHT + SPACER - fontDim.getHeight()) / 2.0f + baseLine
                            );
                }
            }

            private BufferedImage getChannelImage(final ChannelRenderInfo channelRenderInfo) {
                BufferedImage channelImage = CHANNELS_CACHE_MAP.get(channelRenderInfo);
                if (channelImage == null) {
                    final float aspectRationX = this.getWidth() / (float)IMG_MAP_WIDTH;
                    final float aspectRationY = this.getHeight() / (float)IMG_MAP_HEIGHT;

                    int width, height;
                    if (aspectRationX < aspectRationY) {
                        width = this.getWidth();
                        height = (int) (aspectRationX * IMG_MAP_HEIGHT);
                    } else {
                        width = (int) (aspectRationY * IMG_MAP_WIDTH);
                        height = this.getHeight();
                    }

                    channelImage = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D channelImageG2D = (Graphics2D) channelImage.getGraphics();
                    channelImageG2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);


                    channelImageG2D.drawImage(channelRenderInfo.grayScale ? CHANNELS_GRAY : CHANNELS,
                            (this.getWidth() - width) / 2,
                            (this.getHeight() - height) / 2,
                            (this.getWidth() + width) / 2,
                            (this.getHeight() +  height) / 2,
                            0,
                            channelRenderInfo.channelId * IMG_MAP_HEIGHT + 1,
                            IMG_MAP_WIDTH - 1,
                            (channelRenderInfo.channelId + 1) * IMG_MAP_HEIGHT + 1,
                            null
                    );
                    CHANNELS_CACHE_MAP.put(channelRenderInfo, channelImage);
                }
                return channelImage;
            }
        };

    }


}
