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
import org.semispace.SemiLease;
import org.semispace.SemiSpace;
import org.semispace.comet.common.CometConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Supporting semispace read.
 */
public class WriteService extends BayeuxService {
    private static final Logger log = LoggerFactory.getLogger(WriteService.class);
    private final SemiSpace space;

    public WriteService(Bayeux bayeux, SemiSpace space ) {
        super(bayeux, "write");
        subscribe(CometConstants.WRITE_CALL_CHANNEL+"/*", "semispaceWrite");
        this.space = space;
    }

    public void semispaceWrite(Client remote, Message message) {
        final Map<String, Object> data = (Map<String, Object>) message.getData();
        final Map<String, String> searchMap = (Map<String, String>) data.get("searchMap");
        final Long timeToLiveMs = (Long) data.get("timeToLiveMs");
        final String xml = (String) data.get("xml");
        final String className = (String) data.get("classname");
        log.debug("Remote id "+remote.getId()+" Ch: "+message.getChannel()+" clientId: "+message.getClientId()+" id: "+message.getId()+" classname "+className);

        SemiLease lease = space.writeToElements(className,timeToLiveMs, xml, searchMap);

        Map<String, String> output = new HashMap<String, String>();
        if ( lease != null ) {
            output.put("holderId", ""+lease.getHolderId());
        } else {
            output.put("error", "Did not get lease");
        }
        remote.deliver(getClient(), message.getChannel().replace("/call/", "/reply/"), output, message.getId());
    }
}