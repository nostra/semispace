/*
 * ============================================================================
 *
 *  File:     NotificationTest.java
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

import junit.framework.TestCase;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class NotificationTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(NotificationTest.class);

    /**
     * Few listeners and a larger number of inserts.
     */
    public void testNotificationAndTake() throws InterruptedException {
        ToBeNotified a = new ToBeNotified(false);
        ToBeNotified b = new ToBeNotified(false);
        ToBeNotified c = new ToBeNotified(false);

        SemiSpaceInterface space = SemiSpace.retrieveSpace();
        SemiEventRegistration notifyA = space.notify(new NoticeA(), a, 10000);
        a.setNotify( notifyA );
        SemiEventRegistration notifyB = space.notify(new NoticeB(), b, 10000);
        b.setNotify( notifyB );
        SemiEventRegistration notifyC = space.notify(new NoticeC(), c, 10000);
        c.setNotify( notifyC );

        final int numinserts = 5000;
        log.debug("Before inserting {} elements into space", Integer.valueOf(numinserts));
        for ( int i=0 ; i < numinserts ; i++ ) {
            insertIntoSpace(space, i);            
        }
        log.debug("Active threads "+Thread.activeCount());
        // May experience slight lag due to asynchronious operations.
        boolean isOk = false;
        int count = 35;
        while ( !isOk && count-- > 0 ) {
            if ( a.getNotified() != numinserts || b.getNotified() != numinserts ||
                    c.getNotified() != numinserts) {
                log.debug("Sleeping slightly due to async operation");
                Thread.sleep(50);
            } else {
                isOk = true;
            }
        }

        log.debug("Insertion finished. ");

        log.debug(a.getNotified()+" "+b.getNotified()+" "+c.getNotified());
        assertEquals( numinserts, a.getNotified());
        assertEquals( numinserts, b.getNotified());
        assertEquals( numinserts, c.getNotified());

        while ( space.takeIfExists(new NoticeA()) != null ||
                space.takeIfExists(new NoticeB()) != null ||
                space.takeIfExists(new NoticeC()) != null) {
            // Just cleaning away objects from space
        }
    }

    /**
     * A large number of listeners, and a smaller number of inserts
     */
    public void testQuantityOfNotification() throws InterruptedException {
        final int numberOfListeners = 5000;
        ToBeNotified a[] = new ToBeNotified[numberOfListeners];
        ToBeNotified b[] = new ToBeNotified[numberOfListeners];
        SemiSpaceInterface space = SemiSpace.retrieveSpace();
        List<SemiEventRegistration> regs = new ArrayList<SemiEventRegistration>();
        for ( int i=0 ; i < numberOfListeners ; i++ ) {
            a[i] = new ToBeNotified(false);
            b[i] = new ToBeNotified(false);
            regs.add( space.notify(new NoticeA(), a[i], 10000));
            regs.add( space.notify(new NoticeB(), b[i], 10000));
        }
        final int numinserts = 50;
        log.debug("Before insertion of {} elements", Integer.valueOf(numinserts));
        for ( int i=0 ; i < numinserts ; i++ ) {
            insertIntoSpace(space, i);
        }
        log.debug("Active threads "+Thread.activeCount());
        log.debug("Insertion finished, sleeping 200ms");
        Thread.sleep(200);
        log.debug("Active threads "+Thread.activeCount());
        log.debug("Cancelling leases");
        for ( SemiEventRegistration er : regs ) {
            er.getLease().cancel();
        }
        log.debug("Lease cancelled");
        assertEquals(numinserts, a[0].getNotified());
        assertEquals(numinserts, b[b.length-1].getNotified());
        while ( space.takeIfExists(new NoticeA()) != null ||
                space.takeIfExists(new NoticeB()) != null ||
                space.takeIfExists(new NoticeC()) != null) {
            // Just cleaning away objects from space
        }
    }

    /**
     * Repetition test in order to examine stress slightly better.
     * @throws InterruptedException
     */
    public void deactivate___testRepeatedTest() throws InterruptedException {
        testQuantityOfNotification();
        testQuantityOfNotification();
        for ( int i=0 ; i < 20 ; i++ ) {
            testNotificationAndTake();
        }
    }

    private void insertIntoSpace(SemiSpaceInterface space, int i) {
        NoticeA na = new NoticeA();
        na.setField("a "+i);
        space.write(na, 1000);
        
        NoticeB nb = new NoticeB();
        nb.setField("b "+i);
        space.write(nb, 1000);
        
        NoticeC nc = new NoticeC();
        nc.setField("c "+i);
        space.write(nc, 1000);
    }
}
