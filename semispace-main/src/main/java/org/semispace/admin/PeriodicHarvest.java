/*
 * ============================================================================
 *
 *  File:     PeriodicHarvest.java
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
 *  Created:      Jul 11, 2008
 * ============================================================================ 
 */

package org.semispace.admin;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Harvest objects periodically if this instance of semispace is 
 * the master object.
 */
public class PeriodicHarvest {
//    private static final Logger log = LoggerFactory.getLogger(PeriodicHarvest.class);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private SemiSpaceAdmin semiSpaceAdmin;
    private ScheduledFuture<?> handle; 
    
    public PeriodicHarvest(SemiSpaceAdmin semiSpaceAdmin) {
        this.semiSpaceAdmin = semiSpaceAdmin;
        if ( scheduler instanceof ScheduledThreadPoolExecutor) {
            ScheduledThreadPoolExecutor stpe = (ScheduledThreadPoolExecutor) scheduler;
            //stpe.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
            //stpe.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            stpe.setThreadFactory(new DaemonDelegateFactory( stpe.getThreadFactory()));
        }
    }

    public void startReaper() {
        if ( handle != null ) {
            cancelReaper();
        }
        final Runnable harvester = new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
                SemiSpaceInterface space = semiSpaceAdmin.getSpace();
                if ( semiSpaceAdmin.isMaster() && space instanceof SemiSpace ) {
                    //log.debug("Harvesting - start - ...");
                    ((SemiSpace)space).harvest();
                    //log.debug("Harvesting - end - ...");
                }
                    
            }
        };
        handle = scheduler.scheduleAtFixedRate(harvester, 60, 15, SECONDS);
        
    }
    
    public void cancelReaper() {
        if ( handle != null ) {
            handle.cancel(true);            
        }
        handle = null;
    }

    public boolean isCancelled() {
        if ( handle != null ) {
            return handle.isCancelled();
        }
        return true;
    }
}
