/*
 * ============================================================================
 *
 *  File:     TokenWsSpace.java
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

import javax.jws.WebService;

/**
 * Web services definition for token authenticated space.
 * Other than insisting that a token is supplied for each operation,
 * the space is equal to the WsSpace
 * 
 * <p>
 * Notice that the token may be a session variable, and when using
 * an invalid token, you will get errors. In that case, you need to 
 * re-authenticate and try the query again. In other words, the
 * query itself may be wrong if you get the same erroneous result 
 * twice - once before re-authenticating, once after.
 * </p>
 * @see WsSpace
 */
@WebService
public interface TokenWsSpace {
    /**
     * Login the user, and return a token if authenticated.
     */
    public String login( String username, String password);
    public void write( String token, String contents, long timeToLiveMs );
    public String read( String token, String template, long queryLifeMs );
    public String readIfExists( String token, String template );
    public String take( String token, String template, long queryLifeMs );
    public String takeIfExists( String token, String template );

}
