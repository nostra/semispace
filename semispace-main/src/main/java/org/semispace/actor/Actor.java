/*
 * ============================================================================
 *
 *  File:     Actor.java
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

package org.semispace.actor;

import java.util.ArrayList;
import java.util.List;

import org.semispace.NameValueQuery;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiLease;
import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Actor {
	private static final Logger log = LoggerFactory.getLogger(Actor.class);
    /** 
     * 10 years is "actorLifeMs" in this context 
     */
	private static final long FOREVER = 1000*60*60*24*365*10;
			
    private final List<SemiEventRegistration>notifications = new ArrayList<SemiEventRegistration >();
    private SemiSpaceInterface space;
    private Long actorId;
    private long defaultLifeMsOfSpaceObject = 1000;
    
    public long getDefaultLifeMsOfSpaceObject() {
        return defaultLifeMsOfSpaceObject;
    }

    /**
     * How long a message will live in the space when it is sent.
     */
    public void setDefaultLifeMsOfSpaceObject(long defaultLifeMsOfSpaceObject) {
        this.defaultLifeMsOfSpaceObject = defaultLifeMsOfSpaceObject;
    }

    /**
     * Register with the space with a very long long actor life.
     * Useful when you are actively using unregister, or expect
     * the actor to live longer than the VM itself.
     * @see Actor#unregister()
     * @see Actor#register(SemiSpaceInterface, long)
     */
    public final void register( SemiSpaceInterface registerWith ) {
        register( registerWith, FOREVER );
    }
    
    /**
     * You <b>must</b> register with a space in order to activate the 
     * actor.
     * @param actorLifeMs How long the actor shall be active in milliseconds
     * @see Actor#unregister() 
     */
    public final void register( SemiSpaceInterface registerWith, long actorLifeMs ) {
        space = registerWith;
        // Create id
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "internal";
        nvq.value = "disregard";
        SemiLease lease = space.write(nvq, 50);
        actorId = Long.valueOf(lease.getHolderId());
        lease.cancel();
        ActorNotification an = null;
        // Listen for elements to, eh, listen to
        for ( Object toTake: getTakeTemplates() ) {
            an = new ActorNotification( this, space, toTake, true );
            SemiEventRegistration registration = space.notify(toTake, an, actorLifeMs);
            if ( registration == null ) {
                throw new RuntimeException("Did not manage to register with space.");
            }
            notifications.add( registration );
        }
        for ( Object toRead: getReadTemplates() ) {
            an = new ActorNotification( this, space, toRead, false );
            SemiEventRegistration registration = space.notify(toRead, an, actorLifeMs);
            if ( registration == null ) {
                throw new RuntimeException("Did not manage to register with space.");
            }
            notifications.add( registration );
        }
        
        // Listen for messages to myself of any kind
        ActorMessage template = new ActorMessage();
        template.setAddress(actorId);
        an = new ActorNotification( this, space, template, true );
        notifications.add( space.notify(template, an, actorLifeMs));
        log.debug("Registered actor ("+getClass().getName()+") with id "+actorId+" and this object registered "+notifications.size()+" notification element(s).");
    }

    /**
     * Removing the connection(s) from the space rendering the actor, for all
     * practical purposes, dead.
     */
    public void unregister() {
        for (SemiEventRegistration registration : notifications ) {
            registration.getLease().cancel();
        }
        notifications.clear();
        space = null;
        actorId = null;
    }

    /**
     * Send <b>two</b> message to the space. The first is the object
     * to write, the other is the manifest for the message, which can
     * be used by the listening actor to find the originator.
     * This method is intended only to be used when initiating an 
     * actor dialog, as the destination is <b>unknown</b>
     */
    public void send( Object obj ) {
        if ( actorId == null ) {
            throw new RuntimeException("Actor id must be set. Did you remember to register actor class?");
        }
        SemiLease lease = space.write(obj, defaultLifeMsOfSpaceObject);
        ActorManifest manifest = new ActorManifest(Long.valueOf( lease.getHolderId()), actorId);
        space.write(manifest, defaultLifeMsOfSpaceObject);
        log.debug("Wrote manifest with holder id "+manifest.getHolderId());
       //log.debug("send actor message containing: "+new XStream().toXML(msg));
    }
    
    /**
     * This is the <b>regular</b> method for sending a message to an
     * actor. The message will be delivered directly to the indicated actor.
     * No of the parameters may be null.
     * @param destinationId When replying to a message, the originatorId should
     * be the destination id. i.e. the destination address
     */
    public void send(Long destinationId, Object payload) {
        if ( payload == null ) {
            throw new RuntimeException("No parameter is allowed to be null, but payload was. Destination id was "+destinationId);
        }
        if ( destinationId == null ) {
            throw new RuntimeException("No parameter is allowed to be null, but destination was. Are you certain that " +
                    "you used originatorId when replying? Payload class: "+payload.getClass().getName());
        }
        ActorMessage msg = new ActorMessage();
        msg.setOriginatorId(actorId);
        msg.setAddress( destinationId);
        msg.setPayload(payload);
        
        space.write(msg, defaultLifeMsOfSpaceObject);
    }
    
    /**
     * @return Id of this actor provided that it is registered in a space.
     */
    public Long getActorId() {
        return actorId;
    }
    
    /**
     * Default implementation which renders an empty array, ie nothing is
     * tried taken. Exchange with relevant elements. 
     * Object are tried taken from the space. <b>Observe</b> that
     * the object taken is the one that fits the template, and <b>not</b>
     * necessarily the one which triggered the event.
     */
    public Object[] getTakeTemplates() {
        return new Object[0];
    }

    /**
     * Default implementation which renders empty array, ie nothing is tried <b>read</b>. 
     * @see #getTakeTemplates()
     */
    public Object[] getReadTemplates() {
        return new Object[0];
    }

    /**
     * Receive a message which either matches the template, or that is addressed to
     * this actor in particular (which implies that you need to check the type
     * for each message).
     */
    public abstract void receive(ActorMessage msg);

}
