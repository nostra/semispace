/*
 * ============================================================================
 *
 *  File:     EventDistributor.java
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
 *  Created:      Apr 10, 2008
 * ============================================================================ 
 */

package org.semispace;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.semispace.event.SemiEvent;
import org.terracotta.annotations.InstrumentedClass;


/**
 * Used for distributing event through terracotta. 
 */
@InstrumentedClass
public class DistributedEvent {

    private String holderClassName;
    private SemiEvent event;
    private Map<String, String> entrySet;
    
    public DistributedEvent(String holderClassName, SemiEvent event, Map<String, String> map) {
        this.holderClassName = holderClassName;
        this.event = event;
        this.entrySet = map;
    }

    public SemiEvent getEvent() {
        return this.event;
    }

    public Set<Entry<String, String>> getEntrySet() {
        return this.entrySet.entrySet();
    }

    public String getHolderClassName() {
        return holderClassName;
    }

}
