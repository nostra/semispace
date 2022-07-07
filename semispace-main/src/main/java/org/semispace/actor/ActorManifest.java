/*
 * ============================================================================
 *
 *  File:     ActorManifest.java
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
 *  Created:      Aug 14, 2008
 * ============================================================================
 */

package org.semispace.actor;

/**
 * The manifest is used when an object is sent without having a clear destination.
 */
public class ActorManifest {
    private Long holderId;
    private Long originatorId;

    public ActorManifest(Long holderId, Long originatorId) {
        this.holderId = holderId;
        this.originatorId = originatorId;
    }

    public ActorManifest(long id) {
        this.holderId = Long.valueOf(id);
    }

    public Long getHolderId() {
        return this.holderId;
    }

    public Long getOriginatorId() {
        return this.originatorId;
    }
}
