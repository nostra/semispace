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

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultProducer;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SemiSpaceCamelProducer extends DefaultProducer{
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceCamelProducer.class);
    private SemiSpaceInterface space;

    public SemiSpaceCamelProducer(Endpoint endpoint) {
        super(endpoint);
        space = SemiSpace.retrieveSpace();
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        log.debug("Exchange: "+exchange+", properties: "+exchange.getProperties());
        Message message = exchange.getIn();
        Object payload = message.getMandatoryBody();
        log.debug("Payload: " + payload.getClass().getName());
        space.write(payload, 1000);
    }
}
