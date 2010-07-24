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
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
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

    private NotificationRegistrationListener notificationListener;
    private final int callId;
    private SemiEventListener listener;

    public NotificationClient(int callId, SemiEventListener listener) {
        this.callId = callId;
        this.listener = listener;
    }

    private void attach(BayeuxClient client) {
        notificationListener = new NotificationRegistrationListener(callId, client, listener );
        /*client.addListener(notificationListener);
        client.subscribe(CometConstants.NOTIFICATION_REPLY_CHANNEL+"/"+callId+"/all");*/
        client.getChannel(Channel.META_SUBSCRIBE).addListener(notificationListener);
        client.getChannel(CometConstants.NOTIFICATION_REPLY_CHANNEL+"/"+callId+"/all").subscribe(notificationListener);
        /*
        client.getChannel(Channel.META_SUBSCRIBE).addListener(writeListener);
        client.getChannel(CometConstants.WRITE_REPLY_CHANNEL+"/"+callId).subscribe(writeListener);
         */
    }

    private void detach(BayeuxClient client) {
        client.getChannel(CometConstants.NOTIFICATION_REPLY_CHANNEL+"/"+callId+"/all").unsubscribe(notificationListener);

        notificationListener = null;
    }

    public SemiEventRegistration doNotify(BayeuxClient client, Map<String, Object> map) {
        attach(client);

        try {
            client.getChannel(CometConstants.NOTIFICATION_CALL_CHANNEL+"/"+callId+"/"+ CometConstants.EVENT_ALL).publish( map);
            log.debug("Awaiting..."+CometConstants.NOTIFICATION_REPLY_CHANNEL+"/"+callId+"/all map is: "+map);
            boolean finishedOk = notificationListener.getLatch().await(5, TimeUnit.SECONDS);
            log.trace("... unlatched");
            if ( !finishedOk) {
                log.warn("Did not receive callback on notify. That is not to savory. Problem with connection? Returning null for lease");
                return null;
            }
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


    private static class NotificationRegistrationListener implements ClientSessionChannel.MessageListener {
        private final CountDownLatch latch;
        private final int callId;
        private SemiEventRegistration data;
        private BayeuxClient client;
        private SemiEventListener listener;

        public CountDownLatch getLatch() {
            return latch;
        }

        private NotificationRegistrationListener(int callId, BayeuxClient client, SemiEventListener listener) {
            this.latch = new CountDownLatch(1);
            this.callId = callId;
            this.client = client;
            this.listener = listener;
        }

        private void deliverInternal( ClientSessionChannel to, Message message) {
            if ((CometConstants.NOTIFICATION_REPLY_CHANNEL+"/"+callId+"/"+CometConstants.EVENT_ALL).equals(message.getChannel())) {
                log.trace("Notify - Ch: "+message.getChannel()+" message.clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());
                Map map = (Map) message.getData();
                if ( map != null ) {
                    // Timeout value is a roundtrip parameter
                    String timeOutInMs = (String) map.get("duration");
                    NotificationMitigator mitigator = new NotificationMitigator(client, callId, listener, Long.valueOf(timeOutInMs));
                    SemiEventRegistration registration = new SemiEventRegistration(Long.valueOf((String) map.get("leaseId")).longValue(), mitigator );
                    mitigator.attach();
                    data = registration;
                }
                latch.countDown();
            } else {
                // TODO log.warn("Unexpected channel "+message.getChannel());
            }
        }
        public SemiEventRegistration getData() {
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
