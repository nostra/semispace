package org.semispace.admin;

import junit.framework.TestCase;
import org.semispace.SemiSpaceInterface;

public class TerraAdminTest extends TestCase {

    public void testPerformInitializationAsMaster() {
        SemiSpaceInterface ts = new JunitSpace();
        SemiSpaceAdmin admin = new SemiSpaceAdmin(ts);
        assertNotNull(admin);
        admin.performInitialization();
        // Double initialization does not give error(s)
        admin.performInitialization();
        assertTrue(admin.isMaster());
        assertEquals(1, admin.getSpaceId());
    }

    public void testIncreaseOfSpaceId() {
        SemiSpaceInterface ts = new JunitSpace();
        IdentifyAdminQuery iaq = new IdentifyAdminQuery();
        iaq.amIAdmin = Boolean.TRUE;
        iaq.id = new Integer(1);
        iaq.hasAnswered = Boolean.TRUE;
        ts.write(iaq, 1000);
        
        TimeQuery tq = new TimeQuery();
        tq.isFinished = Boolean.TRUE;
        
        SemiSpaceAdmin admin = new SemiSpaceAdmin(ts);
        admin.performInitialization();
        assertFalse(admin.isMaster());
     
        assertEquals("Space id shall typically increase with one.", iaq.id.intValue() +1, admin.getSpaceId());
    }
}
