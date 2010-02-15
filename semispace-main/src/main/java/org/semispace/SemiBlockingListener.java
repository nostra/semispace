/*
 * ============================================================================
 *
 *  File:     SemiBlockingListener.java
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
 *  Created:      30. des.. 2007
 * ============================================================================ 
 */

package org.semispace;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;

/**
 * Block until notification or timeout.
 */
public class SemiBlockingListener implements SemiEventListener {
    private static final Logger log = LoggerFactory.getLogger(SemiBlockingListener.class);
    private transient CountDownLatch latch;
    private transient Boolean beenNotified;

    public SemiBlockingListener( ) {
        reset();
    }

    /**
     * Resetting notified state - necessary for re-blocking
     */
    public void reset() {
        this.beenNotified = Boolean.FALSE;
    }

    public void notify(SemiEvent theEvent) {
        //log.debug("Notify");
        if ( theEvent instanceof SemiAvailabilityEvent ) {
            this.beenNotified = Boolean.TRUE;
            unblock();
        }
    }

    private void unblock() {
        if ( latch != null ) {
            //log.debug("Un-block");
            latch.countDown();
        }
    }

    /**
     * If re-blocking, call reset method first
     * @see #reset() 
     */
    public void block(long msToWait) {
        latch = new CountDownLatch(1);
        try {
            if ( ! beenNotified.booleanValue() ) {
                latch.await(msToWait*1000, TimeUnit.NANOSECONDS);
            }
        } catch (InterruptedException e) {
            log.error("Got interrupted exception (which unblocks await)");
        }
    }

    public boolean hasBeenNotified() {
        return beenNotified.booleanValue();
    }

}
