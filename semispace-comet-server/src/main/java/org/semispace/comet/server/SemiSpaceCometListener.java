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

package org.semispace.comet.server;

import org.cometd.Client;
import org.semispace.SemiEventListener;
import org.semispace.comet.common.CometConstants;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.event.SemiRenewalEvent;
import org.semispace.event.SemiTakenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class SemiSpaceCometListener implements SemiEventListener {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceCometListener.class);
    private final Class interestingClass;
    private final NotificationService service;
    private final Client client;
    private String callId;
    public static final String EVENT_EXPIRATION = "expiration";
    public static final String EVENT_AVAILABILITY = "availability";
    public static final String EVENT_TAKEN = "taken";
    public static final String EVENT_RENEW = "renewal";
    public static final String EVENT_ALL = "all";

    public SemiSpaceCometListener(String listenerType, String callId, Client remote, NotificationService notificationService) {
        this.interestingClass = mapListenerType( listenerType );
        this.service = notificationService;
        this.callId = callId;
        this.client = remote;
    }

    private Class mapListenerType(String listenerType) {
        if ( EVENT_AVAILABILITY.equals( listenerType )) {
            return SemiAvailabilityEvent.class;
        } else if ( EVENT_TAKEN.equals( listenerType )) {
            return SemiTakenEvent.class;
        } else if ( EVENT_EXPIRATION.equals( listenerType )) {
            return SemiExpirationEvent.class;
        } else if ( EVENT_RENEW.equals( listenerType )) {
            return SemiRenewalEvent.class;
        } else if ( EVENT_ALL.equals( listenerType )) {
            return Object.class;
        } else {
            throw new RuntimeException("Erroneous listener type given: "+listenerType);
        }
    }

    @Override
    public void notify(SemiEvent theEvent) {
        log.debug("Got incoming event of type: "+theEvent.getClass().getName());
        if ( interestingClass.isAssignableFrom(theEvent.getClass()) ) {
            log.trace("Interesting event of type: {}",theEvent.getClass().getName());

            Map<String, String> output = new HashMap<String, String>();
            output.put("objectId", ""+theEvent.getId());
            final String outChannel = CometConstants.NOTIFICATION_EVENT_CHANNEL+"/"+callId+"/"+mapEventToListenerType(theEvent);
            try {
                service.deliver(outChannel, output, client);
            } catch ( Throwable t ) {
                log.error("Got a problem delivering", t);
            } finally {
                log.trace(">>>>>>> delivered NOTIFY on channel {} - done", outChannel);
            }            
        }
    }
    private String mapEventToListenerType(SemiEvent event ) {
        if ( event instanceof SemiExpirationEvent ) {
            return EVENT_EXPIRATION;
        } else if ( event instanceof SemiAvailabilityEvent) {
            return EVENT_AVAILABILITY;
        } if ( event instanceof SemiTakenEvent) {
            return EVENT_TAKEN;
        } if ( event instanceof SemiRenewalEvent) {
            return EVENT_RENEW;
        } else {
            throw new RuntimeException("Sanity problem. Unexpected event type "+event.getClass().getName());
        }        
    }
}
