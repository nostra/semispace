/*
 * Copyright 2010 Erlend Nossum
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
 */

package org.semispace.comet.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semispace.NameValueQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


public class ReadClientTest {
    private static final Logger log = LoggerFactory.getLogger(ReadClientTest.class);
    private SemiSpaceCometProxy space;

    @Before
    public void setUp() throws Exception {
        log.warn("\n\n\n\nNOT SUPPORTING NORMAL BUILD TESTS YET\nUse\n  mvn -Denv=dev clean install\nwhen building this module\n\n\n");
        space = new SemiSpaceCometProxy();
        space.init("http://localhost:8080/semispace-comet-server/cometd/");
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(500);
        space.destroy();
    }

    @Test
    public void testRead() throws Exception {
        space.readIfExists(new NameValueQuery());//
        NameValueQuery nvq = space.take(new NameValueQuery(), 500);
        assertNull("Expecting not to be able to take something before something is present.", nvq);

        NameValueQuery q = new NameValueQuery();
        q.name = "somename";
        q.value = "some value";
        space.write(q, 2000);
        //for ( int i=0 ; i < 1000 ; i++ ) space.read(new NameValueQuery(), 1000);

        nvq = space.read(new NameValueQuery(), 2000);
        assertNotNull("Expecting to find value", nvq);
        assertEquals(q.name , nvq.name);
        nvq = space.takeIfExists(new NameValueQuery());
        assertNotNull("Expecting to take", nvq);
        assertEquals(q.name, nvq.name);
        
        assertNull("Expecting not to be able to take something twice", space.take(new NameValueQuery(), 2000));
    }

    @Test
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

    @Test
    public void testTimeout() {
        FieldHolder entry = new FieldHolder();
        entry.setFieldA("c");
        entry.setFieldB("d");
        FieldHolder templ = new FieldHolder();
        templ.setFieldA(entry.getFieldA());
        templ.setFieldB(entry.getFieldB());

        space.write(entry, 1000);
        assertNotNull(space.read(templ, 30));

        try {
            Thread.sleep(1005);
        } catch (InterruptedException ignored) {
            // Ignore
        }
        assertNull("Space must honor timeout", space.readIfExists(entry));
    }

    @Test
    public void testDoNotQueryWithIdentity() throws InterruptedException {
        FieldHolder entry = new FieldHolder();
        entry.setFieldA("c");
        entry.setFieldB("d");

        space.write(entry, 250);
        Thread.sleep(50);
        assertNotNull(space.readIfExists(entry));
        assertNotNull(space.takeIfExists(entry));
    }

    @Test
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

    @Test
    public void testAlmostEqualHolders() {
        AlternateHolder holder = new AlternateHolder();
        holder.fieldA = "a";
        holder.fieldB = "b";
        AlternateButEqual different = new AlternateButEqual();
        different.fieldA = "a";
        different.fieldB = "b";

        AlternateHolder query = new AlternateHolder();
        query.fieldA = "a";
        assertNull("Expecting null",space.takeIfExists(query));

        space.write(holder, 1000);
        space.write(different, 1000);


        assertEquals("" + holder, "" + space.takeIfExists(query));
        assertEquals("null", "" + space.takeIfExists(query));
        assertEquals("" + different, "" + space.takeIfExists(different));
    }

}
