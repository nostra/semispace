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

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
public class SemiSpaceCamelEndpoint extends DefaultEndpoint{
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceCamelEndpoint.class);

    public SemiSpaceCamelEndpoint(String endpointUri, Component component) {
        super(endpointUri, component);
    }

    @Override
    public Producer createProducer() {
        return new SemiSpaceCamelProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        return new SemiSpaceCamelConsumer(this, processor);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Exchange createExchange(Exchange exchange) {
        log.debug("Creating exchange with basis in "+exchange);
        return super.createExchange(exchange);
    }

    @Override
    public Exchange createExchange() {
        log.debug("Creating exchange from scratch");
        return super.createExchange();
    }
}
