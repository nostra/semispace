/*
 * ============================================================================
 *
 *  File:     PingActor.java
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

import java.util.concurrent.atomic.AtomicInteger;

// @SwingActor
public class PingActor extends Actor {
    private AtomicInteger numberOfPings;
    private long bench;

    public int getNumberOfPings() {
        return numberOfPings.get();
    }

    public PingActor(int numberOfPings, SemiSpaceInterface space) {
        this.numberOfPings = new AtomicInteger(numberOfPings);
        register(space);
    }

    public void fireItUp() {
        System.out.println("firing ping");
        bench = System.currentTimeMillis();
        send(new Ping());

    }

    @Override
    public void receive(ActorMessage msg) {
        if (msg.isOfType(Pong.class)) {
            if (numberOfPings.get() % 1000 == 0) {
                System.out.println("Ping: pong");
            }
            if (numberOfPings.decrementAndGet() > 0) {
                send(msg.getOriginatorId(), new Ping());
            } else {
                System.out.println("Ping: sending stop after " + (System.currentTimeMillis() - bench) + " ms");
                send(msg.getOriginatorId(), new Stop());
                unregister();
            }

        } else {
            throw new RuntimeException("Did not expect message of any type than pong. Got " + msg.getPayload().getClass().getName());
        }
    }

}
