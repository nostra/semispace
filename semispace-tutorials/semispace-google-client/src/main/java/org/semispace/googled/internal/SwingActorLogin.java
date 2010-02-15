/*
 * ============================================================================
 *
 *  File:     SwingActorLogin.java
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
 *  Created:      21. des.. 2008
 * ============================================================================ 
 */

package org.semispace.googled.internal;

import java.awt.CardLayout;

import javax.swing.AbstractAction;
import javax.swing.JPanel;

import org.semispace.SemiSpace;
import org.semispace.actor.Actor;
import org.semispace.actor.ActorMessage;
import org.semispace.actor.SwingActor;
import org.semispace.googled.bean.LoginMessage;
import org.semispace.googled.bean.LoginResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SwingActor
public class SwingActorLogin extends Actor {
    private static final Logger log = LoggerFactory.getLogger(SwingActorLogin.class);
    private AbstractAction callback;
    private JPanel cardPanel;
    private boolean disabled = false;
    
    public SwingActorLogin(AbstractAction callback, JPanel bottom) {
        this.callback = callback;
        this.cardPanel = bottom;
        register(SemiSpace.retrieveSpace());
    }

    @Override
    public void receive(ActorMessage msg) {
        if ( msg.isOfType(LoginResult.class) && ! disabled) {
            callback.setEnabled(true);
            changeToEitherSearchOrLoginCard((LoginResult) msg.getPayload());
            
        } else {
            log.warn("Got message of unexpected type (or login is disabled): "+msg.getPayload());
        }

    }

    @Override
    public Object[] getTakeTemplates() {
        return new Object[]{new LoginResult()};
    }
    
    public void performLogin(String username, String password) {
        callback.setEnabled(false);
        LoginMessage msg = new LoginMessage();
        msg.setUsername(username);
        msg.setPassword(password);
        log.debug("Sending login message");
        send(msg);
    }

    private void changeToEitherSearchOrLoginCard(LoginResult logInMessage) {
        CardLayout cards = (CardLayout) ( cardPanel.getLayout() );
        if (logInMessage.getDidGetToken().booleanValue()) {
            log.debug("Did get token, and shall therefore show search panel.");
            cards.show(cardPanel, "searchPanel");
        } else {
            log.debug("Did NOT get token, and shall therefore show login panel.");
            cards.show(cardPanel, "loginPanel");
        }
    }

}
