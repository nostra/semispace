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
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.comet.common.CometConstants;
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
        subscribe(CometConstants.NOTIFICATION_CALL_CHANNEL+"/*", "semispaceNotify");
        this.space = space;
    }

    public void semispaceNotify(final Client remote, final Message message) {
        log.trace("Remote id "+remote.getId()+" Ch: "+message.getChannel()+" clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());

        final Map<String, Object> data = (Map<String, Object>) message.getData();
        final Long duration = Long.valueOf((String) data.get("duration"));
        final Map<String, String> searchMap = (Map<String, String>) data.get("searchMap");
        final String outChannel = message.getChannel().replace("/call/", "/reply/");
        searchMap.put("class", searchMap.remove(CometConstants.OBJECT_TYPE_KEY));
        // TODO Later consider use of createListenerType method instead of all
        SemiEventListener listener = new SemiSpaceCometListener("all", outChannel, remote, this);
        SemiEventRegistration lease = space.notify(searchMap, listener, duration.longValue());
        // TODO Consider doing something useful with the lease.
        Map<String, String> output = new HashMap<String, String>();
        if ( lease != null ) {
            output.put("leaseId", ""+lease.getId());
        } else {
            output.put("error", "Did not get lease");
        }
        remote.deliver(getClient(), message.getChannel().replace("/call/", "/reply/"), output, message.getId());

    }

    /**
     * Extract the channel listen type: all, write, take, expired
     * @return Type as it is found within the slashes. The content is checked elsewhere.
     */
    private String createListenerType(String channel) {
        String beginning = channel.substring(CometConstants.NOTIFICATION_CALL_CHANNEL.length()+1);
        int firstSlash = beginning.indexOf("/");
        if ( firstSlash == -1 ) {
            throw new RuntimeException("Problematic channel determination. Given channel was: "+channel);
        }

        return beginning.substring(0, firstSlash);
    }

    public void deliver(String outChannel, Map<String, String> output, Client remote) {
        log.debug("Delivering notification...");
        remote.deliver(getClient(), outChannel, output, null);
        log.debug("... delivery done");
    }
}
