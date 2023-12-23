/*
 * ============================================================================
 *
 *  File:     HolderContainerTest.java
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
 *  Created:      Apr 27, 2008
 * ============================================================================
 */

package org.semispace;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class HolderContainerTest {
    private static final Logger log = LoggerFactory.getLogger(HolderContainerTest.class);
    private long id = 0;

    @Test
    public void testAlmostEmptyHolderContainer() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        assertEquals(0, hc.size(), "This test requires that the other tests cleaned up after themselves. Types: " + Arrays.asList(hc.retrieveGroupNames()));
        Holder a = createHolder();
        final int orgsize = hc.size();
        hc.addHolder(a);
        assertEquals(orgsize + 1, hc.size());

        assertNull(hc.findById(id + 100, a.getClassName()));
        assertNotNull(hc.findById(a.getId(), a.getClassName()));

        assertNotNull(hc.removeHolderById(a.getId(), a.getClassName()));
        assertEquals(orgsize, hc.size());
        assertNull(hc.removeHolderById(id + 100, a.getClassName()));
    }

    @Test
    public void testHolderContainerWith2Elements() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        assertEquals(0, hc.size(), "This test requires that the other tests cleaned up after themselves.");
        Holder a = createHolder();
        Holder b = createHolder();
        final int orgsize = hc.size();

        hc.addHolder(a);
        hc.addHolder(b);
        assertEquals(orgsize + 2, hc.size());

        assertNotNull(hc.findById(a.getId(), a.getClassName()));
        assertNotNull(hc.findById(b.getId(), b.getClassName()));

        assertTrue(a.getId() != b.getId());

        assertTrue(hc.next(a.getClassName()) != null);

        assertNotNull(hc.removeHolderById(a.getId(), a.getClassName()));
        assertEquals(orgsize + 1, hc.size());
        assertTrue(hc.next(b.getClassName()) != null);
        assertEquals(b.getId(), hc.readHolderWithId(b.getId()).getId());
        assertNotNull(hc.removeHolderById(b.getId(), b.getClassName()));
        HolderElement next = hc.next(b.getClassName());
        if (next != null) {
            assertEquals(0, next.size(), "Expecting holder element to be empty, if it exists.");
        }
        assertEquals(orgsize, hc.size());

        assertNull(hc.findById(a.getId(), b.getClassName()));
        assertNull(hc.findById(b.getId(), b.getClassName()));

    }


    @Test
    public void testHolderContainer() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        Holder a = createHolder();
        Holder b = createHolder();
        Holder c = createHolder();
        Holder d = createHolder();
        final int orgsize = hc.size();

        hc.addHolder(a);
        hc.addHolder(b);
        hc.addHolder(c);
        hc.addHolder(d);
        assertEquals(orgsize + 4, hc.size());

        assertNull(hc.findById(id + 100, a.getClassName()));
        assertNotNull(hc.findById(a.getId(), a.getClassName()));
        assertNotNull(hc.findById(b.getId(), a.getClassName()));
        assertNotNull(hc.findById(c.getId(), a.getClassName()));
        assertNotNull(hc.findById(d.getId(), a.getClassName()));

        assertNotNull(hc.removeHolderById(b.getId(), a.getClassName()));
        assertEquals(orgsize + 3, hc.size());

        assertNotNull(hc.findById(a.getId(), a.getClassName()));
        assertNull(hc.findById(b.getId(), a.getClassName()));
        assertNotNull(hc.findById(c.getId(), a.getClassName()));
        assertNotNull(hc.findById(d.getId(), a.getClassName()));

        assertNotNull(hc.removeHolderById(d.getId(), a.getClassName()));

        assertEquals(orgsize + 2, hc.size());
        assertNotNull(hc.findById(a.getId(), a.getClassName()));
        assertNull(hc.findById(b.getId(), a.getClassName()));
        assertNotNull(hc.findById(c.getId(), a.getClassName()));
        assertNull(hc.findById(d.getId(), a.getClassName()));

        assertNull(hc.removeHolderById(d.getId(), a.getClassName()));

        assertNotNull(hc.removeHolderById(a.getId(), a.getClassName()));
        assertNotNull(hc.removeHolderById(c.getId(), a.getClassName()));

        assertEquals(orgsize, hc.size());

    }

    private Holder createHolder() {
        Holder holder = new Holder("<x>dummy-" + id + "</x>", System.currentTimeMillis() + (60 * 60 * 1000), "junit", id, null);
        id++;
        return holder;
    }

    @Test
    public void testIdentityOfRemovedElement() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        assertEquals(0, hc.size(), "This test requires that the other tests cleaned up after themselves.");
        Holder a = createHolder();
        Holder b = createHolder();
        Holder c = createHolder();
        Holder d = createHolder();
        final int orgsize = hc.size();

        hc.addHolder(a);
        hc.addHolder(b);
        hc.addHolder(c);
        hc.addHolder(d);
        assertEquals(orgsize + 4, hc.size());

        Holder x = hc.removeHolderById(b.getId(), b.getClassName());
        assertEquals(b.getId(), x.getId());
        Holder y = hc.removeHolderById(a.getId(), a.getClassName());
        assertEquals(a.getId(), y.getId());
        Holder z = hc.removeHolderById(d.getId(), d.getClassName());
        assertEquals(d.getId(), z.getId());

        assertEquals(orgsize + 1, hc.size());
        Holder h = hc.removeHolderById(c.getId(), c.getClassName());
        assertEquals(c.getId(), h.getId());
        assertEquals(orgsize, hc.size());
    }

    @Test
    public void testHavingFewElements() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        assertEquals(0, hc.size(), "This test requires that the other tests cleaned up after themselves.");
        final int orgsize = hc.size(); // Using variable as it is cleaner
        Holder a = createHolder();
        Holder b = createHolder();
        hc.addHolder(a);
        hc.addHolder(b);
        assertEquals(orgsize + 2, hc.size());
        Holder aRemoved = hc.removeHolderById(a.getId(), a.getClassName());
        assertEquals(a.getId(), aRemoved.getId());
        assertEquals(orgsize + 1, hc.size());

        // Re-add a
        hc.addHolder(a);
        assertEquals(orgsize + 2, hc.size());
        // ... and remove it again
        aRemoved = hc.removeHolderById(a.getId(), a.getClassName());
        assertEquals(a.getId(), aRemoved.getId());

        Holder bRemoved = hc.removeHolderById(b.getId(), b.getClassName());
        assertEquals(b.getId(), bRemoved.getId());
        assertEquals(orgsize, hc.size());
    }

    /**
     * Not expected to fail on insertion time, really. Just testing how many
     * elements that can be inserted and removed. Note that I
     * <b>do</b> test whether the same number was removed as inserted.
     */
    @Test
    public void testInsertionRate() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        final int orgsize = hc.size();
        final long startingId = id;

        log.debug("Started insertion");
        int counter = 0;
        long startTime = System.currentTimeMillis();

        while (startTime > System.currentTimeMillis() - 1000) {
            hc.addHolder(createHolder());
            counter++;
        }

        log.debug("Inserted " + counter + " elements");
        assertEquals(counter, hc.size());

        log.debug("Last element " + id);

        int takenCounter = 0;
        for (long i = startingId; i < id; i++) {
            assertNotNull(hc.removeHolderById(i, "junit"), "Expecting to be able to remove element " + i);
            takenCounter++;
        }
        log.debug("Total running time " + (System.currentTimeMillis() - startTime) + " ms, inserted (and hopefully took) " + counter + " items.");

        assertEquals(counter, takenCounter, "Should be able to take as many elements as was inserted.");
        assertEquals(orgsize, hc.size());
    }

}
