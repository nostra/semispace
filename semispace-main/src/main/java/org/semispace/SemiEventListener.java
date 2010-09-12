/*
 * ============================================================================
 *
 *  File:     SemiEventListener.java
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
 *  Created:      23. des.. 2007
 * ============================================================================ 
 */

package org.semispace;

import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;

/**
 * Event listener interface
 */
public interface SemiEventListener<E extends SemiEvent> {
    /**
     * <b>Notice</b> that the notification object is an interface,
     * and you will need to figure out the concrete type yourself.
     * @see SemiAvailabilityEvent
     * @see SemiExpirationEvent
     */
    public void notify(E theEvent);
}
