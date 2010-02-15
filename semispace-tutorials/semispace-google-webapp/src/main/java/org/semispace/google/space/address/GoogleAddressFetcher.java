/*
 * ============================================================================
 *
 *  File:     GoogleAddressFetcher.java
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
 *  Created:      Oct 4, 2008
 * ============================================================================ 
 */

package org.semispace.google.space.address;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.google.transport.AddressQuery;
import org.springframework.beans.factory.DisposableBean;

public class GoogleAddressFetcher implements DisposableBean, SemiEventListener{
    private static final Logger log = LoggerFactory.getLogger(GoogleAddressFetcher.class);

    protected static final long TEN_YEARS = SemiSpace.ONE_DAY*365*10;
    
    private SemiEventRegistration lease = null; 
    
    private SemiSpaceInterface space;
    private int simultanousLookups;

    private ExecutorService threadPool;
    public int getSimultanousLookups() {
        return this.simultanousLookups;
    }

    public void setSimultanousLookups(int simultanousLookups) {
        this.simultanousLookups = simultanousLookups;
    }

    public void setSpace(SemiSpaceInterface space) {
        this.space = space;
    }    
    
    public void init() {
        log.debug("Initializing "+getClass().getName());
        threadPool = Executors.newCachedThreadPool();
        
        // TODO Que?
        //AddressLookupSemaphore counter = new AddressLookupSemaphore();
        if ( simultanousLookups < 1 ) {
            throw new RuntimeException("Configuration error - need to configure field simultanousLookups to a value greater than 0.");
        }
        resetSemaphores();
        // Add notifier for query
        if ( lease != null ) {
            log.warn("Double initialization? Strange.");
            lease.getLease().cancel();
        }
        lease = space.notify(new AddressQuery(), this, TEN_YEARS);
    }

    private void resetSemaphores() {
        while (space.takeIfExists(new AddressLookupSemaphore()) != null ) {
            // Intentional
        }
        for ( int i=0 ; i < simultanousLookups ; i++ ) {
            space.write(new AddressLookupSemaphore(),TEN_YEARS);
        }
    }

    public void destroy() throws Exception {
        if ( lease != null ) {
            lease.getLease().cancel();
        }
    }

    public void notify(SemiEvent theEvent) {
        if ( theEvent instanceof SemiAvailabilityEvent ) {
            if ( space.readIfExists(new AddressQuery()) == null ) {
                log.warn("Could not read element even when getting availability in "+this+" Incoming event: "+theEvent.getClass().getName()+" #"+theEvent.getId());
            } else {
                log.info("Object read OK in "+this);
            }
            spawnThreadToTakeCareOfQuery();
        }
    }

    private void spawnThreadToTakeCareOfQuery() {
        threadPool.execute(new FetchAddress(space));
    }
}
