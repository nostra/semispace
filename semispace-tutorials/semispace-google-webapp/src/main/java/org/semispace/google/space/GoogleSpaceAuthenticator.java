/*
 * ============================================================================
 *
 *  File:     GoogleSpaceAuthenticator.java
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
 *  Created:      Oct 2, 2008
 * ============================================================================ 
 */

package org.semispace.google.space;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.SemiSpaceInterface;
import org.semispace.google.webapp.beans.Token;
import org.semispace.google.webapp.beans.UserAuthBean;
import org.semispace.ws.TokenAuthenticator;

public class GoogleSpaceAuthenticator implements TokenAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(GoogleSpaceAuthenticator.class);
    
    private SemiSpaceInterface space;
    
    public void setSpace(SemiSpaceInterface space) {
        this.space = space;
    }

    private final Random wheel = new Random();
    
    /**
     * Default session time of 30 minutes.
     */
    private static final long DEFAULT_SESSION_LENGTH = 1000*60*30;
    
    public String authenticate(String username, String password) {
        UserAuthBean auth = new UserAuthBean();
        auth.setUsername(username);
        auth.setPassword(password);
        UserAuthBean result = (UserAuthBean) space.readIfExists(auth);
        if ( result == null ) {
            log.debug("User with user name "+username+" is not registered as user in space.");
        } else {
            Token tokenQuery = new Token();
            tokenQuery.setUsername(username);
            // Remove potentially existing tokens
            while( space.takeIfExists(tokenQuery) != null ) {
                log.debug("Removed existing token for user "+username);
            }
            String token = generateToken();
            tokenQuery.setToken(token);
            space.write(tokenQuery, DEFAULT_SESSION_LENGTH);
            return token;
        }
        
        return null;
    }

    /**
     * Generate a random token.
     */
    private String generateToken() {
        String rnd = Long.toString( wheel.nextLong() & Long.MAX_VALUE , 36 );
        if ( rnd.length() > 7 ) {
            rnd = rnd.substring(0,7);
        }
        return rnd;
    }
    
    public boolean isTokenValid(String token) {
        Token tokenQuery = new Token();
        tokenQuery.setToken(token);
        tokenQuery = (Token) space.take(tokenQuery, 200);
        if ( tokenQuery != null ) {
            // This is in order to renew session
            space.write(tokenQuery, DEFAULT_SESSION_LENGTH);
            return true;
        }
        return false;
    }

}
