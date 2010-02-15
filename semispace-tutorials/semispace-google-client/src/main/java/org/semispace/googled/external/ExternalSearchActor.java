/*
 * ============================================================================
 *
 *  File:     ExternalSearchActor.java
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
 *  Created:      Dec 28, 2008
 * ============================================================================ 
 */

package org.semispace.googled.external;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.actor.Actor;
import org.semispace.actor.ActorMessage;
import org.semispace.google.transport.AddressQuery;
import org.semispace.google.transport.GoogleAddress;
import org.semispace.googled.bean.LoginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalSearchActor extends Actor {
    private static final Logger log = LoggerFactory.getLogger(ExternalSearchActor.class);
            
    private SemiSpaceInterface tokenSpace;

    public ExternalSearchActor(SemiSpaceInterface tokenSpace) {
        this.tokenSpace = tokenSpace;
        register(SemiSpace.retrieveSpace());
    }

    @Override
    public void receive(ActorMessage msg) {
        if ( msg.isOfType(AddressQuery.class) ) {
            AddressQuery query = (AddressQuery) msg.getPayload();
            try {
                tokenSpace.write(query, 2500);
                GoogleAddress ga = new GoogleAddress();
                ga.setAddress(query.getAddress());
                GoogleAddress result = (GoogleAddress) tokenSpace.read(ga, 5000);
                if ( result == null ) {
                    result = ga;
                    ga.setAddress("Query timed out (on client) - no address available.");
                    ga.setAccuracy("-1");
                } 
                send(msg.getOriginatorId(), result);

            } catch (Exception e) {
                log.error("Got exception searching. Returning to login screen.", e);
                LoginResult logout = new LoginResult();
                logout.setDidGetToken( Boolean.FALSE );
                send( logout);
            }
            
        } else {
            log.warn("Got message of unexpected type: "+msg.getPayload().getClass().getName());
        }
    }

    @Override
    public Object[] getTakeTemplates() {
        return new Object[] {new AddressQuery()};
    }

}
