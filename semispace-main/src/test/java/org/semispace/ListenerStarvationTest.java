package org.semispace;

import junit.framework.TestCase;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starvation test. Thanks to <b>Chris Mcfarlen</b> of Yahoo for providing the
 * scenario and the initial test code.
 *
 * <p>
 * This test will fail with a large iteration number for reasons not pinpointed at the
 * time of writing.
 * </p>
 */
public class ListenerStarvationTest extends TestCase {
	private SemiSpaceInterface space;
    private boolean hasError;
    private StringBuilder output;
    /**
     * When having an iteration number of 10000, the test will almost certainly fail.
     */
    private static final int ITERATION_NUM=10;
    /**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public ListenerStarvationTest( String testName ) {
		super( testName );
	}

    @Override
	protected void setUp() throws Exception {
		super.setUp();
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
						output.append("("+n+") read " + o.getB()+"\n");
                    } else {
                        hasError = true;
                        fail("("+n+") read timeout (bug) Statistics:\n"+((SemiSpace)space).getStatistics());
                    }
					//assertEquals("Missed reading value: " + i, i, o.getB());
                    if (hasError ) {
                        break;
                    }
                    if (o.getB() >= ITERATION_NUM ) {
                        output.append("("+n+") Read last\n");
                        break;
                    }

					try {
						// simulate work
						Thread.sleep(25);
					} catch (InterruptedException ex) {
						Logger.getLogger(ListenerStarvationTest.class.getName()).log(Level.SEVERE, null, ex);
					}

				}
			}
		};
	}

	private Runnable makeTaker(final String n, final TestObject tmpl) {
		return new Runnable() {

			public void run() {
				TestObject obj = null;
				while ((obj == null || obj.getB() < ITERATION_NUM ) && !hasError ) {
					obj = space.take(tmpl, 1000);
					if (obj != null) {
						obj.increment();
						try {
							// simulate update time
							Thread.sleep(25);
						} catch (InterruptedException ex) {
							Logger.getLogger(ListenerStarvationTest.class.getName()).log(Level.SEVERE, null, ex);
						}
						output.append("("+n+") writing " + obj.getB()+"  "+((SemiSpace)space).getStatistics()+"\n");
						space.write(obj, 5000);

						try {
							Thread.sleep(10);
						} catch (InterruptedException ex) {
							Logger.getLogger(ListenerStarvationTest.class.getName()).log(Level.SEVERE, null, ex);
						}
					} else {
                        hasError = true;
                        fail("("+n+") take timeout (bug) Statistics:\n"+((SemiSpace)space).getStatistics());
					}
				}
			}
		};
	}


    /**
     * When having several listeners and a combination of read and take, it possible for listeners to
     * get starved when competing for resource.
     * In this test, this happens after 40 missed reads for a given reader.
     */
	public void testStarvation() {
		final TestObject tmpl = new TestObject("test");


		Thread r1 = new Thread(null, makeReader("1", tmpl));
		Thread r2 = new Thread(null, makeReader("2", tmpl));
		Thread r3 = new Thread(null, makeReader("3", tmpl));
        Thread r4 = new Thread(null, makeReader("4", tmpl));
        Thread r5 = new Thread(null, makeReader("5", tmpl));
        Thread r6 = new Thread(null, makeReader("6", tmpl));
		Thread t1 = new Thread(null, makeTaker("a", tmpl));
		Thread t2 = new Thread(null, makeTaker("b", tmpl));

		// write the singe object
		TestObject obj = new TestObject("test");
		obj.setB(0);
		space.write(obj, 1000);

		r1.start();
		r2.start();
		r3.start();
        r4.start();
        r5.start();
        r6.start();
		t1.start();
		t2.start();

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
			Logger.getLogger(ListenerStarvationTest.class.getName()).log(Level.SEVERE, null, ex);
		}
        if ( hasError ) {
            System.out.println(output);
            System.out.println("Statistics:\n"+((SemiSpace)space).getStatistics());
        }
        assertFalse("State of starvation has been achieved. This does not really indicate an error " +
                "as the behaviour for this to happen is intrinsically correct: The timeout values " +
                "(in milliseconds) have stabilized into the same value. This test is basically made " +
                "for caching if this happens more often than should be expected. It may happen sporadically " +
                "when building with tests, particularly (I expect) when having a quick computer.\n"+output, hasError );
	}
}
