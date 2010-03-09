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

import org.cometd.Client;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.cometd.client.BayeuxClient;
import org.semispace.comet.common.CometConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Taking care of the write interaction
 */
public class WriteClient {
    private static final Logger log = LoggerFactory.getLogger(WriteClient.class);

    private final WriteListener writeListener;
    private final int callId;

    public WriteClient(int  callId) {
        this.callId = callId;
        this.writeListener = new WriteListener(callId);
    }

    private void attach(BayeuxClient client) {
        client.addListener(writeListener);
        client.subscribe(CometConstants.WRITE_REPLY_CHANNEL+"/"+callId);
    }

    private void detach(BayeuxClient client) {
        client.removeListener(writeListener);
        client.unsubscribe(CometConstants.WRITE_REPLY_CHANNEL+"/"+callId);
    }

    public void doWrite(BayeuxClient client, Map<String, Object> map) {
        attach(client);

        try {
            client.publish(CometConstants.WRITE_CALL_CHANNEL+"/"+callId, map, null );
            log.trace("Awaiting...");
            // TODO Add representative timeout value 
            writeListener.getLatch().await(45, TimeUnit.SECONDS);
            log.trace("... unlatched");
        } catch (InterruptedException e) {
            log.warn("Got InterruptedException - returning null. Masked: "+e);
        } catch (Throwable t ) {
            log.error("Got an unexpected exception treating message.", t);
            throw new RuntimeException("Unexpected exception", t);
        } finally {
            detach(client);
        }
    }


    private static class WriteListener implements MessageListener {
        private final CountDownLatch latch;
        private final int callId;

        public WriteListener(int callId) {
            this.callId = callId;
            this.latch = new CountDownLatch(1);
        }

        public CountDownLatch getLatch() {
            return latch;
        }

        @Override
        public void deliver(Client from, Client to, Message message) {
            try {
                deliverInternal(from, to, message);
            } catch (Throwable t ) {
                log.error("Got an unexpected exception treating message.", t);
                throw new RuntimeException("Unexpected exception", t);
            }
        }

        private void deliverInternal(Client from, Client to, Message message) {
            if ((CometConstants.WRITE_REPLY_CHANNEL+"/"+callId).equals(message.getChannel())) {
                log.debug("Channel: "+message.getChannel()+" client id "+message.getClientId());
                latch.countDown();
            }            
        }
    }
    
}
