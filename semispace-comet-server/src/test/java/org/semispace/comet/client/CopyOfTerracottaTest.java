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

import junit.framework.TestCase;
import org.semispace.AlternateHolder;
import org.semispace.FieldHolder;
import org.semispace.NameValueQuery;
import org.semispace.TerraSpaceTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copy of terracotta test set, geared towards comet-space. Notice that the number of
 * elements that are inserted are typically quite fewer than in the terracotta test.
 */
public class CopyOfTerracottaTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(TerraSpaceTest.class);

    // Used in a test:
    private String problem=null;
    private SemiSpaceCometProxy space;

    @Override
    public void setUp() {
        log.warn("\n\n\n\nNOT SUPPORTING NORMAL BUILD TESTS YET\nUse\n  mvn -Denv=dev clean install\nwhen building this module\n\n\n");
        space = new SemiSpaceCometProxy();
        space.init("http://localhost:8080/semispace-comet-server/cometd/");
        problem = null;
        // If running within eclipse, you will have this on your classpath
        //space = SemiSpaceProxy.retrieveSpace("http://localhost:8080/semispace-war/services/space");
    }

    public void tearDown() throws InterruptedException {
        Thread.sleep(500);
        space.destroy();
    }

    public void testWrite() {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        space.write( null, 100 );
        assertNull( space.readIfExists(new FieldHolder()));
        space.write(fh,1000);
        space.write(fh,1000);
        assertNotNull(space.takeIfExists(fh));
        assertNotNull("I put two elements in space, and both should exist.", space.takeIfExists(fh));
        assertNull(space.readIfExists(fh));
    }

    public void testRead() throws InterruptedException {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        space.write(fh,100);
        assertNotNull(space.takeIfExists(fh));
        Thread.sleep(110);
        assertNull(space.takeIfExists(fh));
    }

    public void testFunctionalityBeforeAsync() throws InterruptedException {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");
        Thread.sleep(350);
        space.write(fh, 2500);
        FieldHolder read = new FieldHolder();
        read.setFieldA("a");
        assertNotNull("Should be able to read element.", space.read(read, 500));
        assertNotNull("Element to read should exist", space.take(read, 500));
        assertNull("After field holder have been taken, it should no longer reside in space.", space.readIfExists(read));
    }

    public void testAsync() throws InterruptedException {
        Runnable r = new Runnable() {
            public void run() {
                try {
                    FieldHolder fh = new FieldHolder();
                    fh.setFieldA("a");
                    fh.setFieldB("b");
                    log.debug("Writer thread is sleeping");
                    Thread.sleep(350);
                    log.debug(">>>>>>>>>> Writing object");
                    space.write(fh, 7500);
                    log.debug("Object is written");
                } catch (InterruptedException e) {
                    // Intentional
                }
            }
        };
        new Thread( null, r ).start();
        FieldHolder read = new FieldHolder();
        read.setFieldA("a");
        Thread.sleep(100);
        assertNull(space.readIfExists(read));
        log.debug("After read if exists (which should return null)");
        assertNotNull("Element to read should exist in space after 9 seconds", space.take(read, 9000));
    }

    public void testPreciseRead() throws InterruptedException {
        FieldHolder fh1 = new FieldHolder();
        FieldHolder fh2 = new FieldHolder();
        fh1.setFieldA("1-a");
        fh2.setFieldA("2-a");
        fh1.setFieldB("1-b");
        fh2.setFieldB("2-b");
        space.write(fh1, 9999);
        space.write(fh2, 9999);
        // Just empty
        space.write(new FieldHolder(), 9999);

        FieldHolder template = new FieldHolder();
        template.setFieldA(fh2.getFieldA());
        Thread.sleep(250);
        assertNotNull("Existing element should be found", space.readIfExists(template));

        template.setFieldA("xx");
        FieldHolder elem = (FieldHolder) space.takeIfExists(template);
        assertNull("Non-existing element should not be found, but when querying with "+template+" I got: "+elem, elem);
        assertNotNull(space.takeIfExists(new FieldHolder()));
        assertNotNull(space.takeIfExists(new FieldHolder()));
    }

    public void testQuantity() {
        FieldHolder templ = new FieldHolder();
        templ.setFieldA("a");
        assertNull("Should start with elements present", space.takeIfExists(templ));

        for ( int i=0 ; i < 100 ; i++ ) {
            FieldHolder fh = new FieldHolder();
            fh.setFieldA("a");
            fh.setFieldB("b");

            space.write(fh, 29999);
        }
        for ( int i=0 ; i < 100 ; i++ ) {
            assertNotNull("Notice that this may be due to slow computer... Missing element at "+i, space.takeIfExists(templ));
        }
        assertNull("Should not have any elements left", space.takeIfExists(templ));
    }

    public void testQuantity2() {

        Runnable insert = new Runnable( ) {
            public void run() {
                for ( int i=0 ; i < 1000 ; i++ ) {
                    FieldHolder fh = new FieldHolder();
                    fh.setFieldA("a");
                    fh.setFieldB("b");
                    space.write(fh, 5000);
/*
                    if ( i % 100 == 0 ) {
                        log.debug("Write statistics: "+((SemiSpace)space).getStatistics());
                    }
*/
                }
            }
        };
        new Thread(insert).start();
        FieldHolder templ = new FieldHolder();
        templ.setFieldA("a");
        for ( int i=0 ; i < 1000 ; i++ ) {
/*            if ( i % 100 == 0 ) {
                log.debug("Take statistics: "+((SemiSpace)space).getStatistics());
            }*/
            assertNotNull("Notice that this may be due to slow computer... Failed when tried to take element "+i+".", space.take(templ,5000));
        }
        assertNull("Should not have any elements left", space.takeIfExists(templ));
    }

    public void testAlternateHolder() {
        org.semispace.AlternateHolder ah = new AlternateHolder();
        ah.fieldA = "a";
        ah.fieldB = "b";
        space.write(ah, 1000);
        assertNotNull(space.readIfExists(new AlternateHolder()));
        AlternateHolder non = new AlternateHolder();
        non.fieldA = "x";
        assertNull(space.readIfExists(non));
        non.fieldA = "a";
        assertNotNull(space.readIfExists(non));
        assertNotNull(space.takeIfExists(non));
    }

    public void testDifferentObjects() {
        while ( space.takeIfExists(new FieldHolder()) != null ) {
            // Intentional
        }
        AlternateHolder ah = new AlternateHolder();
        ah.fieldA = "a";
        ah.fieldB = "b";
        space.write(ah, 1000);
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");
        space.write(fh, 1000);
        Object taken = space.take(new FieldHolder(),100);
        assertNotNull(taken);
        taken = space.takeIfExists(new FieldHolder());
        assertNull("Should not exist twice: "+taken, taken);
        assertNotNull(space.takeIfExists(new AlternateHolder()));
        assertNull(space.takeIfExists(new AlternateHolder()));
    }


    public void testAsyncWithFourThreads() throws InterruptedException {
        Runnable write = new Runnable() {
            public void run() {
                    FieldHolder fh = new FieldHolder();
                    fh.setFieldA("a");
                    fh.setFieldB("b");
                    for ( int i=0 ; i < 500 ; i++ ) {
                        space.write(fh, 19500);
                    }
            }
        };
        Runnable write2 = new Runnable() {
            public void run() {
                AlternateHolder ah = new AlternateHolder();
                ah.fieldA = "a";
                ah.fieldB = "b";
                for ( int i=0 ; i < 500 ; i++ ) {
                    space.write(ah, 19500);
                }
            }
        };
        Runnable read = new Runnable() {
            public void run() {
                FieldHolder fh = new FieldHolder();
                fh.setFieldA("a");
                fh.setFieldB("b");

                for ( int i=0 ; i < 500 ; i++ ) {
//                    increaseSimultanous();
                    if (space.take(fh, 19500) == null && problem == null ) {
                        problem = "Got null when taking element "+i;
                    }
//                    decreaseSimultanous();
                }
            }
        };
        Runnable read2 = new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
                AlternateHolder ah = new AlternateHolder();
                ah.fieldA = "a";
                ah.fieldB = "b";

                for ( int i=0 ; i < 500 ; i++ ) {
//                    increaseSimultanous();
                    if (space.take(ah, 19500) == null && problem == null ) {
                        problem = "Got null when taking element "+i;
                    }
//                    decreaseSimultanous();
                }
            }
        };
        Thread a = new Thread( null, write );
        Thread b = new Thread( null, read );
        Thread c = new Thread( null, write2 );
        Thread d = new Thread( null, read2 );

        a.start();
        b.start();
        c.start();
        d.start();
        a.join();
        b.join();
        c.join();
        d.join();
        FieldHolder fx = new FieldHolder();
        fx.setFieldA("a");
        assertNull("Should have consumed all of the elements. Still have some.", space.readIfExists(fx));
        AlternateHolder ah = new AlternateHolder();
        ah.fieldA = "a";
        assertNull("Should have consumed all of the elements. Still have some alternate versions. Logged problem: "+problem, space.readIfExists(ah));
        assertNull(problem, problem );
    }

    /*
    private synchronized void increaseSimultanous() {
        int count = 0;
        while ( simultaneous > 100 ) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // Ignored
            }
            if ( ++count > 10 ) {
                throw new RuntimeException("Problematic count - does not decrease");
            }
        }
        simultaneous++;
    }

    private synchronized void decreaseSimultanous()  {
        simultaneous--;
    }
    */

    /**
     * Will not fail on insertion time, really. Just testing how many
     * elements that can be inserted and removed. Note that I
     * <b>do</b> test whether the same number was removed as inserted.
     */
    public void testInsertionTime() {
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        nvq.value = "insertion time";

        log.debug("Started insertion");
        int counter = 0;
        long startTime = System.currentTimeMillis();
        while ( startTime > System.currentTimeMillis() - 500 ) {
            space.write(nvq, 10000);
            counter++;
        }
        log.debug("Inserted "+counter+" elements");
        int takenCounter = 0;
        while ( space.take( nvq, 200 ) != null ) {
            takenCounter++;
        }
        log.debug("Total running time "+(System.currentTimeMillis() - startTime));

        assertEquals("Should be able to take as many elements as was inserted.", counter, takenCounter);
    }

}

