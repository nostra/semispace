/*
 * ============================================================================
 *
 *  File:     SemiSpaceTest.java
 *----------------------------------------------------------------------------
 *
 * Copyright 2012 Erlend Nossum
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
 *  Created:      Feb 19, 2012
 * ============================================================================ 
 */

package org.semispace;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.semispace.event.SemiEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 * This is basically a benchmark test. The assertion parts of this is not really interesting.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ManyListenersTest {
    private static final Logger log = LoggerFactory.getLogger(ManyListenersTest.class);

    private SemiSpaceInterface space;

    @BeforeAll
    public void setUp() throws Exception {
        space = SemiSpace.retrieveSpace();
    }

    @Test
    public void manyListeners() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        SemiEventRegistration[] registrations = new SemiEventRegistration[100000];
        log.debug("Creating "+registrations.length+" listeners");
        for ( int i=0 ; i < registrations.length ; i++ ) {
            FieldHolder fh = new FieldHolder();
            fh.setFieldA("A:"+i);
            fh.setFieldB("B:"+i);
            SemiEventListener listen = new SpecificListener(latch);
            registrations[i] = space.notify(fh, listen, SemiSpace.ONE_DAY);
        }
        log.debug("writing object to space");
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("A:"+19999);
        fh.setFieldB("B:" + 19999);
        space.write(fh, 1000);
        fh.setFieldA("A:" + ( registrations.length - 1));
        fh.setFieldB("B:" + (registrations.length - 1));
        space.write(fh, 1000);
        fh.setFieldA("A:" + (registrations.length / 2));
        fh.setFieldB("B:" + (registrations.length / 2));
        space.write(fh, 1000);

        latch.await();
        log.debug("cancelling listeners");
        for ( int i=0 ; i < registrations.length ; i++ ) {
            registrations[i].getLease().cancel();
        }
        for ( int i=0 ; i < 3 ; i++ ) {
            assertNotNull(space.takeIfExists(new FieldHolder()),
                    "Expecting to be quick enough to trigger events within a second");
        }
    }

    private class SpecificListener implements SemiEventListener {

        private CountDownLatch latch;

        public SpecificListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void notify(SemiEvent theEvent) {
            log.debug("Got matching event "+theEvent.getId());
            latch.countDown();
        }
    }
}
