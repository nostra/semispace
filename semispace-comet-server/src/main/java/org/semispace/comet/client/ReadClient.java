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
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.client.HttpClient;
import org.semispace.comet.client.SemiSpaceBayeuxClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

/**
 * Supporting read from SemiSpace
 */
public class ReadClient {
    private static final Logger log = LoggerFactory.getLogger(ReadClient.class);
    public static final String CALL_CHANNEL = "/semispace/call/read";
    public static final String REPLY_CHANNEL = "/semispace/reply/read";

    private final ReadListener readListener = new ReadListener();

    private void attach(BayeuxClient client) {
        client.addListener(readListener);
        client.subscribe(REPLY_CHANNEL);
        client.subscribe(Bayeux.META_HANDSHAKE);
    }

    private void detach(BayeuxClient client) {
        client.removeListener(readListener);
        client.unsubscribe(REPLY_CHANNEL);
    }

    public void doRead(BayeuxClient client, Map<String, Object> map) {
        attach(client);
        client.publish(ReadClient.CALL_CHANNEL, map, ""+map.get("id") );
        
        try {
            log.debug("Awaiting...");
            readListener.getLatch().await();
            log.debug("... unlatched");
        } catch (InterruptedException e) {
            log.warn("Got InterruptedException. Masked: "+e);
        }

        detach(client);
    }


    private static class ReadListener implements MessageListener {
        private final CountDownLatch latch;

        public CountDownLatch getLatch() {
            return latch;
        }

        public ReadListener() {
            this.latch = new CountDownLatch(1);
        }
        public void deliver(Client from, Client to, Message message) {
            log.debug("Ch: "+message.getChannel()+" clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());
            if (REPLY_CHANNEL.equals(message.getChannel())) {
                // Here we received a message on the channel
                log.info("Channel is correct: "+message.getChannel()+" client id "+message.getClientId());
                latch.countDown();
            }
        }
    }

}
