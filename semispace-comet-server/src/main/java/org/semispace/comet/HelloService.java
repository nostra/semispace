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

package org.semispace.comet;

import java.util.Map;
import java.util.HashMap;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.Message;
import org.cometd.server.BayeuxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloService extends BayeuxService {
    private static final Logger log = LoggerFactory.getLogger(HelloService.class);
    public HelloService(Bayeux bayeux) {
        super(bayeux, "hello");
        subscribe("/service/hello", "processHello");
    }

    public void processHello(Client remote, Message message)
    {
        Map<String, Object> input = (Map<String, Object>)message.getData();
        String name = (String)input.get("name");
        log.debug("Processhello "+input);
        Map<String, Object> output = new HashMap<String, Object>();
        output.put("greeting", "Hello, " + name);
        remote.deliver(getClient(), "/hello", output, null);
    }
}
