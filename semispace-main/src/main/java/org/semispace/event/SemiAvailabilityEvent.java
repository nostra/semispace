/*
 * ============================================================================
 *
 *  File:     SemiAvailabilityEvent.java
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
 *  Created:      29. des.. 2007
 * ============================================================================ 
 */

package org.semispace.event;



/**
 * An object has become available.
 */
public class SemiAvailabilityEvent extends SemiEvent {
    private long id;

    /**
     * Interim object. This is the object which is used for the distributed notification.
     * @param id Holder id
     */
    public SemiAvailabilityEvent(  long id ) {
        this.id = id;
    }
    
	@Override
    public long getId() {
        return this.id;
    }
}
