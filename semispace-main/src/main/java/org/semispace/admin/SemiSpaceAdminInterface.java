/*
 * ============================================================================
 *
 *  File:     SemiSpaceAdminInterface.java
 *----------------------------------------------------------------------------
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
 *  Created:      Mar 18, 2008
 * ============================================================================ 
 */

package org.semispace.admin;

import java.util.concurrent.ExecutorService;

import org.semispace.EventDistributor;

public interface SemiSpaceAdminInterface {

    public boolean hasBeenInitialized();

    public boolean isMaster();

    public long calculateTime();

    public void performInitialization();
    
    /**
     * @return Executor service used for treating thread elements
     */
    public ExecutorService getThreadPool();

    /**
     * Administrator should get notified about all events. This
     * can be used for attaching persistence engine.
     */
    public void notifyAboutEvent(EventDistributor event);
    
}