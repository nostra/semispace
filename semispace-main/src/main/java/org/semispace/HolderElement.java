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

public class HolderElement {
    private Holder holder;
    private HolderElement next;
    
    public HolderElement( Holder holder ) {
        this.holder = holder;
    }
    
    public  HolderElement next() {
        return next;
    }
    
    public  Holder getHolder() {
        return holder;
    }

    /**
     * Iterating until able to remove <b>next</b> element, which
     * implies that the holder container is responsible for removing
     * head. 
     */
    public  Holder removeHolderById( long id ) {
        HolderElement found = null;
        HolderElement n = this; 
        while ( n != null && found == null && n.next != null ) {
            if ( n.next.holder.getId() == id ) {
                found = n.next;
                n.next = found.next;
            } else {
                n = n.next;
            }
        }
        if ( found != null ) {
            return found.getHolder();
        }
        return null; 
    }

    /**
     * Searching for holder elements with given ID
     */    
    public  Holder findById(long id) {
        HolderElement found = null;
        HolderElement n = this; 
        while ( n != null && found == null ) {
            if ( n.holder != null && n.holder.getId() == id ) {
                found = n;
            }
            n = n.next;
        }
        if ( found != null ) {
            return found.holder;
        }
        return null;
    }

    public  void addHolder(Holder add ) {
        addAsNext( add );        
    }

    private void addAsNext(Holder add) {
        if ( add == null ) {
            throw new RuntimeException("Illegal to add null");
        }
        HolderElement oldnext = next;
        next = new HolderElement(add);
        next.next = oldnext;
    }
}
