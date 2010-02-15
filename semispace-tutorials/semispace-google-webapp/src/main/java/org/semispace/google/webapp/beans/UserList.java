/*
 * ============================================================================
 *
 *  File:     UserList.java
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
 *  Created:      Jan 4, 2009
 * ============================================================================ 
 */

package org.semispace.google.webapp.beans;

import java.util.Set;

/**
 * Rather silly class which retains the user names in an array. 
 * This is just because I do not bother to create a user database
 * in the regular manner.
 */
public class UserList {
    private Set<String>users;
   
    public Set<String> getUsers() {
        return this.users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }
    
}
