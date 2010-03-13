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
import org.semispace.SemiSpace;
import org.semispace.comet.common.CometConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Supporting semispace take.
 */
public class TakeService extends BayeuxService {
    private static final Logger log = LoggerFactory.getLogger(TakeService.class);
    private final SemiSpace space;

    public TakeService(Bayeux bayeux, SemiSpace space ) {
        super(bayeux, "take");
        subscribe(CometConstants.TAKE_CALL_CHANNEL+"/*", "semispaceTake");
        this.space = space;
    }

    public void semispaceTake(Client remote, Message message) {
        log.trace("Remote id "+remote.getId()+" Ch: "+message.getChannel()+" clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());

        final Map<String, Object> data = (Map<String, Object>) message.getData();
        final Long duration = Long.valueOf((String) data.get("duration"));
        final boolean shallTake = true;
        final Map<String, String> searchMap = (Map<String, String>) data.get("searchMap");
        searchMap.put("class", searchMap.remove(CometConstants.OBJECT_TYPE_KEY));

        String result = space.findOrWaitLeaseForTemplate(searchMap, duration.longValue(), shallTake);
        // log.debug("Did "+(result == null?"NOT":"")+" get a result: "+result);

        Map<String, String> output = new HashMap<String, String>();
        if ( result != null ) {
            output.put("result", result);
            log.trace("take ended up with a result");
        } else {
            log.trace("take did not get a result");
        }
        remote.deliver(getClient(), message.getChannel().replace("/call/", "/reply/"), output, message.getId());
    }
}