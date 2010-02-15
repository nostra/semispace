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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Container for holder elements.
 */
public class HolderContainer {
    private Map<String, HolderElement> heads = null;

    /**
     * Read / write lock
     */
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private static HolderContainer instance = null;
    
    private HolderContainer() {
        heads = new HashMap<String, HolderElement>();
    }

    public synchronized static HolderContainer retrieveContainer() {
        if ( instance == null ) {
            instance = new HolderContainer();
        }
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
            
            if (head.getHolder().getId() == id) {
                // First element shall be removed
                toReturn = head.getHolder();
                head = head.next();
                // Reset head entry in map
                if ( head == null ) {
                    heads.remove(className);
                } else {
                    heads.put(className, head);
                }
            } else {
                // Possibly removing one in chain
                toReturn = head.removeHolderById(id);
            }
                
        } finally {
            rwl.writeLock().unlock();
        }
        return toReturn;
    }

    public Holder findById(long id, String className) {
        rwl.readLock().lock();

        try {
            HolderElement n = heads.get(className);
            while (n != null) {
                Holder found = n.findById(id);
                if ( found != null ) {
                    return n.getHolder();
                }
                n = n.next();
            }
        } finally {
            rwl.readLock().unlock();
        }
        return null;
    }

    public void addHolder(Holder add) {
        rwl.writeLock().lock();
        try {
            if (add == null) {
                throw new RuntimeException("Illegal to add null");
            }
            if ( add.getClassName() == null ) {
                throw new RuntimeException("Need classname in holder with contents "+add.getXml());
            }
            HolderElement head = heads.get( add.getClassName() );
            if (head == null) {
                head = new HolderElement(add);
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
                HolderElement c = head;
                while (c != null) {
                    size++;
                    c = c.next();
                }
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
            while ( next != null ) { 
                Holder elem = next.getHolder();
                if (id == elem.getId()  ) {
                    return elem;
                }

                next = next.next();
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
            while ( next != null ) { 
                Holder elem = next.getHolder();
                allIds.add(Long.valueOf( elem.getId() ));
                next = next.next();
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
}
