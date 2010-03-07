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
import org.semispace.NameValueQuery;
import org.semispace.SemiSpace;
import org.semispace.comet.common.CometConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Supporting semispace read.
 */
public class ReadService extends BayeuxService {
    private static final Logger log = LoggerFactory.getLogger(ReadService.class);
    private final SemiSpace space;
    
    public ReadService(Bayeux bayeux, SemiSpace space ) {
        super(bayeux, "read");
        subscribe(CometConstants.READ_CALL_CHANNEL, "semispaceRead");
        this.space = space;
    }

    public void semispaceRead(Client remote, Message message) {
        log.debug("Remote id "+remote.getId()+" Ch: "+message.getChannel()+" clientId: "+message.getClientId()+" id: "+message.getId()+" data: "+message.getData());

        NameValueQuery dummy = new NameValueQuery();
        dummy.name = "dummyName";
        dummy.value= "dummyValue";                        
        space.write(dummy, 1000);

        final Map<String, Object> data = (Map<String, Object>) message.getData();
        final Map<String, String> searchMap = (Map<String, String>) data.get("searchMap");
        final Long duration = (Long) data.get("duration");
        String result = space.findOrWaitLeaseForTemplate(searchMap, duration.longValue(), false);

        Map<String, String> output = new HashMap<String, String>();
        if ( result != null ) {
            output.put("result", result);
        }
        remote.deliver(getClient(), CometConstants.READ_REPLY_CHANNEL, output, message.getId());
    }
}
