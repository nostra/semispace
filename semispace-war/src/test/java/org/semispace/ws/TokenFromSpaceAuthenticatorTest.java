/*
 * ============================================================================
 *
 *  File:     TokenFromSpaceAuthentcatorTest.java
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

import org.semispace.NameValueQuery;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.ws.TokenFromSpaceAuthenticator;

import junit.framework.TestCase;

public class TokenFromSpaceAuthenticatorTest extends TestCase {
    private SemiSpaceInterface space; 
    private TokenFromSpaceAuthenticator tfsa; 
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        space = SemiSpace.retrieveSpace();
        tfsa = new TokenFromSpaceAuthenticator();
        tfsa.setSpace(space);
    }

    
    public void testAuthenticate() {
        NameValueQuery someUser = new NameValueQuery();
        someUser.name = "authname=junit";
        someUser.value = "test";
        space.write(someUser, SemiSpace.ONE_DAY);
        assertNull(tfsa.authenticate("junit", someUser.value+ " INVALID"));
        
        String token = tfsa.authenticate("junit", someUser.value);
        assertNotNull(token);
        assertFalse(tfsa.isTokenValid(token+" INVALID"));
        assertTrue(tfsa.isTokenValid(token));
    }
}
