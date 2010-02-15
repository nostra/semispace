/*
 * ============================================================================
 *
 *  File:     SemiLease.java
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
 *  Created:      Mar 31, 2008
 * ============================================================================ 
 */

package org.semispace;

/**
 * Lease which can be used to either cancel or renew the
 * held object, if possible. The lease is intended to be 
 * used on the same VM only, and can therefore <b>not</b>
 * be transferred anywhere.
 */
public interface SemiLease {
    /**
     * Return true if operation was a success
     */
    public boolean cancel();
    /**
     * Return true if operation was a success
     */
    public boolean renew(long duration);
    /**
     * @return holder id for the lease
     */
    public long getHolderId();
}
