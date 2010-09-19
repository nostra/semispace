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

    public static synchronized HolderContainer retrieveContainer() {
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
            toReturn = head.removeHolderById(id);
            if ( head.size() < 1 ) {
                heads.remove(className);
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
            if ( n == null ) {
                return null;
            }
            return n.findById(id);
        } finally {
            rwl.readLock().unlock();
        }
    }

    public void addHolder(Holder add) {
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
}
