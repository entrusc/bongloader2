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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import javax.swing.JPanel;

/**
 *
 * @author Florian Frankenberger
 */
public class ImagePanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private Image image;
    private String text = null;
    private Color textColor = new Color(0, 0, 0);

    public ImagePanel() {
        this(null);
    }

    public ImagePanel(Image image) {
        this.setImage(image);
    }

    public void setImage(Image image) {
        this.image = image;

        if (image != null) {
            ImageObserver listener = new ImageObserver() {

                @Override
                public boolean imageUpdate(Image img, int infoflags, int x, int y, int width, int height) {
                    setPreferredSize(new Dimension(width + 2, height + 2));
                    repaint();
                    if (getParent() != null)
                        getParent().validate();

                    return true;
                }
                
            };

            this.setPreferredSize(
                        new Dimension(this.image.getWidth(listener) + 2, this.image.getHeight(listener) + 2)
                    );

            repaint();
            if (getParent() != null)
                getParent().validate();
        }
    }

    public synchronized void setText(String text) {
        this.text = text;
    }

    public void setTextColor(Color color) {
        this.textColor = color;
    }

    @Override
    public synchronized void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2d = (Graphics2D) g;
        if (image != null) {
            g2d.drawImage(image, 1, 1, null);
        }

        if (text != null) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(textColor);
            LineMetrics lineMetrics = g2d.getFontMetrics().getLineMetrics(text, g2d);
            Rectangle2D rect = g2d.getFontMetrics().getStringBounds(text, g2d);
            g2d.drawString(
                    text,
                    (float)((this.getWidth() - rect.getWidth()) / 2f),
                    (float)((this.getHeight() + lineMetrics.getAscent()) / 2f)
            );
        }
    }
}
