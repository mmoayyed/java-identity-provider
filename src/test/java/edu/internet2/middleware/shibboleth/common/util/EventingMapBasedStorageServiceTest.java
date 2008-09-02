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

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;

import edu.internet2.middleware.shibboleth.common.TestCaseBase;
import edu.internet2.middleware.shibboleth.common.util.EventingMapBasedStorageService.AddEntryEvent;
import edu.internet2.middleware.shibboleth.common.util.EventingMapBasedStorageService.RemoveEntryEvent;

/**
 * 
 */
public class EventingMapBasedStorageServiceTest extends TestCaseBase {
    
    public void testStorageService() {
        AddEntryListener addListener = new AddEntryListener();
        RemoveEntryListener removeListener = new RemoveEntryListener();

        GenericApplicationContext appCtx = new GenericApplicationContext();
        EventingMapBasedStorageService<String, String> storageService = new EventingMapBasedStorageService<String, String>();
        storageService.setApplicationContext(appCtx);
        appCtx.addApplicationListener(addListener);
        appCtx.addApplicationListener(removeListener);
        appCtx.refresh();

        String partition = "partition";
        String value = "value";

        String key1 = "string1";
        storageService.put(partition, key1, value);

        String key2 = "string2";
        storageService.put(partition, key2, value);

        String key3 = "string3";
        storageService.put(partition, key3, value);

        String key4 = "string4";
        storageService.put(partition, key4, value);

        String key5 = "string5";
        storageService.put(partition, key5, value);

        assertEquals(5, addListener.getCount());
        assertEquals(0, removeListener.getCount());
        assertEquals(true, storageService.contains(partition, key1));
        assertEquals(true, storageService.contains(partition, key2));
        assertEquals(true, storageService.contains(partition, key3));
        assertEquals(true, storageService.contains(partition, key4));
        assertEquals(true, storageService.contains(partition, key5));

        storageService.remove(partition, key1);
        assertEquals(5, addListener.getCount());
        assertEquals(1, removeListener.getCount());
        assertEquals(false, storageService.contains(partition, key1));
        assertEquals(true, storageService.contains(partition, key2));
        assertEquals(true, storageService.contains(partition, key3));
        assertEquals(true, storageService.contains(partition, key4));
        assertEquals(true, storageService.contains(partition, key5));

        Iterator<String> keysItr = storageService.getKeys(partition);
        keysItr.next();
        keysItr.remove();
        assertEquals(5, addListener.getCount());
        assertEquals(2, removeListener.getCount());

        Iterator<String> partiationsItr = storageService.getPartitions();
        partiationsItr.next();
        partiationsItr.remove();
        assertEquals(5, addListener.getCount());
        assertEquals(5, removeListener.getCount());
        assertEquals(false, storageService.contains(partition, key1));
        assertEquals(false, storageService.contains(partition, key2));
        assertEquals(false, storageService.contains(partition, key3));
        assertEquals(false, storageService.contains(partition, key4));
        assertEquals(false, storageService.contains(partition, key5));
    }

    public class AddEntryListener implements ApplicationListener {

        private int count;

        public AddEntryListener() {
            count = 0;
        }

        /** {@inheritDoc} */
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof AddEntryEvent) {
                count++;
            }
        }

        public int getCount() {
            return count;
        }
    }

    public class RemoveEntryListener implements ApplicationListener {

        private int count;

        public RemoveEntryListener() {
            count = 0;
        }

        /** {@inheritDoc} */
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof RemoveEntryEvent) {
                count++;
            }
        }

        public int getCount() {
            return count;
        }
    }
}