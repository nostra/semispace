/*
 * ============================================================================
 *
 *  File:     PongActor.java
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
 *  Created:      Jul 19, 2008
 * ============================================================================
 */

package org.semispace.actor.example;

import org.semispace.SemiSpaceInterface;
import org.semispace.actor.Actor;
import org.semispace.actor.ActorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PongActor extends Actor {
    private static final Logger log = LoggerFactory.getLogger(PongActor.class);
    private int pongCount = 0;

    public int getPongCount() {
        return pongCount;
    }

    public PongActor(SemiSpaceInterface space) {
        register(space);
    }

    @Override
    public void receive(ActorMessage msg) {
        if (msg.isOfType(Pong.class)) {
            throw new RuntimeException("Did not expect, and should not, receive messages from myself.");
        }

        if (msg.isOfType(Ping.class)) {
            if (pongCount % 1000 == 0) {
                System.out.println("Pong: ping " + pongCount);
            }
            pongCount++;
            //log.debug("Sending pong (from "+getActorId()+") to actor "+msg.getOriginatorId());
            send(msg.getOriginatorId(), new Pong());

        } else if (msg.isOfType(Stop.class)) {
            System.out.println("Pong stopped");
            unregister();
//            SemiSpaceAdmin admin  = (SemiSpaceAdmin) ((SemiSpace)SemiSpace.retrieveSpace()).getAdmin();
//            admin.shutdownAndAwaitTermination();
        } else {
            throw new RuntimeException("Ops. Did not expect messages of unexpected type. Got " + msg.getPayload().getClass().getName());
        }
    }

    @Override
    public Object[] getReadTemplates() {
        return new Object[]{new Stop()};
    }

    @Override
    public Object[] getTakeTemplates() {
        log.debug("Returning ping as template");
        return new Object[]{new Ping()};
    }


}
