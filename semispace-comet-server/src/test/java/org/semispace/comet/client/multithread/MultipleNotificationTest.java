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

package org.semispace.comet.client.multithread;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semispace.comet.client.SemiSpaceCometProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Trying to simulate simultaneous clients.
 */
public class MultipleNotificationTest {
    private static final Logger log = LoggerFactory.getLogger(MultipleNotificationTest.class);
    private static final int NUMBER_OF_CLIENTS = 100;

    private SemiSpaceCometProxy space;
    private NotifyAndReadClient clients[];

    @Before
    public void setUp() {
        log.warn("\n\n\n\nNOT SUPPORTING NORMAL BUILD TESTS YET\nUse\n  mvn -Denv=dev clean install\nwhen building this module\n\n\n");
        space = new SemiSpaceCometProxy();
        space.init("http://localhost:8080/semispace-comet-server/cometd/");
        // If running within eclipse, you will have this on your classpath
        //space = SemiSpaceProxy.retrieveSpace("http://localhost:8080/semispace-war/services/space");
        clients = new NotifyAndReadClient[NUMBER_OF_CLIENTS];
        for ( int i=0 ; i < clients.length ; i++ ) {
            clients[i] = new NotifyAndReadClient(space);
        }
    }

    @After
    public void tearDown() {
        for ( NotifyAndReadClient client : clients ) {
            client.destroy();
        }
        space.destroy();
    }


    @Test
    public void testMultipleReadClients() throws InterruptedException {
        for ( NotifyAndReadClient client : clients ) {
            client.activate();
        }
        JustATestElement jate = new JustATestElement();
        jate.setSomefield("This is some field");

        // Let listeners catch up
        //Thread.sleep( 100 );
        space.write( jate, 36000);
        Thread.sleep( 1000 );
        for ( NotifyAndReadClient client : clients ) {
            Assert.assertEquals("Presuming to be able to get the same field that was written.", jate.getSomefield(), client.getReadField());
        }
        Assert.assertNotNull( space.takeIfExists(jate));
    }
}
