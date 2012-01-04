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
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.semispace.SemiSpace;

public class SemiSpaceCamelConsumerTest extends CamelTestSupport {

    @EndpointInject(uri = "mock:result")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "semispace:start")
    protected ProducerTemplate template;


    @Test
    @Ignore
    public void testReadingMessageFromSpace() throws InterruptedException {
        CustomPayload payload = new CustomPayload();
        payload.setId(new Integer(1));
        payload.setContents("Test contents");

        resultEndpoint.expectedBodiesReceived(payload);
        SemiSpace.retrieveSpace().write(payload, 1000);
        resultEndpoint.setResultWaitTime(2000);

        resultEndpoint.assertIsSatisfied();
        assertNull(SemiSpace.retrieveSpace().takeIfExists(new CustomPayload()));
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            public void configure() {
                from("semispace:start").to("mock:result");
            }
        };
    }

}
