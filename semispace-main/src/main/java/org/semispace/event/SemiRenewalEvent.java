/*
 * ============================================================================
 *
 *  File:     SemiRenewalEvent.java
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
 *  Created:      May 26, 2008
 * ============================================================================ 
 */

package org.semispace.event;

import org.terracotta.annotations.InstrumentedClass;

/**
 * Object with id has been renewed, and now has a different expiration date, which may be shorter...
 */
@InstrumentedClass
public class SemiRenewalEvent extends SemiEvent {

    private long id;
    private long liveUntil;
    
    public long getLiveUntil() {
        return this.liveUntil;
    }

    public SemiRenewalEvent(long id, long liveUntil) {
        this.id = id;
        this.liveUntil = liveUntil;
    }

    @Override
    public long getId() {
        return id;
    }

}
