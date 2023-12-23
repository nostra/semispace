/*
 * ============================================================================
 *
 *  File:     SemiSpaceRejectedExecutionHandler.java
 *----------------------------------------------------------------------------
 *
 * Copyright 2011 Erlend Nossum
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
 *
 *  Description:  See javadoc below
 *
 *  Created:      5. feb. 2011
 * ============================================================================
 */

package org.semispace.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

public class SemiSpaceRejectedExecutionHandler implements RejectedExecutionHandler {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceRejectedExecutionHandler.class);

    private final RejectedExecutionHandler rejectedExecutionHandler;

    public SemiSpaceRejectedExecutionHandler(RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        log.debug("Making a delegate for " + rejectedExecutionHandler.getClass().getName());
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        log.error("Could not schedule event.");
        rejectedExecutionHandler.rejectedExecution(r, executor);
    }

}
