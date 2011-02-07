/*
 * ============================================================================
 *
 *  File:     ElementLease.java
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
 *  Created:      Apr 3, 2008
 * ============================================================================ 
 */

package org.semispace;

/**
 * Lease for an object in the space
 */
public class ElementLease implements SemiLease {
    private Holder holder;
    private SemiSpace space;


    public ElementLease(Holder holder, SemiSpace space ) {
        this.holder = holder;
        this.space = space;
    }

    /**
     * @see org.semispace.SemiLease#cancel()
     */
    public boolean cancel() {
        return space.cancelElement( Long.valueOf( holder.getId() ), false, holder.getClassName(), true);
    }

    /**
     * @see org.semispace.SemiLease#renew(long)
     */
    public boolean renew(long duration) {
        return space.renewElement( holder, duration, true);
    }
    public long getHolderId() {
        return holder.getId();
    }

    
}
