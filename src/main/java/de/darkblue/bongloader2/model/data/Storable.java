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
package de.darkblue.bongloader2.model.data;

import java.io.Serializable;

/**
 * Represents a storable item
 * 
 * @author Florian Frankenberger
 * @param <T>
 */
public interface Storable<T extends Storable<T> & Serializable> extends Updateable<T> {

    /**
     * unique id of the item
     * @return
     */
    int getId();

    /**
     * is called when two of these items exist that have
     * the same id. Updates this with the content of the other
     * item.
     * @param other
     */
    void update(T other);

}
