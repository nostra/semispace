/*
 * ============================================================================
 *
 *  File:     TokenProxyActor.java
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
 *  Created:      21. des.. 2008
 * ============================================================================ 
 */

package org.semispace.googled.external;

import org.semispace.SemiSpace;
import org.semispace.actor.Actor;
import org.semispace.actor.ActorMessage;
import org.semispace.googled.bean.LoginMessage;
import org.semispace.googled.bean.LoginResult;
import org.semispace.ws.client.SemiSpaceTokenProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenProxyActor extends Actor {
    private static final Logger log = LoggerFactory.getLogger(TokenProxyActor.class);
            
    private SemiSpaceTokenProxy tokenSpace;

    public TokenProxyActor(SemiSpaceTokenProxy tokenSpace) {
        this.tokenSpace = tokenSpace;
        register(SemiSpace.retrieveSpace());
        log.debug("External connection established.");
    }

    @Override
    public void receive(ActorMessage msg) {
        if ( msg.isOfType(LoginMessage.class) ) {
            log.debug("Shall process login. Sender of login message was "+msg.getOriginatorId());
            LoginMessage login = (LoginMessage) msg.getPayload();
            tokenSpace.setUsername( login.getUsername());
            tokenSpace.setPassword(login.getPassword());
            LoginResult result = new LoginResult();
            result.setDidGetToken( Boolean.valueOf( tokenSpace.hasToken() ));
            send(msg.getOriginatorId(), result);
        } else {
            log.warn("Actor "+getActorId()+" got message from sender "+msg.getOriginatorId()+" of unexpected type: "+msg.getPayload().getClass().getName());
        }
    }

    @Override
    public Object[] getTakeTemplates() {
        return new Object[] {new LoginMessage()};
    }
}
