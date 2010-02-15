/*
 * ============================================================================
 *
 *  File:     ListenerLease.java
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
 *  Created:      Apr 1, 2008
 * ============================================================================ 
 */

package org.semispace;

public class ListenerLease implements SemiLease {

    private ListenerHolder holder;
    private SemiSpace space;

    public ListenerLease(ListenerHolder holder, SemiSpace space ) {
        this.holder = holder;
        this.space = space;
    }

    /**
     * @see org.semispace.SemiLease#cancel()
     */
    public boolean cancel() {
        return space.cancelListener( holder );
    }

    /**
     * @see org.semispace.SemiLease#renew(long)
     */
    public boolean renew(long duration) {
        return space.renewListener( holder, duration );
    }

    public long getHolderId() {
        return holder.getId();
    }

}
