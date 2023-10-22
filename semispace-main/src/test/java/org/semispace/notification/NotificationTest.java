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

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.semispace.AlternateButEqual;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.StressTestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@TestInstance(Lifecycle.PER_CLASS)
public class NotificationTest {
    private static final Logger log = LoggerFactory.getLogger(NotificationTest.class);
    private SemiSpaceInterface space;
    private ExecutorService tpe;

    @BeforeAll
    protected void setUp() {
        space = SemiSpace.retrieveSpace();
        tpe = (ThreadPoolExecutor) ((SemiSpace) space).getAdmin().getThreadPool();
    }

    @AfterEach
    protected void tearDown() {
        // Just remove any superfluous elements. Not really testing the data
        // type - it is the notification which is interesting here
        while (space.takeIfExists(new NoticeA()) != null ||
                space.takeIfExists(new NoticeB()) != null ||
                space.takeIfExists(new NoticeC()) != null) {
            // Nada
        }
    }

    /**
     * Few listeners and a larger number of inserts.
     */
    public void testNotificationAndTake() {
        ToBeNotified a = new ToBeNotified(false);
        ToBeNotified b = new ToBeNotified(false);
        ToBeNotified c = new ToBeNotified(false);

        SemiEventRegistration notifyA = space.notify(new NoticeA(), a, 10000);
        a.setNotify(notifyA);
        SemiEventRegistration notifyB = space.notify(new NoticeB(), b, 10000);
        b.setNotify(notifyB);
        SemiEventRegistration notifyC = space.notify(new NoticeC(), c, 10000);
        c.setNotify(notifyC);

        final int numinserts = StressTestConstants.NUMBER_OF_ELEMENTS_OR_LISTENERS;
        log.debug("Before inserting {} elements into space", Integer.valueOf(numinserts));
        for (int i = 0; i < numinserts; i++) {
            insertIntoSpace(space, i);
        }
        log.debug("Active threads " + Thread.activeCount());
        // May experience slight lag due to asynchronous operations.
        boolean isOk = false;
        int count = 35;
        while (!isOk && count-- > 0) {
            if (a.getNotified() != numinserts || b.getNotified() != numinserts ||
                    c.getNotified() != numinserts) {
                log.debug("Sleeping slightly due to async operation");
                // Pausing:
                space.read(new AlternateButEqual(), 200);
            } else {
                isOk = true;
            }
        }

        log.debug("Insertion finished. ");
        assertTrue(notifyA.getLease().cancel());
        assertTrue(notifyB.getLease().cancel());
        assertTrue(notifyC.getLease().cancel());
        assertFalse(notifyA.getLease().cancel());

        log.debug(a.getNotified() + " " + b.getNotified() + " " + c.getNotified());
        assertEquals(numinserts, a.getNotified());
        assertEquals(numinserts, b.getNotified());
        assertEquals(numinserts, c.getNotified());

        NoticeA aTaken;
        NoticeB bTaken;
        NoticeC cTaken;
        Set<String> noDups = new HashSet<String>();
        do {
            aTaken = space.takeIfExists(new NoticeA());
            bTaken = space.takeIfExists(new NoticeB());
            cTaken = space.takeIfExists(new NoticeC());
            if (aTaken != null) {
                assertTrue(noDups.add(aTaken.getField()));
            }
            if (bTaken != null) {
                assertTrue(noDups.add(bTaken.getField()));
            }
            if (cTaken != null) {
                assertTrue(noDups.add(cTaken.getField()));
            }
        } while (aTaken != null || bTaken != null || cTaken != null);
        assertEquals(3 * StressTestConstants.NUMBER_OF_ELEMENTS_OR_LISTENERS, noDups.size());
    }

    /**
     * A large number of listeners, and a smaller number of inserts
     */
    public void testQuantityOfNotification() throws InterruptedException {
        final int numberOfListeners = StressTestConstants.NUMBER_OF_ELEMENTS_OR_LISTENERS;
        final int numberOfWrittenObjects = ((SemiSpace) space).numberOfWrite();

        // log.debug("Active before starting testing:" + tpe.getActiveCount() + ", Main thread count: " + Thread.activeCount());
        stabilizeThreadNumber();

        ToBeNotified[] a = new ToBeNotified[numberOfListeners];
        ToBeNotified[] b = new ToBeNotified[numberOfListeners];
        List<SemiEventRegistration> regs = new ArrayList<SemiEventRegistration>();
        for (int i = 0; i < numberOfListeners; i++) {
            a[i] = new ToBeNotified(false);
            b[i] = new ToBeNotified(false);
            regs.add(space.notify(new NoticeA(), a[i], 90000));
            regs.add(space.notify(new NoticeB(), b[i], 90000));
        }
        final int numinserts = 50;
        // log.debug("Before insertion of {} elements. Active threads " + tpe.getActiveCount() + ", Main thread count: " + Thread.activeCount(), Integer.valueOf(numinserts));
        for (int i = 0; i < numinserts; i++) {
            insertIntoSpace(space, i);
            // //log.debug("Insertion into space. Active threads "+tpe.getActiveCount()+", Main thread count: "+Thread.activeCount());
        }
        assertEquals(numberOfWrittenObjects + (3 * numinserts), ((SemiSpace) space).numberOfWrite());

        // log.debug("Active threads " + tpe.getActiveCount() + ", Main thread count: " + Thread.activeCount());
        log.debug("Insertion finished, pausing for 400ms");
        space.read(new AlternateButEqual(), 400);

        stabilizeThreadNumber();
        // log.debug("Active threads " + tpe.getActiveCount() + ", Main thread count: " + Thread.activeCount());

        // log.debug("After sleep: Active threads " + tpe.getActiveCount() + ", Main thread count: " + Thread.activeCount());
        int errorCount = 0;
        for (int i = 0; i < a.length; i++) {
            if (numinserts != a[i].getNotified() || numinserts != b[i].getNotified()) {
                /*
                log.error("Warning: Discrepancy. Trying to resolve by sleeping a bit more");
                // log.debug("Before sleeping: Active threads "+tpe.getActiveCount()+", Main thread count: "+Thread.activeCount());
                Thread.sleep(1000);
                stabilizeThreadNumber();
                // log.debug("After sleeping (at "+i+" of "+a.length+"): Active threads "+tpe.getActiveCount()+", Main thread count: "+Thread.activeCount());
                */
                errorCount++;
            }
        }

        log.debug("Error count after slight sleep is " + errorCount);

        log.debug("Cancelling " + regs.size() + " leases");
        for (SemiEventRegistration er : regs) {
            er.getLease().cancel();
        }
        log.debug("Leases cancelled");
        // log.debug("Active threads " + tpe.getActiveCount() + ", Main thread count: " + Thread.activeCount());


        for (int i = 0; i < a.length; i++) {
            assertEquals(numinserts, a[i].getNotified(), "At element a" + i + " notified number had a discrepancy. Element b" + i + ", incidentally, was " + b[i].getNotified() + ".");
            assertEquals(numinserts, b[i].getNotified(), "At element b" + i + " notified number had a discrepancy. Element a" + i + ", incidentally, was " + a[i].getNotified() + ".");
        }
        NoticeA aTaken;
        NoticeB bTaken;
        NoticeC cTaken;
        Set<String> noDups = new HashSet<String>();
        do {
            aTaken = space.takeIfExists(new NoticeA());
            bTaken = space.takeIfExists(new NoticeB());
            cTaken = space.takeIfExists(new NoticeC());
            if (aTaken != null) {
                assertTrue(noDups.add(aTaken.getField()));
            }
            if (bTaken != null) {
                assertTrue(noDups.add(bTaken.getField()));
            }

        } while (aTaken != null || bTaken != null || cTaken != null);
        assertEquals(2 * numinserts, noDups.size());
    }

    private void stabilizeThreadNumber() throws InterruptedException {
        // int lastCheckActive = tpe.getActiveCount() + Thread.activeCount();
        /*
        for (int i = 0; i < 100; i++) {
            Thread.sleep(500);
            // int updatedActive = tpe.getActiveCount() + Thread.activeCount();
            if (updatedActive == lastCheckActive) {
                break;
            }
            lastCheckActive = updatedActive;
        }
         */
    }

    /**
     * Expiration of listener
     */
    @Test
    public void testListenerExpiration() {
        ToBeNotified a = new ToBeNotified(false);

        SemiEventRegistration notifyA = space.notify(new NoticeA(), a, 150);
        a.setNotify(notifyA);

        assertNull(space.take(new NoticeA(), 160), "Forcing notification object time out");
        insertIntoSpace(space, 101010);
        assertNotNull(space.takeIfExists(new NoticeA()), "Recently inserted element should not be null");
        assertFalse(notifyA.getLease().cancel(), "When cancelling a timed out lease, the result should be false");
    }

    /**
     * Repetition test in order to examine stress slightly better.
     */
    public void deactivated_testRepeatedTest() throws InterruptedException {
        testQuantityOfNotification();
        testQuantityOfNotification();
        for (int i = 0; i < 20; i++) {
            testNotificationAndTake();
        }
    }

    private void insertIntoSpace(SemiSpaceInterface space, int i) {
        NoticeA na = new NoticeA();
        na.setField("a " + i);
        space.write(na, StressTestConstants.NUMBER_OF_ELEMENTS_OR_LISTENERS * 100);

        NoticeB nb = new NoticeB();
        nb.setField("b " + i);
        space.write(nb, StressTestConstants.NUMBER_OF_ELEMENTS_OR_LISTENERS * 100);

        NoticeC nc = new NoticeC();
        nc.setField("c " + i);
        space.write(nc, StressTestConstants.NUMBER_OF_ELEMENTS_OR_LISTENERS * 100);
    }
}
