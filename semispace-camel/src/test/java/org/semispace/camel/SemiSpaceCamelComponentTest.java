/*
 * Copyright (c) 2011 Erlend Nossum
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

package org.semispace.camel;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.semispace.SemiSpace;

/**
 *
 */
public class SemiSpaceCamelComponentTest extends CamelTestSupport {

    @EndpointInject(uri = "semispace:result")
    protected SemiSpaceCamelEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        //super.getCamelContextService().
    }

    @Ignore 
    public void testSendMatchingMessage() throws InterruptedException {
        String expectedBody = "<matched/>";

        template.sendBodyAndHeader(expectedBody, "foo", "bar");
    }

    @Test
    public void testStoringMessageInSpace() throws InterruptedException {
        CustomPayload payload = new CustomPayload();
        payload.setId(new Integer(1));
        payload.setContents("Test contents");
        template.sendBodyAndHeader (payload, "header", "foo");
        CustomPayload expect = SemiSpace.retrieveSpace().takeIfExists(new CustomPayload());
        assertNotNull(expect);
        assertEquals(payload.getContents(), expect.getContents());
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("direct:start").to("semispace:result");
            }
        };
    }
    
}
