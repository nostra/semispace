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
 * Supporting read from SemiSpace
 */
public class ReadOrTakeClient {
    private static final Logger log = LoggerFactory.getLogger(ReadOrTakeClient.class);

    private final ReadListener readListener;
    private final int callId;

    public ReadOrTakeClient(int callId) {
        this.callId = callId;
        readListener = new ReadListener(callId);
    }

    private void attach(BayeuxClient client) {
        client.addListener(readListener);
        // Documentation says I have to subscribe to this channel, but it seems like I do not have to.
        client.subscribe(CometConstants.READ_REPLY_CHANNEL+"/"+callId);
    }

    private void detach(BayeuxClient client) {
        client.removeListener(readListener);
        client.unsubscribe(CometConstants.READ_REPLY_CHANNEL+"/"+callId);
    }

    public String doRead(BayeuxClient client, Map<String, Object> map) {
        attach(client);

        try {
            client.publish(CometConstants.READ_CALL_CHANNEL+"/"+callId, map, null );
            log.debug("Awaiting..."+CometConstants.READ_CALL_CHANNEL+"/"+callId+" map is: "+map);
            // TODO Add representative timeout value - 10, TimeUnit.SECONDS
            readListener.getLatch().await(45, TimeUnit.SECONDS);
            log.trace("... unlatched");
            return readListener.data;
        } catch (InterruptedException e) {
            log.warn("Got InterruptedException - returning null. Masked: "+e);
            return null;
        } catch (Throwable t ) {
            log.error("Got an unexpected exception treating message.", t);
            throw new RuntimeException("Unexpected exception", t);
        } finally {
            detach(client);
        }
    }


    private static class ReadListener implements MessageListener {
        private final CountDownLatch latch;
        private final int callId;
        private String data;

        public CountDownLatch getLatch() {
            return latch;
        }

        public ReadListener(int callId) {
            this.latch = new CountDownLatch(1);
            this.callId = callId;
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

        private void deliverInternal( Client from, Client to, Message message) {
            if ((CometConstants.READ_REPLY_CHANNEL+"/"+callId).equals(message.getChannel())) {
                //log.debug("from.getId: "+(from==null?"null":from.getId())+" Ch: "+message.getChannel()+" message.clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());
                Map map = (Map) message.getData();
                if ( map != null ) {
                    data = (String) map.get("result");
                }
                latch.countDown();
            }
        }
        public Object getData() {
            return data;
        }
    }

}
