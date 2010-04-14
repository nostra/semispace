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

package org.semispace.comet.client.multithread;

import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.comet.client.SemiSpaceCometProxy;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class NotifyAndReadClient implements SemiEventListener {
    private static final Logger log = LoggerFactory.getLogger(NotifyAndReadClient.class);
    private SemiSpaceCometProxy space;
    private SemiEventRegistration lease;
    private String readField = "Did not have time to get notified, at least this field was never read from space.";

    public String getReadField() {
        return readField;
    }

    public NotifyAndReadClient(SemiSpaceCometProxy space ) {
        this.space = space;
        //space.init("http://localhost:8080/semispace-comet-server/cometd/");
    }

    public void destroy() {
        lease.getLease().cancel();
        //space.destroy();        
    }

    public void activate() {
        lease = space.notify(new JustATestElement(), this, SemiSpace.ONE_DAY);
    }

    @Override
    public void notify(SemiEvent theEvent) {
        if ( theEvent instanceof SemiAvailabilityEvent) {
            log.debug("Availability: "+theEvent.getId()+" "+theEvent.getClass());
            JustATestElement jat = space.read(new JustATestElement() ,1000);
            if ( jat != null ) {
                this.readField = jat.getSomefield();
            } else {
                this.readField = "Could not read the element.";
            }
        }
    }
}
