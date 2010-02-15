/*
 * ============================================================================
 *
 *  File:     SwingActorSearch.java
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

package org.semispace.googled.internal;

import javax.swing.AbstractAction;
import javax.swing.JTextArea;

import org.semispace.SemiSpace;
import org.semispace.actor.Actor;
import org.semispace.actor.ActorMessage;
import org.semispace.actor.SwingActor;
import org.semispace.google.transport.AddressQuery;
import org.semispace.google.transport.GoogleAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// START SNIPPET: swingActor
@SwingActor
public class SwingActorSearch extends Actor {
// END SNIPPET: swingActor
    private static final Logger log = LoggerFactory.getLogger(SwingActorSearch.class);
    private JTextArea fillArea;
    private AbstractAction swingAction;

    public SwingActorSearch(AbstractAction abstractAction, JTextArea searchResult) {
        this.swingAction = abstractAction;
        this.fillArea = searchResult;
        register(SemiSpace.retrieveSpace());
    }

    @Override
    // START SNIPPET: actorReceive
    public void receive(ActorMessage msg) {
        if ( msg.isOfType(GoogleAddress.class)) {
            swingAction.setEnabled(true);
            fillArea.setText(msg.getPayload().toString());            
        } else {
            log.warn("Unexpected message: "+msg.getPayload().getClass().getName());
        }
    }
    // END SNIPPET: actorReceive

    public void doSearch(String address) {
        swingAction.setEnabled(true);
        // START SNIPPET: actorSend
        AddressQuery query = new AddressQuery();
        query.setAddress(address);
        send( query );
        // END SNIPPET: actorSend
    }

}
