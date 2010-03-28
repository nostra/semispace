/*
 * ============================================================================
 *
 *  File:     ToBeNotified.java
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
 *  Created:      Feb 4, 2009
 * ============================================================================ 
 */

package org.semispace.notification;

import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;

public class ToBeNotified implements SemiEventListener {
    private int notified;
    private boolean toCancelLease;
    private SemiEventRegistration lease;

    public ToBeNotified(boolean toCancelLease) {
        this.toCancelLease = toCancelLease;
    }

    public int getNotified() {
        return this.notified;
    }

    /**
     * synchronized in order to avoid it being called twice (as it may be removed)
     */
    public void notify(SemiEvent theEvent) {
        
        if ( theEvent instanceof SemiAvailabilityEvent ) {
            notified++;
            if (toCancelLease) {
                lease.getLease().cancel();
            }
        }
    }

    public void setNotify(SemiEventRegistration lease) {
        this.lease = lease;
    }

}
