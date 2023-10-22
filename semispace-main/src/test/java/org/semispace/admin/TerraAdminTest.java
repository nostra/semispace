package org.semispace.admin;

import org.junit.jupiter.api.Test;
import org.semispace.JacksonSerializer;
import org.semispace.SemiSpaceInterface;

import static org.junit.jupiter.api.Assertions.*;

public class TerraAdminTest {

    @Test
    public void testPerformInitializationAsMaster() {
        SemiSpaceInterface ts = new JunitSpace();
        SemiSpaceAdmin admin = new SemiSpaceAdmin(ts, new JacksonSerializer());
        assertNotNull(admin);
        admin.performInitialization();
        // Double initialization does not give error(s)
        admin.performInitialization();
        assertTrue(admin.isMaster());
        assertEquals(1, admin.getSpaceId());
    }

    @Test
    public void testIncreaseOfSpaceId() {
        SemiSpaceInterface ts = new JunitSpace();
        IdentifyAdminQuery iaq = new IdentifyAdminQuery();
        iaq.amIAdmin = Boolean.TRUE;
        iaq.id = new Integer(1);
        iaq.hasAnswered = Boolean.TRUE;
        ts.write(iaq, 1000);

        TimeQuery tq = new TimeQuery();
        tq.isFinished = Boolean.TRUE;

        SemiSpaceAdmin admin = new SemiSpaceAdmin(ts, new JacksonSerializer());
        admin.performInitialization();
        assertFalse(admin.isMaster());

        assertEquals(iaq.id.intValue() + 1, admin.getSpaceId(), "Space id shall typically increase with one.");
    }
}
