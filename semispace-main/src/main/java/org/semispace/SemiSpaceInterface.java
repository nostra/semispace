/*
 * ============================================================================
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
 *  Created:      Feb 25, 2008
 * ============================================================================ 
 */

package org.semispace;

/**
 * Operations possible to perform on space, inspired by the JavaSpace interface.
 */
public interface SemiSpaceInterface {
    /** 
     * Write object into tuple space, with a life time
     * given in ms.
     * @param obj Object to be written into the space
     * @param duration Life time in milliseconds of the written object
     * @return either the resulting lease, or null if an error occurred
     */
    public SemiLease write(Object obj, long duration);

    /**
     * Read an object from the space, which has matching fields (or getters)
     * with the template
     * @param template Object of exactly the same type as what 
     * is wanted as return value, with zero or more none-null fields or getters. 
     * @param duration How long you are willing to wait for an answer / match.
     * @return An object when matches the template, or null of none are found.
     */
    public <T> T read(T template, long duration);

    /**
     * Same as read, with duration 0
     * @see #read(Object, long)
     */
    public <T> T readIfExists(T template);

    /**
     * Same as read, except that the object is removed from the space.
     * @see #read(Object, long)
     */
    public <T> T take(T template, long duration);

    /**
     * Same as take, with a duration of 0
     * @see #take(Object, long)
     * @see #read(Object, long)
     */
    public <T> T takeIfExists(T template);
    
    /**
     * Register a listener for a particular template search.
     * 
     * @param template Template to be matched.
     * @param listener Listener to be notified when object with a matching template is found
     * @param duration How long this particular listener is valid.
     * @return An event registration or null
     */
    public SemiEventRegistration notify(Object template,
            SemiEventListener listener,
            long duration);

}
