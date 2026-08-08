/*
 * ============================================================================
 *
 *  File:     ActorMessage.java
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


public class ActorMessage {
    private Long originatorId;
    private Long address;
    private Object payload;


    /**
     * The payload of this message
     */
    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    /**
     * The destination actor for the message. Should <b>not</b> be used
     * for purposes outside the framework. This method is public in
     * order to get the space to pick it up.
     */
    public Long getAddress() {
        return address;
    }

    /**
     * Sender of the message. This is the value you would use
     * to reply to when using the space.
     */
    public Long getOriginatorId() {
        return originatorId;
    }

    public void setOriginatorId(Long originatorId) {
        this.originatorId = originatorId;
    }

    /**
     * Use this method to figure out if the payload is of a certain type.
     */
    public boolean isOfType(Class<?> clazz) {
        return payload.getClass().isAssignableFrom(clazz);
    }

    public void setAddress(Long whichActorToSendTo) {
        address = whichActorToSendTo;
    }


}
