/*
 * ============================================================================
 *
 *  File:     SemiEvent.java
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
 *  Created:      Apr 8, 2008
 * ============================================================================ 
 */

package org.semispace.event;

/**
 * An event on an element in the semispace. The event as such is 
 * distributed through notify (by terracotta).
 */
public interface SemiEvent {
    /**
     * The id of the object in the system
     */
    public long getId();
}
