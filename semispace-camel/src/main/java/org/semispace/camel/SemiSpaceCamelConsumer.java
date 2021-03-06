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

import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SemiSpaceCamelConsumer extends DefaultConsumer {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceCamelConsumer.class);
    private int count = 0;

    public SemiSpaceCamelConsumer(DefaultEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    
    /*
    @Override
    protected int poll() throws Exception {
        int num = count >= 2 ? 1 : 0;
        log.debug("==> Polling and returning "+num);
        //SemiSpace.retrieveSpace().readIfExists(new CustomPayload());
        count++;
        return num;
    }
    */
}
