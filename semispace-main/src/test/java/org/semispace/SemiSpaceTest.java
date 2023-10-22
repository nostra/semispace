/*
 * ============================================================================
 *
 *  File:     SemiSpaceTest.java
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
 *  Created:      Mar 5, 2008
 * ============================================================================
 */

package org.semispace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.semispace.admin.IdentifyAdminQuery;
import org.semispace.admin.SemiSpaceAdmin;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class SemiSpaceTest {
    protected static class JunitIdListener implements SemiEventListener {
        private long id;

        @Override
        public void notify(SemiEvent theEvent) {
            if (theEvent instanceof SemiAvailabilityEvent) {
                this.id = theEvent.getId();
            }
        }

        public long getId() {
            return this.id;
        }

    }

    private SemiSpace space;

    // TODO If I choose beforeall, a problem occurs with notifications. This is fishy.
    @BeforeEach
    protected void setUp() throws Exception {
        // Need to cast, as some internal methods are tested.
        space = (SemiSpace) SemiSpace.retrieveSpace();
    }

    /**
     * Notice that this test is for the benefit of terracotta - it
     * does not have any meaning if not running within terracotta.
     * (Terracotta ran this test green in 2.5.2)
     */
    @Test
    public void testThatAdminCanBeSet() {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");
        SemiLease lease = space.write(fh, 1000);
        space.setAdmin(new SemiSpaceAdmin(space, new JacksonSerializer()));
        assertTrue(lease.cancel());
        lease = space.write(fh, 1000);

        assertNotNull(space.takeIfExists(fh));
        assertFalse(lease.cancel());
    }


    @Test
    public void testIssueWithMaxLong() {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");
        SemiLease lease = space.write(fh, Long.MAX_VALUE);
        space.setAdmin(new SemiSpaceAdmin(space, new JacksonSerializer()));

        assertNull(space.takeIfExists(fh), "If this test actually returns an object, an issue has been CORRECTED. As of now " +
                "null is erroneously returned. Probably due to calculations on lease time.");
    }

    @Test
    public void testRetrievalOfHolderById() throws InterruptedException {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        assertNull(space.readHolderById(-1));
        JunitIdListener jid = new JunitIdListener();
        SemiEventRegistration reg = space.notify(fh, jid, 5000);
        space.write(fh, 2500);
        Thread.sleep(200);
        assertTrue(jid.getId() > 0);
        Holder read = space.readHolderById(jid.getId());
        assertNotNull(read);
        assertEquals(fh.getClass().getName(), read.getClassName());

        // Clean up
        space.takeIfExists(fh);
        assertTrue(reg.getLease().cancel(), "Failed to cancel notify lease");
    }

    @Test
    public void testRetrievePropertiesFromObject() {
        AlternateHolder holder = new AlternateHolder();
        holder.fieldA = "x";
        holder.fieldB = "y";
        Map<String, String> props = space.retrievePropertiesFromObject(holder);
        assertEquals(3, props.size());
        assertNotNull(props.get("class"));
        assertEquals(holder.getClass().getName(), props.get("class"));
    }

    @Test
    public void testRetrieveAdminPropertiesFromObject() {
        IdentifyAdminQuery iaq = new IdentifyAdminQuery();
        iaq.hasAnswered = Boolean.FALSE;
        Map<String, String> props = space.retrievePropertiesFromObject(iaq);
        assertEquals(3, props.size(), "Admin element has an extra (internal) entry");
        assertNotNull(props.get("class"));
        assertEquals(iaq.getClass().getName(), props.get("class"));
    }

    /**
     * None of these operations should give NPE
     */
    @Test
    public void testThatOperationsWithNullValuesAreNotFatal() {
        assertNull(space.read(null, 100));
        assertNull(space.readIfExists(null));
        assertNull(space.take(null, 100));
        assertNull(space.takeIfExists(null));
        space.notify(null, null, 100);

    }

    @Test
    public void testSimpleWrite() {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        assertNull(space.readIfExists(new FieldHolder()));

        space.harvest();
        int before = space.numberOfSpaceElements();

        space.write(fh, 91000);
        space.write(fh, 91000);
        space.harvest();
        assertEquals(before + 2, space.numberOfSpaceElements());

        assertNotNull(space.takeIfExists(fh));
        assertEquals(before + 1, space.numberOfSpaceElements());
        assertNotNull(space.takeIfExists(fh), "I put two elements in space, and both should exist.");
        assertNull(space.readIfExists(fh));
    }

    @Test
    public void testSimpleWriteOf3Elements() {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        space.harvest();
        int before = space.numberOfSpaceElements();

        space.write(fh, 91000);
        space.write(fh, 91000);
        space.write(fh, 91000);
        space.harvest();
        assertEquals(before + 3, space.numberOfSpaceElements());

        assertNotNull(space.takeIfExists(fh));
        assertNotNull(space.takeIfExists(fh));
        assertEquals(before + 1, space.numberOfSpaceElements());
        assertNotNull(space.takeIfExists(fh), "I put three elements in space, and all should exist.");
        assertNull(space.readIfExists(fh));
    }

    /**
     *
     */
    @Test
    public void testStatistics() {
        space.harvest();
        // I know the stats are defensively copied
        SemiSpaceStatistics before = space.getStatistics();
        int spaceSize = space.numberOfSpaceElements();
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");
        space.write(fh, 1000);
        assertEquals(spaceSize + 1, space.numberOfSpaceElements());
        assertNotNull(space.readIfExists(fh));
        assertNotNull(space.takeIfExists(fh));
        assertNull(space.readIfExists(fh));
        assertNull(space.takeIfExists(fh));

        SemiSpaceStatistics after = space.getStatistics();

        assertEquals(before.getRead() + 1, after.getRead());
        assertEquals(before.getMissedRead() + 1, after.getMissedRead());
        assertEquals(before.getTake() + 1, after.getTake());
        assertEquals(before.getMissedTake() + 1, after.getMissedTake());

        space.harvest();
        assertEquals(spaceSize, space.numberOfSpaceElements());
    }


    /**
     * Add notification size test when having a lease which can be canceled.
     */

    @Test
    public void testNotificationStatistics() throws InterruptedException {
        // I know the stats are defensively copied
        SemiSpaceStatistics before = space.getStatistics();

        space.harvest();
        JunitEventListener listener = new JunitEventListener();
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");
        space.notify(fh, listener, 400);
        assertEquals(before.getNumberOfListeners() + 1, space.getStatistics().getNumberOfListeners());
        space.write(fh, 300);
        Thread.sleep(100);
        assertEquals(1, listener.getCount());
        Thread.sleep(450);
        space.harvest();
        assertEquals(before.getNumberOfListeners(), space.getStatistics().getNumberOfListeners());
    }

    /**
     * The purpose of this test is to figure out what happens when you
     * have a take in the notify. All elements should get notified, even
     * when it does not exist anymore.
     *
     * @throws InterruptedException
     */
    @Test

    public void testTakeDuringNotify() throws InterruptedException {
        space.harvest();
        SemiSpaceStatistics before = space.getStatistics();
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");
        JunitEventListener listener1 = new JunitEventListener();
        JunitTakingListener listener2 = new JunitTakingListener();
        JunitEventListener listener3 = new JunitEventListener();
        //JunitTakingListener listener4 = new JunitTakingListener();
        SemiEventRegistration reg1 = space.notify(fh, listener1, 400);
        SemiEventRegistration reg2 = space.notify(fh, listener2, 400);
        SemiEventRegistration reg3 = space.notify(fh, listener3, 400);
        space.write(fh, 250);

        // Test if increase in listener is deterministic
        SemiSpaceStatistics after = space.getStatistics();
        assertEquals(before.getNumberOfListeners() + 3, after.getNumberOfListeners());
        assertTrue(reg1.getLease().cancel());
        assertTrue(reg2.getLease().cancel());
        assertTrue(reg3.getLease().cancel());

        after = space.getStatistics();
        assertEquals(before.getNumberOfListeners(), after.getNumberOfListeners(), "Number of listeners should now be on same level");

        Thread.sleep(100);
        assertNull(space.takeIfExists(fh), "Element should have been taken by listener");
        assertEquals(1, listener1.getCount());
        assertEquals(1, listener2.getCount());
        assertEquals(1, listener3.getCount());
    }

    @Test

    public void testExtensionOfListener() throws InterruptedException {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");
        JunitTakingListener listener = new JunitTakingListener();

        SemiEventRegistration reg = space.notify(fh, listener, 250);
        space.write(fh, 210);
        Thread.sleep(90);

        assertTrue(reg.getLease().renew(250), "Should be allowed to renew lease");
        Thread.sleep(140);
        // Write element once more in order to be taken once more
        space.write(fh, 210);
        Thread.sleep(50);
        assertEquals(2, listener.getCount(), "Listener should have been triggered twice.");
        assertTrue(reg.getLease().cancel());
    }

    @Test
    public void testLeaseCancel() throws InterruptedException {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        SemiLease lease = space.write(fh, 99999);
        Thread.sleep(50);
        assertNotNull(space.readIfExists(fh));
        assertTrue(lease.cancel());
        assertNull(space.readIfExists(fh));
        assertFalse(lease.cancel());
    }

    @Test
    public void testLeaseRenew() throws InterruptedException {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        SemiLease lease = space.write(fh, 200);
        Thread.sleep(100);
        assertTrue(lease.renew(500));
        Thread.sleep(120);
        assertNotNull(space.readIfExists(fh), "Element should still exist even if original lease has expired ");
        assertTrue(lease.cancel());
        assertNull(space.readIfExists(fh));
    }

    protected class JunitEventListener implements SemiEventListener {
        private int count = 0;

        @Override
        public void notify(SemiEvent theEvent) {
            if (theEvent instanceof SemiAvailabilityEvent) {
                count++;
            }
        }

        public int getCount() {
            return count;
        }
    }

    protected class JunitTakingListener implements SemiEventListener {
        private int count = 0;

        @Override
        public void notify(SemiEvent theEvent) {
            FieldHolder fh = new FieldHolder();
            fh.setFieldA("a");
            fh.setFieldB("b");
            if (space.takeIfExists(fh) != null) {
                count++;
            }
        }

        public int getCount() {
            return count;
        }
    }
}
