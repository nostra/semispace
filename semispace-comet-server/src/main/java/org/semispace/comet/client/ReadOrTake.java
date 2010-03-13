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

import org.cometd.client.BayeuxClient;

import java.util.Map;

/**
 * Similar functionality collected
 */
public interface ReadOrTake {
    /**
     * Lag in network which will be added to wait time for operations
     */
    public static final long PRESUMED_NETWORK_LAG_MS = 500;

    String doReadOrTake(BayeuxClient client, Map<String, Object> map, long maxWaitMs );
}
