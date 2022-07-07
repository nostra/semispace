/*
 * ============================================================================
 *
 *  File:     TerraSpaceTest.java
 *----------------------------------------------------------------------------
 *
 * No copying allowed without explicit permission.
 *
 *  All rights reserved.
 *
 *  Description:  See javadoc below
 *
 *  Created:      24. des.. 2007
 * ============================================================================
 */

package org.semispace;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests targeted to test terracotta integration.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class TerraSpaceTest {
    private static final Logger log = LoggerFactory.getLogger(TerraSpaceTest.class);

    // Used in a test:
    private String problem = null;
    private SemiSpaceInterface space;
    private int globalCounter;

    protected SemiSpace getSpace() {
        return (SemiSpace) space;
    }

    @BeforeAll
    public void setUp() {
        space = SemiSpace.retrieveSpace();
        problem = null;
        // If running within eclipse, you will have this on your classpath
        //space = SemiSpaceProxy.retrieveSpace("http://localhost:8080/semispace-war/services/space");
    }

    @Test
    public void testWrite() {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        space.write(null, 100);
        assertNull(space.readIfExists(new FieldHolder()));
        space.write(fh, 1000);
        space.write(fh, 1000);
        assertNotNull(space.takeIfExists(fh));
        assertNotNull(space.takeIfExists(fh), "I put two elements in space, and both should exist.");
        assertNull(space.readIfExists(fh));
    }

    @Test
    public void testRead() throws InterruptedException {
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("a");
        fh.setFieldB("b");

        space.write(fh, 100);
        assertNotNull(space.takeIfExists(fh));
        Thread.sleep(110);
        assertNull(space.takeIfExists(fh));
    }

    @Test
    public void testHolderIdArray() {
        Long[] holderIds = ((SemiSpace) space).findAllHolderIds();
        assertNotNull(holderIds);
    }

    @Test
    public void testAsync() throws InterruptedException {
        Runnable r = new Runnable() {
            public void run() {
                try {
                    FieldHolder fh = new FieldHolder();
                    fh.setFieldA("a");
                    fh.setFieldB("b");

                    Thread.sleep(350);
                    space.write(fh, 2500);
                    //System.out.println("Added element.");
                } catch (InterruptedException e) {
                    // Intentional
                }
            }
        };
        new Thread(null, r).start();
        FieldHolder read = new FieldHolder();
        read.setFieldA("a");
        Thread.sleep(100);
        assertNull(space.readIfExists(read));
        assertNotNull(space.take(read, 6000));
    }

    @Test
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
        assertNotNull(space.readIfExists(template));

        template.setFieldA("xx");
        FieldHolder elem = space.takeIfExists(template);
        assertNull(elem);
        assertNotNull(space.takeIfExists(new FieldHolder()));
        assertNotNull(space.takeIfExists(new FieldHolder()));
        assertNotNull(space.takeIfExists(new FieldHolder()));
        assertNull(space.takeIfExists(new FieldHolder()));
    }

    @Test
    public void testQuantity() {
        FieldHolder templ = new FieldHolder();
        assertNull(space.takeIfExists(templ));
        templ.setFieldA("a");

        for (int i = 0; i < StressTestConstants.LARGE_QUANTITY_NUMBER; i++) {
            FieldHolder fh = new FieldHolder();
            fh.setFieldA("a");
            fh.setFieldB("b");

            space.write(fh, 29999);
        }
        for (int i = 0; i < StressTestConstants.LARGE_QUANTITY_NUMBER; i++) {
            assertNotNull(space.takeIfExists(templ), "Notice that this may be due to slow computer... Missing element at " + i);
        }
        assertNull(space.takeIfExists(templ), "Should not have any elements left");
    }

    @Test
    public void testQuantity2() {
        FieldHolder shouldBeNull = space.takeIfExists(new FieldHolder());
        assertNull(shouldBeNull, "Should not start with elements present: " + shouldBeNull);

        Runnable insert = new Runnable() {
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    FieldHolder fh = new FieldHolder();
                    fh.setFieldA("a");
                    fh.setFieldB("b");
                    space.write(fh, 5000);
                    if (i % 100 == 0) {
                        log.debug("Write statistics: " + ((SemiSpace) space).getStatistics());
                    }
                }
            }
        };
        new Thread(insert).start();
        FieldHolder templ = new FieldHolder();
        templ.setFieldA("a");
        for (int i = 0; i < 1000; i++) {
            if (i % 100 == 0) {
                log.debug("Take statistics: " + ((SemiSpace) space).getStatistics());
            }
            assertNotNull(space.take(templ, 5000), "Notice that this may be due to slow computer... Failed when tried to take element " + i + ". Statistics: " + ((SemiSpace) space).getStatistics());
        }
        assertNull(space.takeIfExists(templ), "Should not have any elements left");
    }

    /**
     * Test illustrates that a large number of items could benefit from having an index. The
     * test is horrendously slow when having a large number of elements.
     * NB: This test will fail when having a large (or large-ish) number of elements. This is
     * a deficiency that will need indexes to be improved.
     */
    @Test
    public void testWhichIllustratesSlownessWithManyElements() {
        FieldHolder shouldBeNull = space.takeIfExists(new FieldHolder());
        assertNull(shouldBeNull, "Should not start with elements present: " + shouldBeNull);
        log.debug("Statistics before start: " + ((SemiSpace) space).getStatistics());
        for (int i = 0; i < StressTestConstants.LARGE_QUANTITY_NUMBER; i++) {
            FieldHolder fh = new FieldHolder();
            fh.setFieldA("a");
            fh.setFieldB("" + i);

            space.write(fh, 30000);
        }
        log.debug("Statistics after write loop: " + ((SemiSpace) space).getStatistics());
        long stopAt = System.currentTimeMillis() + 25500;
        for (int i = 0; System.currentTimeMillis() < stopAt && i < StressTestConstants.LARGE_QUANTITY_NUMBER; i++) {
            if (i % 1000 == 0) {
                log.debug("Within read loop: " + ((SemiSpace) space).getStatistics());
            }
            FieldHolder templ = new FieldHolder();
            templ.setFieldA("a");
            templ.setFieldB("" + i);
            long takeOp = System.currentTimeMillis();
            FieldHolder x = space.takeIfExists(templ);
            assertNotNull(x, "Notice that this may be due to slow computer... Missing element at " + i +
                    ". Retake gave " + space.take(templ, 100) + " Time left: " + (System.currentTimeMillis() - stopAt) +
                    ". Last takeIfExistOp: " + (System.currentTimeMillis() - takeOp) + ". " +
                    "Statistics " + ((SemiSpace) space).getStatistics());
        }
        // Cleanup if necessary as we may have exceeded time limit
        int numberOfCleanedUpElements = 0;
        while (space.takeIfExists(new FieldHolder()) != null) {
            numberOfCleanedUpElements++;
        }
        log.debug("number of elements cleaned up " + numberOfCleanedUpElements);
    }

    @Test
    public void testAlternateHolder() {
        AlternateHolder ah = new AlternateHolder();
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

    @Test
    public void testDifferentObjects() {
        while (space.takeIfExists(new FieldHolder()) != null) {
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
        Object taken = space.take(new FieldHolder(), 100);
        assertNotNull(taken);
        taken = space.takeIfExists(new FieldHolder());
        assertNull(taken);
        assertNotNull(space.takeIfExists(new AlternateHolder()));
        assertNull(space.takeIfExists(new AlternateHolder()));
    }

    @Test
    public void testAsyncWithFourThreads() throws InterruptedException {
        final int numberToTest = 5000;
        Runnable write = new Runnable() {
            public void run() {
                FieldHolder fh = new FieldHolder();
                fh.setFieldA("a");
                fh.setFieldB("b");
                for (int i = 0; i < numberToTest; i++) {
                    space.write(fh, 19500);
                }
            }
        };
        Runnable write2 = new Runnable() {
            public void run() {
                AlternateHolder ah = new AlternateHolder();
                ah.fieldA = "a";
                ah.fieldB = "b";
                for (int i = 0; i < numberToTest; i++) {
                    space.write(ah, 19500);
                }
            }
        };
        Runnable read = new Runnable() {
            public void run() {
                FieldHolder fh = new FieldHolder();
                fh.setFieldA("a");
                fh.setFieldB("b");

                for (int i = 0; i < numberToTest; i++) {
                    if (space.take(fh, 19500) == null && problem == null) {
                        problem = "Got null when taking element " + i;
                    }
                }
            }
        };
        Runnable read2 = new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
                AlternateHolder ah = new AlternateHolder();
                ah.fieldA = "a";
                ah.fieldB = "b";

                for (int i = 0; i < numberToTest; i++) {
                    if (space.take(ah, 19500) == null && problem == null) {
                        problem = "Got null when taking element " + i;
                    }
                }
            }
        };
        Thread a = new Thread(null, write);
        Thread b = new Thread(null, read);
        Thread c = new Thread(null, write2);
        Thread d = new Thread(null, read2);

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
        assertNull(space.readIfExists(fx), "Should have consumed all of the elements. Still have some.");
        AlternateHolder ah = new AlternateHolder();
        ah.fieldA = "a";
        assertNull(space.readIfExists(ah), "Should have consumed all of the elements. Still have some alternate versions.");
        assertNull(problem, problem);
    }

    @Test
    public void testAsyncWithFourThreadsAndId() throws InterruptedException {
        globalCounter = 2;
        final int numberOfIterations = StressTestConstants.LARGE_QUANTITY_NUMBER;
        Runnable write = new Runnable() {
            public void run() {
                FieldHolder fh = new FieldHolder();
                fh.setFieldA("a");
                for (int i = 0; i < numberOfIterations; i++) {
                    fh.setFieldB("b" + i);
                    space.write(fh, SemiSpace.ONE_DAY);
                }
                log.debug("Writer thread 1 finished");
                globalCounter--;
            }
        };
        Runnable write2 = new Runnable() {
            public void run() {
                FieldHolder fh = new FieldHolder();
                fh.setFieldB("b");
                for (int i = 0; i < numberOfIterations; i++) {
                    fh.setFieldA("a" + i);
                    space.write(fh, SemiSpace.ONE_DAY);
                }
                log.debug("Writer thread 2 finished");
                globalCounter--;
            }
        };
        Runnable read = new Runnable() {
            public void run() {
                FieldHolder fh = new FieldHolder();
                fh.setFieldA("a");

                for (int i = 0; i < numberOfIterations; i++) {
                    fh.setFieldB("b" + i);
                    if (problem == null) {
                        FieldHolder r = space.take(fh, 10 + (globalCounter * 9500));
                        if (r == null) {
                            r = space.takeIfExists(fh);
                            problem = "Got null when taking element b" + i + ". Result when trying to re-take: " + r;
                        } else if (!r.getFieldA().equals(fh.getFieldA()) ||
                                !r.getFieldB().equals(fh.getFieldB())) {
                            problem = "Rather disturbing. When querying for " + fh + " the result was: " + r;
                        }
                    }
                }
            }
        };
        Runnable read2 = new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
                FieldHolder fh = new FieldHolder();
                fh.setFieldB("b");
                for (int i = 0; i < numberOfIterations; i++) {
                    fh.setFieldA("a" + i);
                    if (problem == null) {
                        FieldHolder r = space.take(fh, 10 + (globalCounter * 9500));
                        if (r == null) {
                            r = space.takeIfExists(fh);
                            problem = "Got null when taking element a" + i + ". Result when trying to re-take: " + r;
                        } else if (!r.getFieldA().equals(fh.getFieldA()) ||
                                !r.getFieldB().equals(fh.getFieldB())) {
                            problem = "Rather disturbing. When querying for " + fh + " the result was: " + r;
                        }
                    }
                }
            }
        };
        Thread a = new Thread(null, write);
        Thread b = new Thread(null, read);
        Thread c = new Thread(null, write2);
        Thread d = new Thread(null, read2);

        a.start();
        b.start();
        c.start();
        d.start();
        a.join();
        b.join();
        c.join();
        d.join();
        assertNull( problem, "Number of writers not finished: " + globalCounter + ". " + problem);
        FieldHolder fx = new FieldHolder();
        fx.setFieldA("a");
        fx = space.readIfExists(fx);
        assertNull(fx, "Should have consumed all of the elements. Still have some: " + fx);
    }


    /**
     * Will not fail on insertion time, really. Just testing how many
     * elements that can be inserted and removed. Note that I
     * <b>do</b> test whether the same number was removed as inserted.
     */
    @Test
    public void testInsertionTime() {
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        nvq.value = "insertion time";

        log.debug("Started insertion");
        int counter = 0;
        long startTime = System.currentTimeMillis();
        while (startTime > System.currentTimeMillis() - 1000) {
            space.write(nvq, 20000);
            counter++;
        }
        log.debug("Inserted " + counter + " elements");
        int takenCounter = 0;
        while (space.take(nvq, 200) != null) {
            takenCounter++;
        }
        log.debug("Total running time " + (System.currentTimeMillis() - startTime) + " ms, inserted (and hopefully took) " + counter + " items.");

        assertEquals(counter, takenCounter, "Should be able to take as many elements as was inserted.");
    }

}
