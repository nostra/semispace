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

package org.semispace.comet.server;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.Message;
import org.cometd.server.BayeuxService;
import org.semispace.Holder;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.comet.common.CometConstants;
import org.semispace.comet.common.Json2Xml;
import org.semispace.comet.common.XmlManipulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class NotificationService extends BayeuxService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final SemiSpace space;

    public NotificationService(Bayeux bayeux, SemiSpace space ) {
        super(bayeux, "notification");
        subscribe(CometConstants.NOTIFICATION_CALL_CHANNEL+"/**", "semispaceNotify");
        this.space = space;
    }

    public void semispaceNotify(final Client remote, final Message message) {
        log.trace("Remote id "+remote.getId()+" Ch: "+message.getChannel()+" clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());

        final Map<String, Object> data = (Map<String, Object>) message.getData();
        final Long duration = Long.valueOf((String) data.get("duration"));
        final String json = (String) data.get(CometConstants.PAYLOAD_MARKER); // TODO Change name to payload
        final String xml = Json2Xml.transform(json);
        final Holder holder = XmlManipulation.retrievePropertiesFromXml(xml, duration);

        final String outChannel = message.getChannel().replace("/call/", "/reply/");

        // TODO Move method into listener.
        String listenerType = createListenerType(outChannel);
        String callId = createCallId( outChannel, listenerType);
        log.trace("------- Constructed type: "+listenerType+", callId: "+callId+" out of "+outChannel);

        SemiSpaceCometListener listener = new SemiSpaceCometListener(listenerType, callId, remote, this);
        SemiEventRegistration lease = space.notify(holder.getSearchMap(), listener, duration.longValue());
        Map<String, String> output = new HashMap<String, String>();
        output.put( "duration", ""+duration);
        if ( lease != null ) {
            output.put("leaseId", ""+lease.getId());
            // TODO Create surveillance of listener in order to remove it if it is expired
            LeaseCancellationService.registerCancelableLease( callId, lease.getLease(), message.getClientId());
        } else {
            output.put("error", "Did not get lease");
        }
        remote.deliver(getClient(), message.getChannel().replace("/call/", "/reply/"), output, message.getId());

    }

    private String createCallId(String outChannel, String listenerType) {
        String basis = outChannel.substring(0, outChannel.length()-listenerType.length()-1);

        return basis.substring(basis.lastIndexOf("/")+1);
    }

    /**
     * Extract the channel listen type: all, write, take, expired
     * @return Type as it is found within the slashes. The content is checked elsewhere.
     */
    private String createListenerType(String channel) {
        int lastSlash = channel.lastIndexOf("/");
        if ( lastSlash == -1 ) {
            throw new RuntimeException("Problematic channel determination. Given channel was: "+channel);
        }

        String type = channel.substring(lastSlash+1);
        return type;
    }

    public void deliver(String outChannel, Map<String, String> output, Client remote) {
        log.trace("Delivering notification...");
        try {
            remote.deliver(getClient(), outChannel, output, null);
        } catch (Throwable t ) {
            log.error("Could not deliver message to client.", t);
        }
        log.trace("... delivery done");
    }
}
