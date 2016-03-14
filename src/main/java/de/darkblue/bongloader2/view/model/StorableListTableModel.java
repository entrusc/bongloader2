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
package de.darkblue.bongloader2.view.model;

import de.darkblue.bongloader2.model.data.ListListener;
import de.darkblue.bongloader2.model.data.StorableList;
import de.darkblue.bongloader2.model.data.Storable;
import java.io.Serializable;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Florian Frankenberger
 */
public abstract class StorableListTableModel<T extends Serializable & Storable<T>> 
    extends AbstractTableModel implements ListListener {
    
    protected StorableList<T> list;
    
    public StorableListTableModel(StorableList<T> list) {
        this.list = list;
        this.list.addListener(this);
    }
    
    public T get(int index) {
        return list.get(index);
    }
    
    public int getIndex(T item) {
        return list.getIndex(item);
    }
    
    @Override
    public int getRowCount() {
        return list.getSize();
    }

    @Override
    public void onInserted(int index, int indexTo) {
        this.fireTableRowsInserted(index, indexTo);
    }

    @Override
    public void onUpdated(int index, int indexTo) {
        this.fireTableRowsUpdated(index, indexTo);
    }

    @Override
    public void onDeleted(int index, int indexTo) {
        this.fireTableRowsDeleted(index, indexTo);
    }    
    
    @Override
    public void onDataChanged() {
        this.fireTableDataChanged();
    }

    
}
