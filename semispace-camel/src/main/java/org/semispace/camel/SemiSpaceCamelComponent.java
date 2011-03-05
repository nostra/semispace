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
import org.apache.camel.impl.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 *
 */
public class SemiSpaceCamelComponent extends DefaultComponent {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceCamelComponent.class);

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)  {
        log.debug("uri: "+uri+", remaining: "+remaining+", parameters: "+parameters);
        return new SemiSpaceCamelEndpoint(uri, this);
    }
}
