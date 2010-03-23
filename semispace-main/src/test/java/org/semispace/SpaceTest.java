/*
 * ============================================================================
 *
 *  File:     SpaceTest.java
 *----------------------------------------------------------------------------
 *
 * No copying allowed without explicit permission.
 *
 *  All rights reserved.
 *
 *  Description:  See javadoc below
 *
 *  Created:      25. des.. 2007
 * ============================================================================ 
 */

package org.semispace;

import junit.framework.TestCase;
import org.semispace.admin.IdentifyAdminQuery;
import org.semispace.admin.SemiSpaceAdminInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;

public class SpaceTest extends TestCase {
    private SemiSpaceInterface space;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        space = SemiSpace.retrieveSpace();
        // If running within eclipse, you will have this on your classpath
        // space = SemiSpaceProxy.retrieveSpace("http://localhost:8080/semispace-war/services/space");
    }

    public void testPresenceOfAdmin() {
        if (space instanceof SemiSpace) {
            // Casting, etc, is just because this test sometimes is used for testing proxy
            SemiSpace semi = (SemiSpace) space;
            SemiSpaceAdminInterface admin = semi.getAdmin();
            assertNotNull(admin);
        }
    }

    public void testWrite() {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        // Lease lease = space.write(entry,100000);
        space.write(fh, 100000);

        FieldHolder search = new FieldHolder();
        search.setFieldA("a");
        assertNotNull("Expecting to be able to find element searched for. Using \n" + search + " \nto search for \n"
                + fh, space.readIfExists(search));
        assertNotNull(space.read(fh, 0));
        assertEquals("Identity", fh, fh);
        assertEquals(fh, space.take(fh, 0));
        assertNull(space.readIfExists(fh));
        assertNull(space.takeIfExists(fh));
    }

    public void testTimeout() {
        FieldHolder entry = new FieldHolder();
        entry.setFieldA("c");
        entry.setFieldB("d");
        FieldHolder templ = new FieldHolder();
        templ.setFieldA(entry.getFieldA());
        templ.setFieldB(entry.getFieldB());

        space.write(entry, 100);
        assertNotNull(space.read(templ, 30));

        try {
            Thread.sleep(105);
        } catch (InterruptedException ignored) {
            // Ignore
        }
        assertNull("Space must honor timeout", space.readIfExists(entry));
    }

    public void testDoNotQueryWithIdentity() throws InterruptedException {
        FieldHolder entry = new FieldHolder();
        entry.setFieldA("c");
        entry.setFieldB("d");

        space.write(entry, 250);
        Thread.sleep(50);
        assertNotNull(space.readIfExists(entry));
        assertNotNull(space.takeIfExists(entry));
    }

    public void testReadTimeout() {
        FieldHolder entry = new FieldHolder();
        entry.setFieldA("e");
        entry.setFieldB("f");

        long time = System.currentTimeMillis() + 500;
        FieldHolder read = space.read(entry, 501);
        assertNull("Expected null, got " + read, read);
        long systime = System.currentTimeMillis();
        assertTrue("Read should block for the indicated time. It did not. Got systime " + systime
                + " which should (but is not) greater than estimated time " + time, time < systime);
    }

    public void testAdminQueryObject() throws InterruptedException {
        IdentifyAdminQuery iaq = new IdentifyAdminQuery();
        iaq.hasAnswered = Boolean.FALSE;
        assertNull( space.takeIfExists(iaq));
        
        space.write(iaq, SemiSpace.ONE_DAY);
        Thread.sleep(1000);
        assertNotNull( "Should not be eaten by space.", space.takeIfExists(iaq));
        iaq.hasAnswered = Boolean.TRUE;
        
        assertNotNull( "Answer should have been put into space", space.takeIfExists(iaq));
    }
    
    public void testAnswerOfAdminQuery() {
        IdentifyAdminQuery iaq = new IdentifyAdminQuery();
        iaq.hasAnswered = Boolean.FALSE;
        space.write(iaq, SemiSpace.ONE_DAY);

        IdentifyAdminQuery want = new IdentifyAdminQuery();
        want.hasAnswered = Boolean.TRUE;

        IdentifyAdminQuery answer = space.take(want, 6500);
        assertNotNull("Admin element makes sure that identity queries are answered.", answer);
        assertNotNull( "Need to remove query for admin element", space.takeIfExists(iaq));
    }

    public void testSameButDifferentObject() {
        AlternateHolder onlyA = new AlternateHolder();
        onlyA.fieldA = "a";
        AlternateHolder onlyB = new AlternateHolder();
        onlyA.fieldB = "b";

        AlternateHolder both = new AlternateHolder();
        both.fieldA = "a";
        both.fieldB = "b";

        space.write(onlyA, 1000);
        space.write(onlyB, 1000);
        space.write(both, 1000);

        AlternateHolder query = new AlternateHolder();
        query.fieldA = "a";

        assertEquals("Due to the present nature of the holder structure, queries are LIFO. This test may fail " +
                "if this changes, and then the test would need to be corrected.", "" + both, "" + space.takeIfExists(query));
        assertEquals("" + onlyA, "" + space.takeIfExists(query));
        assertEquals("null", "" + space.takeIfExists(query));
        assertEquals("" + onlyB, "" + space.takeIfExists(onlyB));

    }

    public void testAlmostEqualHolders() {
        AlternateHolder holder = new AlternateHolder();
        holder.fieldA = "a";
        holder.fieldB = "b";
        AlternateButEqual different = new AlternateButEqual();
        different.fieldA = "a";
        different.fieldB = "b";

        space.write(holder, 1000);
        space.write(different, 1000);

        AlternateHolder query = new AlternateHolder();
        query.fieldA = "a";

        assertEquals("" + holder, "" + space.takeIfExists(query));
        assertEquals("null", "" + space.takeIfExists(query));
        assertEquals("" + different, "" + space.takeIfExists(different));

    }

    public void testNotification() throws InterruptedException {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("notify");
        NotificationTestListener listener = new NotificationTestListener();
        SemiEventRegistration reg = space.notify(null, listener, 1000);
        assertNull(reg);
        reg = space.notify(fh, null, 1000);
        assertNull(reg);
        reg = space.notify(fh, listener, 1000);
        assertNotNull(reg);
        assertFalse(listener.notified);
        space.write(fh, 1000);
        // Cannot check notified here, unless first sleeping 
        assertNotNull("Taking the object first, as this also will sleep for the desired amount of time. The object should exist.", space.take(fh, 1000));
        Thread.sleep(250);
        assertTrue("The listener shall have been notified sometime during the write process.", listener.notified);
    }

    protected static class NotificationTestListener implements SemiEventListener {
        protected boolean notified = false;

        public void notify(SemiEvent theEvent) {
            if ( theEvent instanceof SemiAvailabilityEvent ) {
                // Testing only that elements are added
                notified = true;
            }
        }

    }


}
