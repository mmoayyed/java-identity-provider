/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.util;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opensaml.util.storage.StorageService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;

/**
 * A thread-safe implementation of {@link StorageService} that publishes event when items are added or removed from the
 * service.
 * 
 * An {@link AddEntryEvent} is published after an item has been added to the storage service. A {@link RemoveEntryEvent}
 * is published after an item has been removed from the storage service. These events are published in the root
 * application context, that is the highest ancestor, of the application context presented to this class.
 * 
 * @param <KeyType> object type of the keys
 * @param <ValueType> object type of the values
 */
public class EventingMapBasedStorageService<KeyType, ValueType> implements StorageService<KeyType, ValueType>,
        ApplicationContextAware {

    /** Spring application context. */
    private ApplicationContext appCtx;

    /** Backing map. */
    private Map<String, Map<KeyType, ValueType>> store;

    /** Constructor. */
    public EventingMapBasedStorageService() {
        store = new ConcurrentHashMap<String, Map<KeyType, ValueType>>();
    }

    /** {@inheritDoc} */
    public boolean contains(String partition, Object key) {
        if (partition == null || key == null) {
            return false;
        }

        if (store.containsKey(partition)) {
            return store.get(partition).containsKey(key);
        }

        return false;
    }

    /** {@inheritDoc} */
    public ValueType get(String partition, Object key) {
        if (partition == null || key == null) {
            return null;
        }

        if (store.containsKey(partition)) {
            return store.get(partition).get(key);
        }

        return null;
    }

    /** {@inheritDoc} */
    public Iterator<KeyType> getKeys(String partition) {
        return this.new PartitionEntryIterator(partition);
    }

    /** {@inheritDoc} */
    public Iterator<String> getPartitions() {
        return this.new PartitionIterator();
    }

    /** {@inheritDoc} */
    public ValueType put(String partition, KeyType key, ValueType value) {
        if (partition == null || key == null) {
            return null;
        }

        Map<KeyType, ValueType> partitionMap;
        synchronized (store) {
            partitionMap = store.get(partition);
            if (partitionMap == null) {
                partitionMap = new ConcurrentHashMap<KeyType, ValueType>();
                store.put(partition, partitionMap);
            }
        }

        ValueType replacedEntry = partitionMap.put(key, value);
        appCtx.publishEvent(new AddEntryEvent(this, partition, key, value));
        return replacedEntry;
    }

    /** {@inheritDoc} */
    public ValueType remove(String partition, KeyType key) {
        if (partition == null || key == null) {
            return null;
        }

        if (store.containsKey(partition)) {
            ValueType removedEntry = store.get(partition).remove(key);
            appCtx.publishEvent(new RemoveEntryEvent(this, partition, key, removedEntry));
            return removedEntry;
        }

        return null;
    }

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext ctx) {
        ApplicationContext rootContext = ctx;
        while (rootContext.getParent() != null) {
            rootContext = rootContext.getParent();
        }
        appCtx = rootContext;
    }

    /** An event indicating an item has been added to an storage service. */
    public static class AddEntryEvent<KeyType, ValueType> extends ApplicationEvent {

        /** Serial version UID. */
        private static final long serialVersionUID = -1939512157260059492L;

        /** Storage service to which the item was added. */
        private StorageService<KeyType, ValueType> storageService;

        /** Storage partition to which the item was added. */
        private String partition;

        /** Key to the added item. */
        private KeyType key;

        /** The added item. */
        private ValueType value;

        /**
         * Constructor.
         * 
         * @param storageService storage service to which an item was added
         * @param partition partition to which the entry was added
         * @param key key of the added item
         * @param value added item
         */
        public AddEntryEvent(StorageService<KeyType, ValueType> storageService, String partition, KeyType key,
                ValueType value) {
            super(storageService);
            this.storageService = storageService;
            this.partition = partition;
            this.key = key;
            this.value = value;
        }

        /**
         * Gets the storage service to which an item was added.
         * 
         * @return storage service to which an item was added
         */
        public StorageService<KeyType, ValueType> getStorageService() {
            return storageService;
        }

        /**
         * Gets the partition to which the entry was added.
         * 
         * @return partition to which the entry was added
         */
        public String getPartition() {
            return partition;
        }

        /**
         * Gets the key of the added item.
         * 
         * @return key of the added item
         */
        public KeyType getKey() {
            return key;
        }

        /**
         * Gets the added item.
         * 
         * @return added item
         */
        public ValueType getValue() {
            return value;
        }
    }

    /** An event indicating an item has been removed from an storage service. */
    public static class RemoveEntryEvent<KeyType, ValueType> extends ApplicationEvent {

        /** Serial version UID. */
        private static final long serialVersionUID = 7414605158323325366L;

        /** Storage service to which the item was removed. */
        private StorageService<KeyType, ValueType> storageService;

        /** Storage partition to which the item was removed. */
        private String partition;

        /** Key to the removed item. */
        private KeyType key;

        /** The removed item. */
        private ValueType value;

        /**
         * Constructor.
         * 
         * @param storageService storage service to which an item was removed
         * @param partition partition to which the entry was removed
         * @param key key of the removed item
         * @param value removed item
         */
        public RemoveEntryEvent(StorageService<KeyType, ValueType> storageService, String partition, KeyType key,
                ValueType value) {
            super(storageService);
            this.storageService = storageService;
            this.partition = partition;
            this.key = key;
            this.value = value;
        }

        /**
         * Gets the storage service to which an item was removed.
         * 
         * @return storage service to which an item was removed
         */
        public StorageService<KeyType, ValueType> getStorageService() {
            return storageService;
        }

        /**
         * Gets the partition to which the entry was removed.
         * 
         * @return partition to which the entry was removed
         */
        public String getPartition() {
            return partition;
        }

        /**
         * Gets the key of the removed item.
         * 
         * @return key of the removed item
         */
        public KeyType getKey() {
            return key;
        }

        /**
         * Gets the removed item.
         * 
         * @return removed item
         */
        public ValueType getValue() {
            return value;
        }
    }

    /** An iterator over the partitions of the storage service. */
    public class PartitionIterator implements Iterator<String> {

        /** Iterator over the partitions in the backing store. */
        private Iterator<String> partitionItr;

        /** Current partition. */
        private String currentParition;

        /** Constructor. */
        public PartitionIterator() {
            partitionItr = store.keySet().iterator();
        }

        /** {@inheritDoc} */
        public boolean hasNext() {
            return partitionItr.hasNext();
        }

        /** {@inheritDoc} */
        public String next() {
            currentParition = partitionItr.next();
            return currentParition;
        }

        /** {@inheritDoc} */
        public void remove() {
            Iterator<KeyType> partitionEntries = getKeys(currentParition);
            while (partitionEntries.hasNext()) {
                partitionEntries.next();
                partitionEntries.remove();
            }
            store.remove(currentParition);
        }
    }

    /** An iterator over the entries of a partition of the storage service. */
    public class PartitionEntryIterator implements Iterator<KeyType> {

        /** Partition on which we are operating. */
        private String partition;

        /** Iterator of keys within the partition. */
        private Iterator<KeyType> keysItr;

        /** Current key within the iteration. */
        private KeyType currentKey;

        /**
         * Constructor.
         * 
         * @param partition partition upon which this iterator operates
         */
        public PartitionEntryIterator(String partition) {
            this.partition = partition;
            keysItr = store.get(partition).keySet().iterator();
        }

        /** {@inheritDoc} */
        public boolean hasNext() {
            return keysItr.hasNext();
        }

        /** {@inheritDoc} */
        public KeyType next() {
            currentKey = keysItr.next();
            return currentKey;
        }

        /** {@inheritDoc} */
        public void remove() {
            EventingMapBasedStorageService.this.remove(partition, currentKey);
        }
    }
}