/*
 * ============================================================================
 *
 * Copyright 2008 Erlend Nossum
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
 *  Created:      Mar 14, 2009
 * ============================================================================
 */
package org.semispace.admin;

import java.util.concurrent.ThreadFactory;

/**
 * A simple class which just wraps a thread factory in a manner
 * which makes all threads it creates become daemon threads.
 */
public final class DaemonDelegateFactory implements ThreadFactory {
    private ThreadFactory threadFactory;

    public DaemonDelegateFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public Thread newThread(Runnable r) {
        Thread t = threadFactory.newThread(r);
        t.setDaemon(true);
        return t;
    }
}
