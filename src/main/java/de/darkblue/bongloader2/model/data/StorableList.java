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

import com.moebiusgames.xdata.DataMarshaller;
import com.moebiusgames.xdata.DataNode;
import com.moebiusgames.xdata.ListDataKey;
import com.moebiusgames.xdata.XData;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import de.darkblue.bongloader2.utils.Utils;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A list that can be saved and loaded
 *
 * @author Florian Frankenberger
 * @param <T>
 */
public class StorableList<T extends Serializable & Storable<T>> implements UpdateableListener<T> {

    protected final Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    private final File file;

    protected Map<Integer, T> items = new ConcurrentHashMap<Integer, T>();
    protected List<T> itemList = new ArrayList<T>();
    private Map<T, Integer> itemsIndexLookup = new ConcurrentHashMap<T, Integer>();

    private final XStream xStream = new XStream(new DomDriver("UTF-8"));
    private final Set<ListListener> listeners = new HashSet<ListListener>();
    private final AfterLoadingHook<T> afterLoadingHook;

    private Long lastUpdate = 0L;
    private final Comparator<T> comparator;

    private final List<DataMarshaller<T>> dataMarshallers = new ArrayList<DataMarshaller<T>>();

    /**
     * default comparator is by id asc
     * @param file
     * @param dataMarshallers
     */
    public StorableList(File file, DataMarshaller<T>... dataMarshallers) {
        this(null, file, null, dataMarshallers);
    }

    /**
     * creates a sorted list by comparator
     *
     * @param order
     * @param file
     * @param afterLoadingHook
     * @param dataMarshallers
     */
    public StorableList(Comparator<T> order, File file, AfterLoadingHook<T> afterLoadingHook, DataMarshaller<T>... dataMarshallers) {
        if (order == null) {
            this.comparator = new Comparator<T>() {

                    @Override
                    public int compare(T o1, T o2) {
                        return Integer.valueOf(o1.getId()).compareTo(o2.getId());
                    }

                };
        } else {
            this.comparator = order;
        }

        this.dataMarshallers.addAll(Arrays.asList(dataMarshallers));
        this.afterLoadingHook = afterLoadingHook;
        this.file = file;


        //for backwards compatibility we load the old XML format first
        final File xmlFile = new File(file.getParentFile(), Utils.getBaseName(file.getName()) + ".xml");
        if (xmlFile.exists()) {
            if (!loadXML(xmlFile)) {
                logger.log(Level.WARNING, "{0} broken - restoring backup (if existing)", this.file);
                loadXML(getBackupFile(xmlFile));
            }
            save(); //save to new format
            logger.log(Level.WARNING, "Found old xml format file ({0}) and converted it to xdata.", xmlFile);

            xmlFile.delete();
            getBackupFile(xmlFile).delete();
        } else {
            load(file);
        }
    }

    /**
     * creates and adds the new item using the provided WithUniqueIdCreator that
     * gets the unique id provided when invoked. This essentially prevents any
     * race condition from adding two items with the same primary id.
     * @param withUniqueIdCreator
     */
    public synchronized void create(WithUniqueIdCreator<T> withUniqueIdCreator) {
        T item = withUniqueIdCreator.createWithId(getNewPrimaryId());
        this.add(item);
    }

    public void add(Collection<T> newItems) {
        this.update(newItems, false);
    }

    public void add(T newItem) {
        final List<T> list = new ArrayList<T>(1);
        list.add(newItem);
        this.update(list, false);
    }

    public synchronized void delete(T item) {
        final List<T> list = new ArrayList<T>(1);
        list.add(item);
        this.delete(list);
    }

    public synchronized void delete(Collection<T> items) {
//        List<Integer> removedIndexes = new ArrayList<Integer>();
        for (T item : items) {
            this.items.remove(item.getId());
            item.removeListener(this);
//            final int itemIndex = this.itemsIndexLookup.get(item);
//            removedIndexes.add(itemIndex);
        }

        updateList();
//        for (int index : removedIndexes) {
//            notifyOnDeleted(index);
//        }
    }

    public synchronized boolean contains(int id) {
        return this.items.containsKey(id);
    }

    public synchronized void update(Collection<T> newItems, boolean remove) {

        //now merge them with the current recording list
        for (T newItem : newItems) {
            if (this.items.containsKey(newItem.getId())) {
                final T itemToUpdate = this.items.get(newItem.getId());
                itemToUpdate.update(newItem);
            } else {
                this.items.put(newItem.getId(), newItem);
                newItem.addListener(this);
            }
        }

        //store the result
        updateList();

        if (remove) {
            final Set<T> hashedNewItems = new HashSet<T>(newItems);
            final List<T> toDelete = new ArrayList<T>();
            for (T item : this.itemList) {
                if (!hashedNewItems.contains(item)) {
                    toDelete.add(item);
                }
            }

            delete(toDelete);
        }

        save();
    }

    public synchronized List<T> getAll() {
        return new ArrayList<T>(this.itemList);
    }

    public synchronized List<T> getAll(Comparator<T> comparator) {
        List<T> list = new ArrayList<T>(this.itemList);
        Collections.sort(list, comparator);
        return list;
    }

    public synchronized T get(int index) {
        if (index >= this.itemList.size() || index < 0) {
            return null;
        }
        return this.itemList.get(index);
    }

    public synchronized T getById(int id) {
        return this.items.get(id);
    }

    public synchronized int getSize() {
        return this.itemList.size();
    }

    /**
     * returns the index or null if the item is not contained
     * in the list
     *
     * @param item
     * @return
     */
    public synchronized Integer getIndex(T item) {
        return this.itemsIndexLookup.get(item);
    }

    public synchronized int getMaxId() {
        int max = 0;
        for (T item : itemList) {
            max = Math.max(max, item.getId());
        }
        return max;
    }

    /**
     * returns the max value of all contained objects T's attributes that
     * you specify by returning it via the callback.call method.
     * @param <C>
     * @param valueCallback
     * @param defaultValue the value to return if there are no item in the list
     * @return
     */
    public synchronized <C extends Comparable<? super C>> C getMax(Callback<C, T> valueCallback, C defaultValue) {
        List<C> results = sortByCallback(valueCallback);
        return results.isEmpty() ? defaultValue : results.get(results.size() - 1);
    }

    public synchronized int getMinId() {
        int min = Integer.MAX_VALUE;
        for (T item : itemList) {
            min = Math.min(min, item.getId());
        }
        return min;
    }

    /**
     * returns true if for one item of this list the valueCallback returns true, otherwise
     * false
     * @param valueCallback
     * @return
     */
    public synchronized boolean containsAtLeastOne(Callback<Boolean, T> valueCallback) {
        for (T item : itemList) {
            if (valueCallback.call(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * returns the min value of all contained objects T's attributes that
     * you specify by returning it via the callback.call method.
     * @param <C>
     * @param valueCallback
     * @param defaultValue the value to return if the list is empty
     * @return
     */
    public synchronized <C extends Comparable<? super C>> C getMin(Callback<C, T> valueCallback, C defaultValue) {
        List<C> results = sortByCallback(valueCallback);
        return results.isEmpty() ? defaultValue : results.get(0);
    }

    private <C extends Comparable<? super C>> List<C> sortByCallback(Callback<C, T> valueCallback) {
        List<C> results = new ArrayList<C>();
        for (T item : itemList) {
            results.add(valueCallback.call(item));
        }
        Collections.sort(results);
        return results;
    }

    public synchronized int getNewPrimaryId() {
        return getMaxId() + 1;
    }

    public void addListener(ListListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(ListListener listener) {
        this.listeners.remove(listener);
    }

    protected final synchronized boolean load(File aFile) {
        if (aFile.exists() && aFile.canRead()) {
            try {
                final DataMarshaller<T>[] marshallers = (DataMarshaller<T>[]) dataMarshallers.toArray(new DataMarshaller[0]);
                DataNode dataNode = XData.load(aFile, marshallers);
                final List<T> itemsInFile = (List<T>) (List) dataNode.getObjectList(KEY_ITEMS);

                this.items = new ConcurrentHashMap<Integer, T>();
                for (T item : itemsInFile) {
                    this.items.put(item.getId(), item);
                }
                updateList();
                for (T item : this.items.values()) {
                    item.addListener(this);
                    if (afterLoadingHook != null) {
                        afterLoadingHook.doAfterLoading(item);
                    }
                }
//                notifyOnInserted(0, this.itemList.size());

            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not load list from " + aFile, e);
                return false;
            }
        }
        return true;
    }

    @Deprecated
    protected final synchronized boolean loadXML(File aFile) {
        if (aFile.exists() && aFile.canRead()) {
            try {
                final Object fromXML = xStream.fromXML(aFile);
                this.items = new ConcurrentHashMap<Integer, T>((Map<Integer, T>) fromXML);
                updateList();
                for (T item : this.items.values()) {
                    item.addListener(this);
                    if (afterLoadingHook != null) {
                        afterLoadingHook.doAfterLoading(item);
                    }
                }
//                notifyOnInserted(0, this.itemList.size());

            } catch (XStreamException e) {
                logger.log(Level.WARNING, "Could not load list from " + aFile, e);
                return false;
            }
        }
        return true;
    }

    public synchronized void save() {
        final File backupFile = getBackupFile(this.file);
        if (backupFile.exists()) {
            backupFile.delete();
        }

        if (this.file.exists()) {
            this.file.renameTo(backupFile);
        }

        saveTo(this.file);
    }

    private static ListDataKey<Object> KEY_ITEMS = ListDataKey.create("items", Object.class);

    private synchronized void saveTo(File aFile) {
        try {
            DataNode dataNode = new DataNode();
            dataNode.setObjectList(KEY_ITEMS, (List<Object>) (List) this.itemList);
            final DataMarshaller<T>[] marshallers = (DataMarshaller<T>[]) dataMarshallers.toArray(new DataMarshaller<?>[0]);
            XData.store(dataNode, aFile, marshallers);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not store list to " + items, e);
        }
    }

    private synchronized void updateList() {
        itemList.clear();

        for (T item : this.items.values()) {
            this.itemList.add(item);
        }

        Collections.sort(itemList, comparator);

        Set<Integer> addedIndexes = new LinkedHashSet<Integer>();
        Set<Integer> updatedIndexes = new LinkedHashSet<Integer>();
        TreeSet<Integer> removedIndexes = new TreeSet<Integer>(new Comparator<Integer>() {

            @Override
            public int compare(Integer o1, Integer o2) {
                return Integer.valueOf(o1).compareTo(o2);
            }

        });

        Map<T, Integer> newItemIndexLookup = new HashMap<T, Integer>();
        for (int i = 0; i < this.itemList.size(); ++i) {
            final T item = this.itemList.get(i);
            newItemIndexLookup.put(item, i);
            if (!this.itemsIndexLookup.containsKey(item)) {
                addedIndexes.add(i);
            } else {
                if (this.itemsIndexLookup.get(item) != i) {
                    updatedIndexes.add(i);
                    updatedIndexes.add(this.itemsIndexLookup.get(item));
                }
            }
        }

        for (Integer i : this.itemsIndexLookup.values()) {
            if (i >= this.itemList.size()) {
                removedIndexes.add(i);
                updatedIndexes.remove(i);
            }
        }

        this.itemsIndexLookup = newItemIndexLookup;

        for (Integer index : addedIndexes) {
            notifyOnInserted(index);
        }

        for (Integer index : updatedIndexes) {
            notifyOnUpdated(index);
        }

        //check if we have a continous row
        if (!removedIndexes.isEmpty()) {
            if (removedIndexes.size() > 1) {
                Integer lastIndex = null;
                int min = Integer.MAX_VALUE;
                int max = 0;
                boolean row = true;

                for (Integer index : removedIndexes) {
                    min = Math.min(min, index);
                    max = Math.max(max, index);

                    if (lastIndex != null) {
                        if (index != lastIndex + 1) {
                            row = false;
                            break;
                        }
                    }
                    lastIndex = index;
                }

                if (row) {
                    notifyOnDeleted(min, max);
                } else {
                    //too much deleted so we fire a data changed
                    notifyOnDataChanged();
                }
            } else {
                notifyOnDeleted(removedIndexes.first());
            }
        }

    }

    private void notifyOnUpdated(int index) {
        notifyOnUpdated(index, index);
    }

    private void notifyOnUpdated(int index, int toIndex) {
        for (ListListener listener : this.listeners) {
            listener.onUpdated(index, toIndex);
        }
    }

    private void notifyOnInserted(int index) {
        notifyOnInserted(index, index);
    }

    private void notifyOnInserted(int index, int toIndex) {
        for (ListListener listener : this.listeners) {
            listener.onInserted(index, toIndex);
        }
    }

    private void notifyOnDeleted(int index) {
        notifyOnDeleted(index, index);
    }

    private void notifyOnDeleted(int index, int toIndex) {
        for (ListListener listener : this.listeners) {
            listener.onDeleted(index, toIndex);
        }
    }

    private void notifyOnDataChanged() {
        for (ListListener listener : this.listeners) {
            listener.onDataChanged();
        }
    }

    @Override
    public synchronized void onUpdate(T item) {
        //only saveXML in 10 sec interval on update!
        if (System.currentTimeMillis() - lastUpdate > 10000) {
            this.save();
            lastUpdate = System.currentTimeMillis();
        }
        this.updateList();
        final Integer itemId = this.itemsIndexLookup.get(item);
        if (itemId != null) {
            notifyOnUpdated(itemId);
        }
    }

    private static File getBackupFile(File file) {
        return new File(file.getParentFile(), file.getName() + ".bak");
    }
}
