/*
 * ============================================================================
 *
 *  File:     ActorNotification.java
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
 *  Created:      Jul 21, 2008
 * ============================================================================ 
 */

package org.semispace.actor;

import com.thoughtworks.xstream.XStream;
import org.semispace.SemiEventListener;
import org.semispace.SemiSpaceInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.exception.ActorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Listener of a semispace template. The actor will be notified
 * with actor message.
 */
public class ActorNotification implements SemiEventListener {
    private static final Logger log = LoggerFactory.getLogger(ActorNotification.class);

    private Actor actor;

    private SemiSpaceInterface space;

    private Object template;

    private ExecutorService pool = null;

    private boolean toTake;

    public ActorNotification(Actor actor, SemiSpaceInterface space, Object template, boolean toTake) {
        this.actor = actor;
        this.space = space;
        this.template = template;
        this.toTake = toTake;
        
        // Do not need pool if swing actor
        if ( ! actor.getClass().isAnnotationPresent(SwingActor.class)) {
            pool = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 5L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>());    
        }
        
    }

    @Override
    public void notify(final SemiEvent theEvent) {
        // log.debug("incoming event "+theEvent.getId()+" "+theEvent.getClass().getName());

        if (theEvent instanceof SemiAvailabilityEvent) {
            final Runnable receive = new ActorMessageTaker(theEvent);
            if ( pool == null ) {
                SwingUtilities.invokeLater(receive);
            } else {                
                pool.submit(receive);
            }
        }
    }

    /*
     * public void shutdown() { pool.shutdown(); try { if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
     * pool.shutdownNow(); // Cancel currently executing tasks pool.awaitTermination(60, TimeUnit.SECONDS); } } catch
     * (InterruptedException ie) { pool.shutdownNow(); Thread.currentThread().interrupt(); } }
     */

    private class ActorMessageTaker implements Runnable {
        private final SemiEvent theEvent;

        public ActorMessageTaker(SemiEvent theEvent) {
            this.theEvent = theEvent;
        }

        @Override
        public void run() {
            Object element = null;
            if (toTake) {
                element = space.takeIfExists(template);
            } else {
                element = space.readIfExists(template);
            }
            //log.debug("incoming event with " + (element == null ? "null" : element.getClass().getName()));

            // final long holderId = theEvent.getId();
            ActorMessage payload = null;
            if (element instanceof ActorMessage) {
                payload = (ActorMessage) element;
            } else if (element != null) {
                payload = new ActorMessage();
                payload.setPayload(element);
                // Trying to find the manifest
                ActorManifest manifest = new ActorManifest(theEvent.getId());
                //log.debug("Trying to take with manifest " + theEvent.getId());
                if (toTake) {
                    manifest = space.take(manifest, 1000);
                } else {
                    manifest = space.read(manifest, 1000);
                }
                if (manifest != null) {
                    payload.setOriginatorId(manifest.getOriginatorId());
                }
            } else {
                log.debug("Probably having competing listeners, and this listener was not quick enough to take the object.");
                return;
            }


            final ActorMessage msg = payload;
            if ( msg.getOriginatorId() == null ){
                throw new ActorException("Originator was not found for message with address "+msg.getAddress()+" and payload "+msg.getPayload().getClass().getName());
            }
            //final long holderId = theEvent.getId();
            //log.debug("Holder id=" + holderId + " Notifying "+ actor.getActorId()+" ("+actor.getClass().getName() + ") of "+ msg.getPayload().getClass()+" with address "+msg.getAddress());
            try {
                actor.receive(msg);
            } catch (Exception e) {
                XStream xStream  = new XStream();
                log.error("Got exception with template:\n"+xStream.toXML(template)+"\n... and incoming actor message ...\n"+xStream.toXML(msg), e);
            }
        }
    }
}
