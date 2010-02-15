/*
 * ============================================================================
 *
 *  File:     TokenFromSpaceAuthenticator.java
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

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.NameValueQuery;
import org.semispace.SemiSpaceInterface;

/**
 * Query on simple name / value elements read from 
 * space. This implementation exist as an <b>example</b>
 * and for the benefit of junit tests. You will want
 * to implement your own version in a live situation.
 * 
 * <p>See method doc for more information.</p>
 * 
 * @see NameValueQuery
 */
public class TokenFromSpaceAuthenticator implements TokenAuthenticator {
    private static final Logger log = LoggerFactory.getLogger(TokenFromSpaceAuthenticator.class);
    /**
     * Default session time of 30 minutes.
     */
    private static final long DEFAULT_SESSION_LENGTH = 1000*60*30;
    private SemiSpaceInterface space;
    
    /** For the benefit of spring */
    public void setSpace(SemiSpaceInterface space) {
        this.space = space;
    }

    /**
     * As we use NameValueQuery from the semispace-main, we 
     * prepend the name field with <code>authname=</code> in order to 
     * avoid name clashes. 
     * @see org.semispace.ws.TokenAuthenticator#authenticate(java.lang.String, java.lang.String)
     */
    public String authenticate(String username, String password) {
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "authname="+username;
        nvq.value = password;
        NameValueQuery result = (NameValueQuery) space.readIfExists(nvq);
        if ( result != null ) {
            // User exist in space
            NameValueQuery tokenQuery = new NameValueQuery();
            tokenQuery.value = "tokenFor="+username;
            // Remove potentially existing tokens
            for ( NameValueQuery existing = tokenQuery ; existing != null ; existing = (NameValueQuery) space.takeIfExists(tokenQuery) ) {
                // Intentional
            }
            String token = generateToken();
            tokenQuery.name = "token="+token;
            space.write(tokenQuery, DEFAULT_SESSION_LENGTH);
            return token;
        }
        
        return null;
    }

    /**
     * Generate a random token.
     */
    private String generateToken() {
        final Random wheel = new Random();
        String rnd = Long.toString( wheel.nextLong() & Long.MAX_VALUE , 36 );
        if ( rnd.length() > 7 ) {
            rnd = rnd.substring(0,7);
        }
        return rnd;
    }

    /**
     * Side effect: The token life was renewed
     * @return If the token was found to be valid
     * @see org.semispace.ws.TokenAuthenticator#isTokenValid(java.lang.String)
     */
    public boolean isTokenValid(String token) {
        NameValueQuery tokenQuery = new NameValueQuery();
        tokenQuery.name = "token="+token;
        NameValueQuery spaceToken = (NameValueQuery) space.take(tokenQuery, 200);
        if ( spaceToken != null ) {
            // This is in order to renew session
            space.write(spaceToken, DEFAULT_SESSION_LENGTH);
            return true;
        }
        return false;
    }

}
