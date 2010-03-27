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
 * Notification methods
 */
public class NotificationClient {
    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final NotificationListener notificationListener;
    private final int callId;

    public NotificationClient(int callId) {
        this.callId = callId;
        notificationListener = new NotificationListener(callId);
    }

    private void attach(BayeuxClient client) {
        client.addListener(notificationListener);
        client.subscribe(CometConstants.NOTIFICATION_CALL_CHANNEL+"/"+callId);
    }

    private void detach(BayeuxClient client) {
        client.removeListener(notificationListener);
        client.unsubscribe(CometConstants.NOTIFICATION_REPLY_CHANNEL+"/"+callId);
    }

    public String doNotify(BayeuxClient client, Map<String, Object> map, long maxWaitMs ) {
        attach(client);

        try {
            client.publish(CometConstants.NOTIFICATION_CALL_CHANNEL+"/"+callId, map, null );
            log.debug("Awaiting..."+CometConstants.NOTIFICATION_REPLY_CHANNEL+"/"+callId+" map is: "+map);
            // TODO Change timeout if necessary
            boolean finishedOk = notificationListener.getLatch().await(5, TimeUnit.SECONDS);
            if ( !finishedOk) {
                log.warn("Did not receive callback on notify. That is not to savory. Problem with connection?");
            }
            log.trace("... unlatched");
            return notificationListener.data;
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


    private static class NotificationListener implements MessageListener {
        private final CountDownLatch latch;
        private final int callId;
        private String data;

        public CountDownLatch getLatch() {
            return latch;
        }

        private NotificationListener(int callId) {
            this.latch = new CountDownLatch(1);
            this.callId = callId;
        }
        @Override
        public void deliver(Client from, Client to, Message message) {
            try {
                deliverInternal(to, message);
            } catch (Throwable t ) {
                log.error("Got an unexpected exception treating message.", t);
                throw new RuntimeException("Unexpected exception", t);
            }
        }

        private void deliverInternal( Client to, Message message) {
            if ((CometConstants.NOTIFICATION_REPLY_CHANNEL+"/"+callId).equals(message.getChannel())) {
                log.debug("Notify - Ch: "+message.getChannel()+" message.clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());
                Map map = (Map) message.getData();
                if ( map != null ) {
                    // TODO Not using data yet
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
