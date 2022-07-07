/*
 * Copyright 2010 Erlend Nossum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.semispace.take;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class Storage {
    private static final Logger log = LoggerFactory.getLogger(Storage.class);
    private static final Storage instance = new Storage();
    private final StringBuilder errors = new StringBuilder();

    private final ConcurrentHashMap<String, Item> items = new ConcurrentHashMap<String, Item>();
    private final LinkedBlockingQueue<String> writers = new LinkedBlockingQueue<String>();

    public static Storage getInstance() {
        return instance;
    }

    public synchronized String addWriter() {
        String writer = null;

        int wid = writers.size() + 1;
        writer = String.valueOf(wid);
        writers.add(writer);

        return writer;
    }

    public synchronized void addItem(Item item) {
        Item old = items.put(item.getValue(), item);
        if (old != null) {
            errors.append("Item " + item.getValue() + " was already in the map\n");
        }
    }

    public synchronized void removeItem(Item item) {
        if (!items.remove(item.getValue(), item)) {
            errors.append("Item removal failed: " + item.getValue() + "\n");
        }
    }

    public synchronized int dumpItems() {
        int count = 0;
        for (String k : items.keySet()) {
            log.error("Item not collected: {}", k);
            count++;
        }
        return count;
    }

    public synchronized String getErrors() {
        return errors.toString();
    }
}
