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

import de.darkblue.bongloader2.model.Recording;
import de.darkblue.bongloader2.utils.ToolBox;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import org.imgscalr.Scalr;

/**
 *
 * @author Florian Frankenberger
 */
public class ThumbnailCellRenderer implements TableCellRenderer {

    private static final Logger LOGGER = Logger.getLogger(ThumbnailCellRenderer.class.getCanonicalName());
    private final Map<URL, SoftReference<DeferredLoadingCellComponent>> rendererCache = new HashMap<URL, SoftReference<DeferredLoadingCellComponent>>();

    @SuppressWarnings("serial")
    @Override
    public Component getTableCellRendererComponent(JTable table, final Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        final Recording recording = (Recording) value;
        final URL url = recording.getThumbUrl();
        DeferredLoadingCellComponent renderer = rendererCache.containsKey(url) ? rendererCache.get(url).get() : null;
        if (renderer == null) {
            renderer = new DeferredLoadingCellComponent(table, url);
            rendererCache.put(url, new SoftReference<DeferredLoadingCellComponent>(renderer));
        }
        renderer.setSelected(isSelected);
        renderer.setHd(recording.hasMovieFile(Recording.MovieFile.Quality.HD));

        return renderer;
    }
}

class DeferredLoadingCellComponent extends Component {

    private static final BufferedImage HD_LOGO;
    private static final BufferedImage NO_IMAGE;

    static {
        try {
            HD_LOGO = ImageIO.read(DeferredLoadingCellComponent.class.getResourceAsStream("/de/darkblue/bongloader2/icons/hd.png"));
        } catch (IOException e) {
            throw new IllegalStateException("HD logo could not be loaded", e);
        }
        try {
            NO_IMAGE = ImageIO.read(DeferredLoadingCellComponent.class.getResourceAsStream("/de/darkblue/bongloader2/icons/no_image.png"));
        } catch (IOException e) {
            throw new IllegalStateException("Image placeholder could not be loaded", e);
        }
    }

    private static final Logger LOGGER = Logger.getLogger(DeferredLoadingCellComponent.class.getCanonicalName());

    private final URL url;
    private final JTable table;
    private boolean hd = false;
    private volatile BufferedImage image = null;
    private volatile BufferedImage imageDisabled = null;
    private volatile boolean failed = false;

    private boolean selected = false;

    public DeferredLoadingCellComponent(JTable table, URL url) {
        this.table = table;
        this.url = url;

        if (url != null) {
            Thread thread = new Thread() {

                @Override
                public void run() {
                    try {
                        BufferedImage loaded = ImageIO.read(DeferredLoadingCellComponent.this.url);
                        image = Scalr.resize(loaded, 96, 55);
                        //image = ToolBox.resizeImage(loaded, 96, 55);
                        imageDisabled = ToolBox.grayScale(image);
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Could not load thumbnail", ex);
                        image = NO_IMAGE;
                        failed = true;
                    }

                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            DeferredLoadingCellComponent.this.table.repaint();
                        }

                    });

                }

            };
            thread.setDaemon(true);
            thread.start();
        } else {
            image = NO_IMAGE;
            failed = true;
        }
    }


    public void setHd(boolean hd) {
        this.hd = hd;
    }

    public boolean isFailed() {
        return failed;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        ToolBox.setDefaultRendering(g2d);

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        if (this.selected) {
            this.setForeground(table.getSelectionForeground());
            this.setBackground(table.getSelectionBackground());
        } else {
            this.setForeground(table.getForeground());
            this.setBackground(table.getBackground());
        }

        g2d.setColor(this.getBackground());
        g2d.fillRect(0, 0, this.getWidth(), this.getHeight());

        if (table.isEnabled() && image != null) {
            g2d.drawImage(image,
                    (this.getWidth() - image.getWidth()) / 2,
                    (this.getHeight() - image.getHeight()) / 2,
                    null);
        }
        if (!table.isEnabled() && imageDisabled != null) {
            g2d.drawImage(imageDisabled,
                    (this.getWidth() - imageDisabled.getWidth()) / 2,
                    (this.getHeight() - imageDisabled.getHeight()) / 2,
                    null);
        }

        if (image != null && hd) {
            g2d.drawImage(HD_LOGO,
                        (this.getWidth() - image.getWidth()) / 2,
                        (this.getHeight() - image.getHeight()) / 2,
                        null
                    );
        }

//        if (failed) {
//            g2d.setColor(Color.GRAY);
//            g2d.fillRect(
//                        (this.getWidth() - 96) / 2,
//                        (this.getHeight() - 55) / 2,
//                    96, 55);
//
//            g2d.setFont(this.getFont());
//            g2d.setColor(Color.BLACK);
//            g2d.drawString("[no prev]", 24, 33);
//        }
    }
}