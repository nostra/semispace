/*
 * ============================================================================
 *
 *  File:     TokenWsSpaceImpl.java
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.SemiSpace;

/**
 * All methods will throw RuntimeException if the token is not found to be OK. Otherwise, the operation in question is
 * just relayed to WsSpaceImpl.
 * <p>
 * You need to set both the space and the authenticator.
 * </p>
 * 
 * @see WsSpaceImpl
 */
@WebService(endpointInterface = "org.semispace.ws.TokenWsSpace")
public class TokenWsSpaceImpl implements TokenWsSpace {
    private static final Logger log = LoggerFactory.getLogger(TokenWsSpaceImpl.class);

    private WsSpaceImpl wsspace;

    private TokenAuthenticator auth;

    public TokenWsSpaceImpl() {
        wsspace = new WsSpaceImpl();
    }

    public void setTokenAuthenticator(TokenAuthenticator auth) {
        this.auth = auth;
    }

    /** For the benefit of spring */
    public void setSpace(SemiSpace space) {
        wsspace.setSpace(space);
    }

    public String login(String username, String password) {
        makeSureAuthIsPresent();
        return auth.authenticate(username, password);
    }

    private void makeSureAuthIsPresent() {
        if (auth == null) {
            String error = "Erroneous initizalization - need TokenAuthenticator.";
            log.error(error);
            throw new RuntimeException(error);
        }
    }

    public String read(String token, String template, long queryLifeMs) {
        makeSureAuthIsOk(token);
        return wsspace.read(template, queryLifeMs);
    }

    private void makeSureAuthIsOk(String token) {
        makeSureAuthIsPresent();
        if (!auth.isTokenValid(token)) {
            log.warn("Space tried used with token which is not OK. Token: " + token);
            throw new RuntimeException("Token incorrect");
        }
    }

    public String readIfExists(String token, String template) {
        makeSureAuthIsOk(token);
        return wsspace.readIfExists(template);
    }

    public String take(String token, String template, long queryLifeMs) {
        makeSureAuthIsOk(token);
        return wsspace.take( template,queryLifeMs);
    }

    public String takeIfExists(String token, String template) {
        makeSureAuthIsOk(token);
        return wsspace.takeIfExists(template);
    }

    public void write(String token, String contents, long timeToLiveMs) {
        makeSureAuthIsOk(token);
        wsspace.write(contents, timeToLiveMs);
    }

}
