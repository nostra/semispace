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

package org.semispace.comet.client.notification;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.comet.client.FieldHolder;
import org.semispace.comet.client.SemiSpaceCometProxy;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.event.SemiRenewalEvent;
import org.semispace.event.SemiTakenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class NotificationClientIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(NotificationClientIntegrationTest.class);

    private SemiSpaceCometProxy space;

    @Before
    public void setUp() {
        log.warn("\n\n\n\nNOT SUPPORTING NORMAL BUILD TESTS YET\nUse\n  mvn -Denv=dev clean install\nwhen building this module\n\n\n");
        space = new SemiSpaceCometProxy();
        space.init("http://localhost:8080/semispace-comet-server/cometd/");
        // If running within eclipse, you will have this on your classpath
        //space = SemiSpaceProxy.retrieveSpace("http://localhost:8080/semispace-war/services/space");
    }

    @Test
    public void testSimpleNotify() throws InterruptedException {
        NotificationTestListener listener = new NotificationTestListener();
        SemiEventRegistration lease = space.notify(new FieldHolder(), listener, 1000);
        FieldHolder onlyOne = new FieldHolder();
        onlyOne.setFieldA("A");
        onlyOne.setFieldB("B");
        space.write(onlyOne, 900);
        space.write(new FieldHolder(), 50); // Any
        space.write(new FieldHolder(), 50); // Any
        Assert.assertNotNull( space.read(onlyOne, 900)); // Reading the one
        Assert.assertNotNull( space.take(onlyOne, 900)); // Taking the one
        Assert.assertNull( "Just using some time in order to get space elements notified properly", space.take(onlyOne, 300));
        Assert.assertEquals(3, listener.availability);
        Assert.assertEquals(1, listener.taken);
        // Cannot test listener expiration, as it depends on the sequence of objects in the space, which now
        // is unordered
        // Assert.assertEquals("0, 1 or 2 objects tend to be expired.", 2, listener.expiration);
        Assert.assertEquals(0, listener.renewal);

        Assert.assertNull( "Space should now be empty", space.read(onlyOne, 50));
        lease.getLease().cancel();
    }

    @Test
    public void testThatCancellationOfNotificationWorks() {
        NotificationTestListener listener = new NotificationTestListener();
        SemiEventRegistration lease = space.notify(new FieldHolder(), listener, 1000);
        lease.getLease().cancel();
        FieldHolder onlyOne = new FieldHolder();
        onlyOne.setFieldA("A");
        onlyOne.setFieldB("B");
        space.write(onlyOne, 900);
        Assert.assertNotNull(space.take(onlyOne, 100));
        // Pausing for 300ms
        Assert.assertNull(space.take(onlyOne, 300));
        
        Assert.assertEquals("As the listener was cancelled, I should not have been triggered.", 0, listener.availability);
        Assert.assertEquals(0, listener.taken);
    }

    private class NotificationTestListener implements SemiEventListener {
        private int expiration;
        private int availability;
        private int taken;
        private int renewal;

        @Override
        public void notify(SemiEvent theEvent) {
            if ( theEvent instanceof SemiExpirationEvent ) {
                SemiExpirationEvent expirationEvent = (SemiExpirationEvent) theEvent;
                expiration++;
                log.debug("Got expiration event id "+expirationEvent.getId());

            } else if ( theEvent instanceof SemiAvailabilityEvent) {
                SemiAvailabilityEvent availabilityEvent = (SemiAvailabilityEvent) theEvent;
                availability++;
                log.debug("Got availability event id "+availabilityEvent.getId());

            } else if ( theEvent instanceof SemiTakenEvent) {
                SemiTakenEvent takenEvent = (SemiTakenEvent) theEvent;
                taken++;
                log.debug("Got taken event id "+takenEvent.getId());

            } else if ( theEvent instanceof SemiRenewalEvent) {
                SemiRenewalEvent renewalEvent = (SemiRenewalEvent) theEvent;
                renewal++;
                log.debug("Got taken renewal event id "+renewalEvent.getId());
            } else {
                log.error("Not expected at all: Got id "+theEvent.getId()+", event class "+theEvent.getClass().getName());
            }
        }
    }
}
