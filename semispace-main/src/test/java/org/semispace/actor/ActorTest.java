/*
 * ============================================================================
 *
 *  File:     ActorTest.java
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
 *  Created:      Aug 4, 2008
 * ============================================================================
 */

package org.semispace.actor;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.semispace.JacksonSerializer;
import org.semispace.SemiEventListener;
import org.semispace.SemiSpace;
import org.semispace.actor.example.Ping;
import org.semispace.actor.example.PingActor;
import org.semispace.actor.example.PongActor;
import org.semispace.event.SemiAvailabilityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(Lifecycle.PER_CLASS)
public class ActorTest {
    private SemiSpace space;
    private Logger log = LoggerFactory.getLogger(ActorTest.class);

    @BeforeAll
    public void setUp() {
        space = (SemiSpace) SemiSpace.retrieveSpace();
    }

    /**
     * This test was used to find a bug which was introduced when
     * the scope of a getter was mistakenly set to protected.
     */
    @Test
    public void testGiveAndTake() {
        ActorMessage template = new ActorMessage();
        template.setAddress(Long.valueOf(8));
        //template.setOriginatorId(Long.valueOf(111118));
        ActorMessage msg = new ActorMessage();
        msg.setAddress(Long.valueOf(7));

        assertNull(space.takeIfExists(msg));
        assertNull(space.takeIfExists(template));

        space.write(msg, 3600 * 24 * 1000);
        ActorMessage match = space.takeIfExists(template);

        assertNull(match, "The take template should not match any element in space. Template: \n" +
                space.getXStream().objectToXml(template) + "\n... should not match match...\n" +
                space.getXStream().objectToXml(match));
        assertNotNull(space.takeIfExists(msg));

    }

    @Test
    public void testSimpleActor() throws InterruptedException {
        if ( space.getXStream() instanceof JacksonSerializer) {
            // TODO Correct test when using jacksonserializer
            log.error("Skipping test, because JacksonSerializer is not supported by this test");
            return;
        }
        int listenerNum = space.numberOfNumberOfListeners();
        PingActor pingActor = new PingActor(10, space);
        PongActor pongActor = new PongActor(space);
        assertEquals(listenerNum + 4, space.numberOfNumberOfListeners());
        pingActor.fireItUp();
        // TODO Terracotta test will fail if not this amount of sleep. Analyze later.
        Thread.sleep(550);

        assertEquals(listenerNum, space.numberOfNumberOfListeners());
        assertEquals(10, pongActor.getPongCount());
    }

    @Test
    public void testManyCallsForActor() throws InterruptedException {
        if ( space.getXStream() instanceof JacksonSerializer) {
            // TODO Correct test when using jacksonserializer
            log.error("Skipping test, because JacksonSerializer is not supported by this test");
            return;
        }

        int listenerNum = space.numberOfNumberOfListeners();
        PingActor pingActor = new PingActor(2000, space);
        PongActor pongActor = new PongActor(space);
        assertEquals(listenerNum + 4, space.numberOfNumberOfListeners());
        pingActor.fireItUp();
        int count = -1;
        int updt = 0;
        do {
            count = updt;
            Thread.sleep(150);
            updt = pingActor.getNumberOfPings();
        } while (count != updt);
        Thread.sleep(200);

        assertEquals(listenerNum, space.numberOfNumberOfListeners());
        assertEquals(2000, pongActor.getPongCount());
    }

    @Test
    public void testListeningForPing() throws InterruptedException {
        NotificationTestListener listener = new NotificationTestListener();
        space.notify(new Ping(), listener, 1000);
        assertFalse(listener.notified);
        space.write(new Ping(), 100);
        Thread.sleep(95);
        assertTrue(listener.notified);
    }

    protected class NotificationTestListener implements SemiEventListener<SemiAvailabilityEvent> {
        protected boolean notified = false;

        public void notify(SemiAvailabilityEvent ignore) {
            notified = true;
        }
    }

}
