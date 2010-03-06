/*
 * Copyright 2010 Erlend Nossum
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
 */

package org.semispace.comet.client;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.semispace.comet.HelloServiceTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloMessageListener implements MessageListener {
    private static final Logger log = LoggerFactory.getLogger(HelloMessageListener.class);

    public void deliver(Client from, Client to, Message message) {
        log.debug("Ch: "+message.getChannel()+" clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());
        if (Bayeux.META_HANDSHAKE.equals(message.getChannel()) ) {
            log.debug("Handshake");
        } else if (Bayeux.META_CONNECT.equals(message.getChannel()) ) {
            log.debug("Connect");
        } else if (HelloServiceTest.CHANNEL.equals(message.getChannel()) ){
            log.debug(" ==== data: "+message.getData());
        } else {
            log.debug("Not categorized");
        }
    }

}
