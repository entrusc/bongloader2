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
import de.darkblue.bongloader2.utils.ToolBox.ShortenStyle;
import de.darkblue.bongloader2.view.MultilineCellContent.LineStyle;
import static de.darkblue.bongloader2.view.MultilineCellContent.LineStyle.STRIKED_OUT;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JTable;

/**
 *
 * @author Florian Frankenberger
 */
class MultilineCell extends JPanel {

    private final boolean rightAligned;
    private final boolean isSelected;
    private final JTable table;
    private final MultilineCellContent content;

    private final Map<Integer, ShortenStyle> shortenStyles = new HashMap<Integer, ShortenStyle>();

    public MultilineCell(boolean rightAligned, boolean isSelected, JTable table, MultilineCellContent content) {
        this.rightAligned = rightAligned;
        this.isSelected = isSelected;
        this.table = table;
        this.content = content;
    }

    public void setShortenStyle(int index, ShortenStyle shortenStyle) {
        this.shortenStyles.put(index, shortenStyle);
    }

    private ShortenStyle getShortenStyle(int index) {
        if (this.shortenStyles.containsKey(index)) {
            return this.shortenStyles.get(index);
        } else {
            return ShortenStyle.RIGHT;
        }
    }

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

        g2d.setFont(table.getFont());
        final FontMetrics fontMetrics = g2d.getFontMetrics();
        final int ascent = fontMetrics.getAscent();
        final int spacing = 2;

        for (int index = 0; index < content.getLines().size(); ++index) {
            String line = content.getLines().get(index);
            final Color color = table.isEnabled() ? content.getColor(index) : Color.GRAY;
            final LineStyle style = content.getStyle(index);

            g2d.setColor(color);
            switch (style) {
                case STRIKED_OUT:
                    g2d.setFont(getStrikeOutFont(table.getFont()));
                    break;
                default:
                    g2d.setFont(table.getFont());
            }

            final int maxWidth = this.getWidth() - 10;
            line = ToolBox.getStringFittingInto(line, maxWidth, getShortenStyle(index), g2d);
            final Dimension fontExtent = ToolBox.getFontExtent(line, g2d);

            g2d.drawString(line,
                    rightAligned
                        ? this.getWidth() - 5 - fontExtent.width
                        : 5,
                    index * (fontMetrics.getHeight() + spacing) + ascent + 5);
        }

    }

    private static Font getStrikeOutFont(Font font) {
        final Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) font.getAttributes();
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        return font.deriveFont(attributes);
    }
}
