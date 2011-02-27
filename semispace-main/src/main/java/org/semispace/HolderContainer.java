/*
 * ============================================================================
 *
 *  File:     HolderContainer.java
 *----------------------------------------------------------------------------
 *
 * Copyright 2008 Erlend Nossum
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
 *
 *  Description:  See javadoc below
 *
 *  Created:      Apr 27, 2008
 * ============================================================================ 
 */

package org.semispace;

import org.semispace.exception.SemiSpaceObjectException;
import org.semispace.exception.SemiSpaceUsageException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Container for holder elements.
 */
public class HolderContainer {
    private AtomicLong idseq = new AtomicLong();

    private Map<String, HolderElement> heads = null;

    /**
     * Read / write lock
     */
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private static HolderContainer instance = new HolderContainer();
    
    private HolderContainer() {
        heads = new ConcurrentHashMap<String, HolderElement>();
    }

    public static synchronized HolderContainer retrieveContainer() {
        return instance;
    }
    
    public HolderElement next(String className) {
        rwl.writeLock().lock();
        try {
            return heads.get(className);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public Holder removeHolderById(long id, String className) {
        Holder toReturn = null;
        rwl.writeLock().lock();
        try {
            HolderElement head = heads.get(className);
            if ( head == null ) {
                return null;
            }
            toReturn = head.removeHolderById(id);
            if ( (idseq.longValue() % 5000) == 0 && head.size() < 1 ) {
                // It may not be deterministic when this actually occurs, but that does not matter. 
                removeEmptyHeads();
            }

        } finally {
            rwl.writeLock().unlock();
        }
        return toReturn;
    }

    /**
     * Instance need to have been locked in beforehand.
     * Intended to be used occasionally in order to remove empty heads.
     */
    private void removeEmptyHeads() {
        List<String> toPurge = new ArrayList<String>();
        for ( String name : heads.keySet()) {
            HolderElement head = heads.get( name);
            if ( !head.isWaiting() && head.size() < 1 ) {
                toPurge.add(name);
            }
        }
        for ( String name : toPurge ) {
            heads.remove(name);
        }
    }

    public Holder findById(long id, String className) {
        rwl.readLock().lock();

        try {
            HolderElement n = heads.get(className);
            if ( n == null ) {
                return null;
            }
            return n.findById(id);
        } finally {
            rwl.readLock().unlock();
        }
    }

    /**
     * Protected for the benefit of junit tests.
     */
    protected void addHolder(Holder add) {
        rwl.writeLock().lock();
        try {
            if (add == null) {
                throw new SemiSpaceUsageException("Illegal to add null");
            }
            if ( add.getClassName() == null ) {
                throw new SemiSpaceObjectException("Need classname in holder with contents "+add.getXml());
            }
            HolderElement head = heads.get( add.getClassName() );
            if (head == null) {
                head = HolderElement.createNewCollection(add);
                heads.put( add.getClassName(), head);
            } else {
                head.addHolder(add);
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Method presumed called on first object, which is the holder object. Returning count, excluding holder.
     */
    public int size() {
        rwl.readLock().lock();
        try {
            if (heads == null) {
                return 0;
            }
            int size = 0;
            
            for ( HolderElement head : heads.values() ) {
                size += head.size();
            }
            return size;
        } finally {
            rwl.readLock().unlock();
        }

    }
    
    public String[] retrieveGroupNames() {
        rwl.readLock().lock();
        String[] result = null;
        try {
            result = heads.keySet().toArray(new String[0]);
        } finally {
            rwl.readLock().unlock();
        }
        return result;
    }

    public Holder readHolderWithId(long id) {
        String[] cnames = retrieveClassNames();
        for (String lookup : cnames ) {
            HolderElement next = next(lookup);
            Holder toReturn = next.findById(id);
            if ( toReturn != null ) {
                return toReturn;
            }
        }
        return null;
    }
    
    /**
     * Return all ids present. Notice that this method will
     * be rather network expensive, and is only intended to 
     * be used for persistence purposes.
     */
    public Long[] findAllHolderIds() {
        List<Long> allIds = new ArrayList<Long>();
        String[] cnames = retrieveClassNames();
        for (String lookup : cnames ) {
            HolderElement next = next(lookup);
            rwl.readLock().lock();
            try {
                for ( Holder elem : next.toArray()) {
                    allIds.add(Long.valueOf( elem.getId() ));
                }

            } finally {
                rwl.readLock().unlock();
            }
        }
        return allIds.toArray(new Long[0]);
    }
    
    private String[] retrieveClassNames() {
        rwl.readLock().lock();
        String[] cnames = null;
        try {
            cnames = heads.keySet().toArray(new String[0]);
        } finally {
            rwl.readLock().unlock();
        }

        return cnames;
    }

    public Holder addHolder(String xml, long liveUntil, String entryClassName, Map<String, String> searchMap) {
        // Methods used herein are thread safe, and therefore no reason to lock at this point.
        long holderId = incrementReturnNextId();
        Holder holder = new Holder(xml, liveUntil, entryClassName, holderId, searchMap);
        addHolder(holder);
        return holder;
    }

    public long incrementReturnNextId() {
        return idseq.incrementAndGet();
    }

    public void waitHolder(String className, long timeout) {
        HolderElement e = null;
        rwl.writeLock().lock();
        try {
            e = heads.get(className);
            if (e == null) {
                e = new HolderElement();
                heads.put(className, e);
            }
        } finally {
            rwl.writeLock().unlock();
        }
        e.waitHolder(timeout);
    }
}
