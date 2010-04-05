/*
 * ============================================================================
 *
 *  File:     HolderElement.java
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
 *  Created:      May 2, 2008
 * ============================================================================ 
 */

package org.semispace;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HolderElement implements Iterable<Holder>{
    private Map<Long, Holder> elements = new ConcurrentHashMap<Long, Holder>();

    public synchronized int size() {
        return elements.size();
    }

    public static synchronized HolderElement createNewCollection(Holder holder) {
        HolderElement hc = new HolderElement();
        hc.addHolder(holder);
        return hc;
    }

    public synchronized Holder removeHolderById( long id ) {
        Holder found = elements.remove(Long.valueOf(id));
        return found;
    }

    /**
     * Searching for holder elements with given ID
     */
    public synchronized Holder findById(long id) {
        Holder found = elements.get(Long.valueOf(id));
        return found;
    }

    public synchronized void addHolder(Holder add ) {
        Holder old = elements.put( Long.valueOf(add.getId()), add);
        if ( old != null ) {
            throw new RuntimeException("Unexpected duplication id IDs. Found twice: "+old.getId());
        }
    }

    public synchronized Holder[] toArray() {
        return elements.values().toArray( new Holder[0]);
    }

    @Override
    public synchronized Iterator<Holder> iterator() {
        // TODO Will this be thread safe?
        /*
        List<Holder> defensive = new ArrayList();
        defensive.addAll(elements.values());
        return defensive.iterator();
        */
        return elements.values().iterator();
    }
}
