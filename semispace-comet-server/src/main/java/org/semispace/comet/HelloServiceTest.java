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

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.ClientListener;
import org.cometd.Message;
import org.cometd.MessageListener;
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.client.HttpClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.semispace.comet.client.SemiSpaceBayeuxClient;
import org.semispace.comet.client.HelloMessageListener;
import org.semispace.comet.client.ReadClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class HelloServiceTest {
    private static final Logger log = LoggerFactory.getLogger(HelloServiceTest.class);
    private SemiSpaceBayeuxClient helloHttp;
    private BayeuxClient client;
    private ClientListener helloListener;
    public static final String CHANNEL = "/service/hello";
    private int id = 0;


    @Before
    public void setUp() throws Exception {
               HttpClient httpClient = new HttpClient();
        // Here setup Jetty's HttpClient, for example:
        // httpClient.setMaxConnectionsPerAddress(2);
        helloListener = new HelloMessageListener();

        httpClient.start();
        client = new BayeuxClient(httpClient, "http://localhost:8080/semispace-comet-server/cometd/" );

        client.addListener(new MessageListener() {
            public void deliver(Client from, Client to, Message message)
            {
                if (Bayeux.META_HANDSHAKE.equals(message.getChannel()))
                {
                    Boolean successful = (Boolean) message.get(Bayeux.SUCCESSFUL_FIELD);
                    if (successful != null && successful) {
                        log.debug("Handshake successful");
                    } else {
                        log.debug("Handshake NOT OK");
                    }
                }
            }
        });


        client.addListener( helloListener );
        log.debug("Now starting client");
        client.start();
        client.subscribe(CHANNEL);
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(10000);

        client.unsubscribe(CHANNEL);
        client.removeListener(helloListener);
        client.stop();
        if ( helloHttp != null ) {
            helloHttp.stop();
        }
    }

    @Test
    public void testProcessHello() throws Exception {
        id++;
        Map map = new HashMap();
        map.put("name", "Zzzzzzzzzzz");
        map.put("something", "Inconsequential");
        client.publish(ReadClient.CALL_CHANNEL, map, ""+id );
        log.debug("Published successfully");
    }
}
