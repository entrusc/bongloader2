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

import java.util.Comparator;

/**
 *
 * @author Florian Frankenberger
 */
public class StringArrayComparator implements Comparator<String[]> {

    @Override
    public int compare(String[] o1, String[] o2) {
        int result = 0;
        for (int i = 0; i < Math.min(o1.length, o2.length); ++i) {
            result = o1[i].compareTo(o2[i]);
            if (result != 0) {
                break;
            }
        }
        
        if (result == 0) {
            if (o1.length < o2.length) {
                return -1;
            } else
                if (o1.length > o2.length) {
                    return 1;
                }
        }
        
        return result;
    }
    
}
