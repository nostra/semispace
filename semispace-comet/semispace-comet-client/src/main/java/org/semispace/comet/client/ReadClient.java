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

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
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
public class ReadClient implements ReadOrTake {
    private static final Logger log = LoggerFactory.getLogger(ReadClient.class);

    private final ReadListener readListener;
    private final int callId;

    public ReadClient(int callId) {
        this.callId = callId;
        readListener = new ReadListener(callId);
    }

    private void attach(BayeuxClient client) {
        client.getChannel(CometConstants.READ_REPLY_CHANNEL+"/"+callId).subscribe(readListener);

    }

    private void detach(BayeuxClient client) {
        client.getChannel(CometConstants.TAKE_REPLY_CHANNEL+"/"+callId).unsubscribe(readListener);
    }

    @Override
    public String doReadOrTake(BayeuxClient client, Map<String, Object> map, long maxWaitMs ) {
        attach(client);

        try {
            client.getChannel(CometConstants.READ_CALL_CHANNEL+"/"+callId).publish( map);
            log.debug("Awaiting..."+CometConstants.READ_REPLY_CHANNEL+"/"+callId+" map is: "+map);
            boolean finishedOk = readListener.getLatch().await(maxWaitMs+PRESUMED_NETWORK_LAG_MS, TimeUnit.MILLISECONDS);
            if ( !finishedOk) {
                log.warn("Did not receive callback on read. That is not to savory. Problem with connection?");
            }
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


    private static class ReadListener implements ClientSessionChannel.MessageListener {
        private final CountDownLatch latch;
        private final int callId;
        private String data;

        public CountDownLatch getLatch() {
            return latch;
        }

        private ReadListener(int callId) {
            this.latch = new CountDownLatch(1);
            this.callId = callId;
        }
        
        private void deliverInternal( ClientSessionChannel to, Message message) {
            if ((CometConstants.READ_REPLY_CHANNEL+"/"+callId).equals(message.getChannel())) {
                //log.debug("from.getId: "+(from==null?"null":from.getId())+" Ch: "+message.getChannel()+" message.clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());
                Map<String,String> map = (Map) message.getData();
                if ( map != null ) {
                    data = map.get("result");
                }
                latch.countDown();
            } else {
                //log.warn("Unexpected channel "+message.getChannel()+" Expected: "+CometConstants.READ_REPLY_CHANNEL+"/"+callId);
            }
        }
        public Object getData() {
            return data;
        }

        @Override
        public void onMessage(ClientSessionChannel channel, Message message) {
            try {
                deliverInternal(channel, message);
            } catch (Throwable t ) {
                log.error("Got an unexpected exception treating message.", t);
                throw new RuntimeException("Unexpected exception", t);
            }
        }
    }

}
