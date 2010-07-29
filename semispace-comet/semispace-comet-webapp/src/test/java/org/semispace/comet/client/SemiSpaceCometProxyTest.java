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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.semispace.SemiEventRegistration;
import org.semispace.comet.client.notification.NotificationClientIntegration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class SemiSpaceCometProxyTest {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceCometProxyTest.class);
    SemiSpaceCometProxy[] proxies = new SemiSpaceCometProxy[100];

    @Before
    public void setUp() throws Exception {
        log.warn("\n\n\n\nNOT SUPPORTING NORMAL BUILD TESTS YET\nUse\n  mvn -Denv=dev clean install\nwhen building this module\n\n\n");
        for ( int i=0 ; i < proxies.length ; i++ ) {
            proxies[i] = new SemiSpaceCometProxy();
            proxies[i].init("http://localhost:8080/semispace-comet-server/cometd/");
        }
    }

    @After
    public void tearDown() throws Exception {
        for ( SemiSpaceCometProxy proxy : proxies ) {
            proxy.destroy();
        }
    }

    @Test
    public void testNotifyOnSeveralProxies() throws InterruptedException {
        long bench = System.currentTimeMillis();
        NotificationClientIntegration listener = new NotificationClientIntegration();
        List<SemiEventRegistration> leases = new ArrayList<SemiEventRegistration>();
        for ( SemiSpaceCometProxy proxy : proxies ) {
            leases.add( proxy.notify(new FieldHolder(), listener, 5000));
        }

        FieldHolder onlyOne = new FieldHolder();
        onlyOne.setFieldA("A");
        onlyOne.setFieldB("B");
        proxies[0].write(onlyOne, 500);
        proxies[0].write(new FieldHolder(), 50); // Any
        proxies[0].write(new FieldHolder(), 50); // Any
        Assert.assertNotNull( proxies[0].read(onlyOne, 900)); // Reading the one
        Assert.assertNotNull( proxies[0].take(onlyOne, 900)); // Taking the one
        Assert.assertNull( "Just using some time in order to get space elements notified properly", proxies[0].take(onlyOne, 1300));
        Assert.assertEquals("As 3 objects was written into space, 3 objects should have flagged availability in "+proxies.length+" of spaces", 3*proxies.length, listener.getAvailability());
        Assert.assertEquals(proxies.length, listener.getTaken());
        // Cannot test listener expiration, as it depends on the sequence of objects in the space, which now
        // is unordered
        // Assert.assertEquals("0, 1 or 2 objects tend to be expired.", 2, listener.expiration);
        Assert.assertEquals(0, listener.getRenewal());

        Assert.assertNull( "Space should now be empty", proxies[0].read(onlyOne, 50));
        for ( SemiEventRegistration lease: leases) {
            lease.getLease().cancel();
        }
        log.info("Time taken for test "+(System.currentTimeMillis() - bench )+" ms");
    }

    @Test
    public void comparingPerformanceUsingOneProxy() throws InterruptedException {
        long bench = System.currentTimeMillis();
        NotificationClientIntegration listener = new NotificationClientIntegration();
        List<SemiEventRegistration> leases = new ArrayList<SemiEventRegistration>();
        leases.add( proxies[0].notify(new FieldHolder(), listener, 20000));

        FieldHolder onlyOne = new FieldHolder();
        onlyOne.setFieldA("A");
        onlyOne.setFieldB("B");
        for ( int i=0 ; i < proxies.length ; i++ ) {
            proxies[0].write(onlyOne, 20000);
        }
        for ( int i=0 ; i < proxies.length ; i++ ) {
            Assert.assertNotNull( proxies[0].take(onlyOne, 200)); // Taking the one
        }
        Assert.assertNull( "Just using some time in order to get space elements notified properly", proxies[0].take(onlyOne, 600));
        Assert.assertEquals("As 3 objects was written into space, 3 objects should have flagged availability in "+proxies.length+" of spaces", proxies.length, listener.getAvailability());
        Assert.assertEquals(proxies.length, listener.getTaken());
        // Cannot test listener expiration, as it depends on the sequence of objects in the space, which now
        // is unordered
        // Assert.assertEquals("0, 1 or 2 objects tend to be expired.", 2, listener.expiration);
        Assert.assertEquals(0, listener.getRenewal());

        Assert.assertNull( "Space should now be empty", proxies[0].read(onlyOne, 50));
        for ( SemiEventRegistration lease: leases) {
            lease.getLease().cancel();
        }
        log.info("Time taken for test "+(System.currentTimeMillis() - bench )+" ms");
    }

}
