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
import org.semispace.NameValueQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ReadClientTest {
    private static final Logger log = LoggerFactory.getLogger(ReadClientTest.class);
    private SemiSpaceCometProxy proxy;
    @Before
    public void setUp() throws Exception {
        log.warn("\n\n\n\nNOT SUPPORTING NORMAL BUILD TESTS YET\nUse\n  mvn -Denv=dev clean install\nwhen building this module\n\n\n");
        proxy = new SemiSpaceCometProxy();
        proxy.init("http://localhost:8080/semispace-comet-server/cometd/");
    }

    @After
    public void tearDown() throws Exception {
        Thread.sleep(500);
        proxy.destroy();
    }

    @Test
    public void testRead() throws Exception {
        //for ( int i=0 ; i < 1000 ; i++ )
        NameValueQuery nvq = proxy.read(new NameValueQuery(), 8000);
        Assert.assertNotNull(nvq);
        Assert.assertEquals("dummyName", nvq.name);        
    }
}
