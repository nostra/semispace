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

import org.cometd.bayeux.Channel;
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
public class TakeClient implements ReadOrTake {
    private static final Logger log = LoggerFactory.getLogger(TakeClient.class);

    private final TakeListener takeListener;
    private final int callId;

    public TakeClient(int callId) {
        this.callId = callId;
        takeListener = new TakeListener(callId);
    }

    private void attach(BayeuxClient client) {
/*      client.addListener(takeListener);
        // Documentation says I have to subscribe to this channel, but it seems like I do not have to.
        client.subscribe(CometConstants.TAKE_REPLY_CHANNEL+"/"+callId);*/

        //client.getChannel(Channel.META_SUBSCRIBE).addListener(takeListener);
        ClientSessionChannel channel = client.getChannel(CometConstants.TAKE_REPLY_CHANNEL+"/"+callId);
        if ( channel == null ) {
            throw new RuntimeException("Could not obtain channel.");
        }
        
        final CountDownLatch latch = new CountDownLatch(1);
        client.getChannel(Channel.META_HANDSHAKE).addListener(new ClientSessionChannel.MessageListener()
                {
                    @Override
                    public void onMessage(ClientSessionChannel channel, Message message)
                    {
                        latch.countDown();
                    }
                });

        client.handshake();
        try {
            if ( !latch.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Ops - no connection");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Not expected");
        }
        channel.subscribe(takeListener);

    }

    private void detach(BayeuxClient client) {
        client.getChannel(CometConstants.TAKE_REPLY_CHANNEL+"/"+callId).unsubscribe(takeListener);
    }

    public String doReadOrTake(BayeuxClient client, Map<String, Object> map, long maxWaitMs ) {
        attach(client);
        /*
        Map sm = ((Map)map.get("searchMap"));
        Set<Map.Entry<Object,Object>> es = sm.entrySet();
        for ( Map.Entry x : es) {
            log.debug(x.getKey()+"="+x.getValue()+": "+x.getValue().getClass().getName());
        }
        */
        try {
            client.waitFor(1000,BayeuxClient.State.CONNECTED);
            client.getChannel(CometConstants.TAKE_CALL_CHANNEL+"/"+callId).publish( map);
            log.trace("Awaiting..."+CometConstants.TAKE_REPLY_CHANNEL+"/"+callId+" map is: "+map);
            boolean finishedOk = takeListener.getLatch().await(maxWaitMs+ PRESUMED_NETWORK_LAG_MS, TimeUnit.MILLISECONDS);
            if ( !finishedOk) {
                log.warn("Did not receive callback on take. That is not to savory. Problem with connection?");
            }
            log.trace("... unlatched");
            return takeListener.data;
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


    private static class TakeListener implements ClientSessionChannel.MessageListener {
        private final CountDownLatch latch;
        private final int callId;
        private String data;

        public CountDownLatch getLatch() {
            return latch;
        }

        public TakeListener(int callId) {
            this.latch = new CountDownLatch(1);
            this.callId = callId;
        }

        private void deliverInternal( ClientSessionChannel channel, Message message) {
            log.debug("from.getId: "+(channel==null?"null":channel.getId())+" Ch: "+message.getChannel()+" message.clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());
            if ((CometConstants.TAKE_REPLY_CHANNEL+"/"+callId).equals(message.getChannel())) {
                Map<String,String> map = (Map) message.getData();
                if ( map != null ) {
                    data = map.get("result");
                }
                latch.countDown();
            } else {
                //log.warn("Unexpected channel "+message.getChannel()+" Expected: "+CometConstants.TAKE_REPLY_CHANNEL+"/"+callId);                
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