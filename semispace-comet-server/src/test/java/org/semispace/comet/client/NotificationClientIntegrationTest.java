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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semispace.SemiEventListener;
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
    public void testSimpleNotify() {
        space.notify(new FieldHolder(), new NotificationTestListener(), 1000);
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("A");
        fh.setFieldB("B");
        space.write(fh, 900);
        Assert.assertNotNull( space.read(fh, 900));
        Assert.assertNotNull( space.take(fh, 400));
    }

    private class NotificationTestListener implements SemiEventListener {
        @Override
        public void notify(SemiEvent theEvent) {
            log.debug("Incoming event id: "+theEvent.getId());
            if ( theEvent instanceof SemiExpirationEvent ) {
                SemiExpirationEvent expirationEvent = (SemiExpirationEvent) theEvent;
                log.debug("Got expiration event id "+expirationEvent.getId());

            } else if ( theEvent instanceof SemiAvailabilityEvent) {
                SemiAvailabilityEvent availabilityEvent = (SemiAvailabilityEvent) theEvent;
                log.debug("Got availability event id "+availabilityEvent.getId());

            } if ( theEvent instanceof SemiTakenEvent) {
                SemiTakenEvent takenEvent = (SemiTakenEvent) theEvent;
                log.debug("Got taken event id "+takenEvent.getId());

            } if ( theEvent instanceof SemiRenewalEvent) {
                SemiRenewalEvent renewalEvent = (SemiRenewalEvent) theEvent;
                log.debug("Got taken renewal event id "+renewalEvent.getId());

            } else {
                log.error("Not expected at all: Got id "+theEvent.getId()+" event class "+theEvent.getClass().getName());
            }
        }
    }
}
