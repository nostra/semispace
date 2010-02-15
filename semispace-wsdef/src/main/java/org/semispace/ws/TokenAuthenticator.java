/*
 * ============================================================================
 *
 *  File:     TokenAuthenticator.java
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
 *  Created:      Mar 16, 2008
 * ============================================================================ 
 */

package org.semispace.ws;

/**
 * Authenticator for token login.
 */
public interface TokenAuthenticator {
    /**
     * Generate a token based on the username / password. This
     * will typically be performed as query on a member 
     * database, or as a question upon the space.
     */
    public String authenticate( String username, String password);
    
    /**
     * @return True if the token is valid, false otherwise
     */
    public boolean isTokenValid( String token );
}
