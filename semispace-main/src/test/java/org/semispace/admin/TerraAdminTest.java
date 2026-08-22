/*
 * Copyright © 2026 Erlend Nossum
 *
 * This file is part of the semispace project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Description:  See javadoc below
 */

package org.semispace.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.semispace.JacksonSerializer;
import org.semispace.SemiSpaceInterface;

public class TerraAdminTest {

    @Test
    public void testPerformInitializationAsMaster() {
        SemiSpaceInterface ts = new JunitSpace();
        SemiSpaceAdmin admin = new SemiSpaceAdmin(ts, JacksonSerializer.jacksonSerializerFactory(false));
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
        iaq.id = Integer.valueOf(1);
        iaq.hasAnswered = Boolean.TRUE;
        ts.write(iaq, 1000);

        TimeQuery tq = new TimeQuery();
        tq.isFinished = Boolean.TRUE;

        SemiSpaceAdmin admin = new SemiSpaceAdmin(ts, JacksonSerializer.jacksonSerializerFactory(false));
        admin.performInitialization();
        assertFalse(admin.isMaster());

        assertEquals(iaq.id.intValue() + 1, admin.getSpaceId(), "Space id shall typically increase with one.");
    }
}
