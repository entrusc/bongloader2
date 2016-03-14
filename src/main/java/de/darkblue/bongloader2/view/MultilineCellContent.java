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
import java.util.ArrayList;
import java.util.List;

/**
 * Content for a multiline cell renderer
 * 
 * @author Florian Frankenberger
 */
public class MultilineCellContent {

    public static enum LineStyle {
        NONE,
        STRIKED_OUT
    }
    
    private List<String> lines = new ArrayList<String>();
    private List<Color> colors = new ArrayList<Color>();
    private List<LineStyle> styles = new ArrayList<LineStyle>();

    public MultilineCellContent() {
    }
    
    public MultilineCellContent(String[] lines) {
        for (String line : lines) {
            this.addLine(line);
        }
    }
    
    public MultilineCellContent(String line) {
        addLine(line);
    }
    
    public final void addLine(String line) {
        this.lines.add(line);
        this.colors.add(this.colors.isEmpty() ? Color.BLACK : Color.DARK_GRAY);
        this.styles.add(LineStyle.NONE);
    }
    
    public void setColor(int lineIndex, Color color) {
        this.colors.set(lineIndex, color);
    }
    
    public Color getColor(int lineIndex) {
        return this.colors.get(lineIndex);
    }
    
    public void setStyle(int lineIndex, LineStyle style) {
        this.styles.set(lineIndex, style);
    }
    
    public LineStyle getStyle(int lineIndex) {
        return this.styles.get(lineIndex);
    }

    public List<String> getLines() {
        return lines;
    }
    
}
