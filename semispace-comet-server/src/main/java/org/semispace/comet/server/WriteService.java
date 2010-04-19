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
import org.semispace.SemiLease;
import org.semispace.SemiSpace;
import org.semispace.comet.common.CometConstants;
import org.semispace.comet.common.Json2Xml;
import org.semispace.comet.common.XmlManipulation;
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

        final Long timeToLiveMs = Long.valueOf(""+data.get("timeToLiveMs")); // TODO Change to duration
        final String json = (String) data.get(CometConstants.PAYLOAD_MARKER); // TODO Change name to payload
        final String xml = Json2Xml.transform(json);
        Holder holder = XmlManipulation.retrievePropertiesFromXml(xml, timeToLiveMs);
        holder.getSearchMap().put("class", holder.getSearchMap().get(CometConstants.OBJECT_TYPE_KEY));
                        
        log.trace("Remote id "+remote.getId()+" Ch: "+message.getChannel()+" clientId: "+message.getClientId()+" id: "+message.getId()+" class "+holder.getClassName()+" xml:\n"+xml);
        log.trace("Holder searchmap: "+holder.getSearchMap());
        log.trace("Parameter searchmap: "+data.get("searchMap"));


        // Not putting this operation into separate thread, as it is expected to perform reasonably quickly
        SemiLease lease = space.writeToElements(holder.getClassName(), timeToLiveMs.longValue(), xml, holder.getSearchMap());

        Map<String, String> output = new HashMap<String, String>();
        if ( lease != null ) {
            output.put("holderId", ""+lease.getHolderId());
        } else {
            output.put("error", "Did not get lease");
        }
        remote.deliver(getClient(), message.getChannel().replace("/call/", "/reply/"), output, message.getId());
    }
}