package org.semispace;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Starvation test. Thanks to <b>Chris Mcfarlen</b> of Yahoo for providing the
 * scenario and the initial test code.
 *
 * <p>
 * This test will fail with a large iteration number for reasons not pinpointed at the
 * time of writing.
 * </p>
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ListenerStarvationTest {
    private static final Logger log = LoggerFactory.getLogger(ListenerStarvationTest.class);

    private SemiSpaceInterface space;
    private boolean hasError;
    private StringBuilder output;
    /**
     * When having an iteration number of 10000, the test will almost certainly fail.
     */
    private static final int ITERATION_NUM = 10;

    @BeforeAll
    protected void setUp() throws Exception {
        hasError = false;
        output = new StringBuilder();
        space = SemiSpace.retrieveSpace();
    }

    public static class TestObject {
        private String a;
        private Integer b;

        public TestObject() {
        }

        public TestObject(String a) {
            this.a = a;
        }

        public String getA() {
            return a;
        }

        public void setA(String a) {
            this.a = a;
        }

        public Integer getB() {
            return b;
        }

        public void setB(Integer b) {
            this.b = b;
        }

        public void increment() {
            this.b += 1;
        }
    }

    private Runnable makeReader(final String n, final TestObject tmpl) {
        return new Runnable() {

            public void run() {
                while (true) {
                    TestObject o = space.read(tmpl, 1000);
                    if (o != null) {
                        output.append("(" + n + ") read " + o.getB() + "\n");
                    } else {
                        hasError = true;
                        fail("(" + n + ") read timeout (bug) Statistics:\n" + ((SemiSpace) space).getStatistics());
                    }
                    //assertEquals("Missed reading value: " + i, i, o.getB());
                    if (hasError) {
                        break;
                    }
                    if (o.getB() >= ITERATION_NUM) {
                        output.append("(" + n + ") Read last\n");
                        break;
                    }

                    try {
                        // simulate work
                        Thread.sleep(25);
                    } catch (InterruptedException ex) {
                        log.warn("Inconsequential, but unexpected.", ex);
                    }

                }
            }
        };
    }

    private Runnable makeTaker(final String n, final TestObject tmpl) {
        return new Runnable() {

            public void run() {
                TestObject obj = null;
                do {
                    obj = space.take(tmpl, 1000);
                    // System.out.println("Took in "+n+" "+obj.getB());
                    if (obj != null) {
                        obj.increment();
                        try {
                            // simulate update time
                            Thread.sleep(25);
                            output.append("(" + n + ") writing " + obj.getB() + "  " + ((SemiSpace) space).getStatistics() + "\n");
                        } catch (InterruptedException ex) {
                            log.error("Exception", ex);
                            output.append("interrupted");
                        } finally {
                            space.write(obj, 5000);
                        }

                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            log.error("Exception", ex);
                        }
                    } else {
                        hasError = true;
                        fail("(" + n + ") take timeout (bug) Statistics:\n" + ((SemiSpace) space).getStatistics());
                    }
                } while ( obj.getB() < ITERATION_NUM && !hasError);
            }
        };
    }


    /**
     * When having several listeners and a combination of read and take, it possible for listeners to
     * get starved when competing for resource.
     * In this test, this happens after 40 missed reads for a given reader.
     */
    @Test
    public void testStarvation() {
        final TestObject tmpl = new TestObject("test");
        // write the singe object
        TestObject obj = new TestObject("test");
        obj.setB(0);
        space.write(obj, 1000);

        Thread r1 = Thread.ofVirtual().start(makeReader("1", tmpl));
        Thread r2 = Thread.ofVirtual().start(makeReader("2", tmpl));
        Thread r3 = Thread.ofVirtual().start(makeReader("3", tmpl));
        Thread r4 = Thread.ofVirtual().start(makeReader("4", tmpl));
        Thread r5 = Thread.ofVirtual().start(makeReader("5", tmpl));
        Thread r6 = Thread.ofVirtual().start(makeReader("6", tmpl));
        Thread t1 = Thread.ofVirtual().start(makeTaker("a", tmpl));
        Thread t2 = Thread.ofVirtual().start(makeTaker("b", tmpl));

        try {
            t1.join();
            t2.join();
            r1.join();
            r2.join();
            r3.join();
            r4.join();
            r5.join();
            r6.join();
        } catch (InterruptedException ex) {
            log.error("Unexpected", ex);
        }
        if (hasError) {
            log.error(output.toString());
            log.error("Statistics:\n" + ((SemiSpace) space).getStatistics());
        }
        assertFalse(hasError, "State of starvation has been achieved. This does not really indicate an error " +
                "as the behaviour for this to happen is intrinsically correct: The timeout values " +
                "(in milliseconds) have stabilized into the same value. This test is basically made " +
                "for caching if this happens more often than should be expected. It may happen sporadically " +
                "when building with tests, particularly (I expect) when having a quick computer.\n" + output);
        assertNotNull(space.takeIfExists(tmpl), "Presumed the template to be present in space at end of test");
    }
}
