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

import org.semispace.SemiEventListener;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.event.SemiRenewalEvent;
import org.semispace.event.SemiTakenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationClientIntegration implements SemiEventListener {
    private static final Logger log = LoggerFactory.getLogger(NotificationClientIntegration.class);
    private int expiration;
    private int availability;
    private int taken;
    private int renewal;

    public int getExpiration() {
        return expiration;
    }

    public int getAvailability() {
        return availability;
    }

    public int getTaken() {
        return taken;
    }

    public int getRenewal() {
        return renewal;
    }

    @Override
    public void notify(SemiEvent theEvent) {
        if (theEvent instanceof SemiExpirationEvent) {
            SemiExpirationEvent expirationEvent = (SemiExpirationEvent) theEvent;
            expiration++;
            log.debug("Got expiration event id " + expirationEvent.getId());

        } else if (theEvent instanceof SemiAvailabilityEvent) {
            SemiAvailabilityEvent availabilityEvent = (SemiAvailabilityEvent) theEvent;
            availability++;
            log.debug("Got availability event id " + availabilityEvent.getId());

        } else if (theEvent instanceof SemiTakenEvent) {
            SemiTakenEvent takenEvent = (SemiTakenEvent) theEvent;
            taken++;
            log.debug("Got taken event id " + takenEvent.getId());

        } else if (theEvent instanceof SemiRenewalEvent) {
            SemiRenewalEvent renewalEvent = (SemiRenewalEvent) theEvent;
            renewal++;
            log.debug("Got taken renewal event id " + renewalEvent.getId());
        } else {
            log.error("Not expected at all: Got id " + theEvent.getId() + ", event class " + theEvent.getClass().getName());
        }
    }
}