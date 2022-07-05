/*
 * ============================================================================
 *
 *  File:     SemiSpaceAdmin.java
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
 *  Created:      16. feb.. 2008
 * ============================================================================
 */

package org.semispace.admin;

import com.thoughtworks.xstream.XStream;
import org.semispace.DistributedEvent;
import org.semispace.Holder;
import org.semispace.NameValueQuery;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SemiSpaceAdmin implements SemiSpaceAdminInterface {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceAdmin.class);

    private boolean master;

    private SemiSpaceInterface space;

    private boolean beenInitialized;

    private long clockSkew;

    private int spaceId;

    private ExecutorService pool;

    private Thread shutDownHook;

    private PeriodicHarvest periodicHarvest;

    public SemiSpaceAdmin(SemiSpaceInterface terraSpace) {
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(0, 5000,
                5L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(true));
        tpe.setThreadFactory(new DaemonDelegateFactory(tpe.getThreadFactory()));
        // Exchanging strategy. When thread pool is full, try to run on local thread.
        tpe.setRejectedExecutionHandler(new SemiSpaceRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()));
        tpe.allowCoreThreadTimeOut(true);
        this.pool = tpe;
        this.space = terraSpace;
        this.beenInitialized = false;
        this.clockSkew = 0;
        this.spaceId = 0;
        this.master = false;
        this.periodicHarvest = new PeriodicHarvest(this);
    }

    /**
     * Used from junit test.
     */
    protected int getSpaceId() {
        return spaceId;
    }

    /**
     * @return space configured for this admin. Beneficiary for subclasses.
     */
    protected SemiSpaceInterface getSpace() {
        return space;
    }

    @Override
    public ExecutorService getThreadPool() {
        return pool;
    }

    @Override
    public boolean hasBeenInitialized() {
        return this.beenInitialized;
    }

    @Override
    public boolean isMaster() {
        return this.master;
    }

    @Override
    public long calculateTime() {
        return System.currentTimeMillis() - clockSkew;
    }

    @Override
    public void performInitialization() {
        if (beenInitialized) {
            log.warn("Initialization called more than once.");
            return;
        }
        beenInitialized = true;

        Runnable hook = new Runnable() {
            @Override
            @SuppressWarnings("synthetic-access")
            public void run() {
                log.info("Shutdown hook shutting down semispace.");
                shutdownAndAwaitTermination();
            }
        };
        shutDownHook = new Thread(hook);
        Runtime.getRuntime().addShutdownHook(shutDownHook);

        //
        // Fire up connection
        //
        long last;
        long current = SemiSpace.ONE_DAY;
        int count = 0;
        // Perform query as long as the connection is improving
        do {
            count++;
            last = current;
            current = fireUpConnection();
        } while (current < last);
        log.info("Needed " + count + " iterations in order to find the best time, which was " + current + " ms.");

        //
        // Figure out the ID of this space
        //
        spaceId = figureOutSpaceId();
        log.info("Space id was found to be " + spaceId);

        //
        // (Try to) find clock skew
        queryForMasterTime();
        // log.info( "Calculate time, which should give an approximation of the master time, reports ["+new
        // Date(calculateTime())+"]");

        periodicHarvest = new PeriodicHarvest(this);
        periodicHarvest.startReaper();
    }

    private int figureOutSpaceId() {
        List<IdentifyAdminQuery> admins = new ArrayList<IdentifyAdminQuery>();

        IdentifyAdminQuery masterFound = populateListOfAllSpaces(admins);

        Collections.sort(admins, new IdentifyAdminQueryComparator());
        int foundId = 1;
        if (!admins.isEmpty()) {
            // Collection is sorted, and therefore the admin should increase
            IdentifyAdminQuery admin = admins.get(0);
            if (admin.id != null) {
                foundId = admin.id.intValue() + 1;
            }
        }
        if (masterFound == null) {
            log.info("I am master, as no other master was identified.");
            assumeAdminResponsibility(!admins.isEmpty());
        }
        return foundId;
    }

    protected void assumeAdminResponsibility(boolean sendAdminInfoAboutSystemTime) {
        master = true;
        if (sendAdminInfoAboutSystemTime) {
            log.info("Informing other masters of system time.");
            TimeAnswer ta = new TimeAnswer();
            ta.masterId = getSpaceId();
            ta.timeFromMaster = Long.valueOf(System.currentTimeMillis());
            space.write(ta, 1000);
        }
    }

    /**
     * Protected as it is used every once in a while from periodic object reaper
     *
     * @param admins List to fill with the admin processes found
     * @return List of identified SemiSpace admin classes
     */
    protected IdentifyAdminQuery populateListOfAllSpaces(List<IdentifyAdminQuery> admins) {
        IdentifyAdminQuery identifyAdmin = new IdentifyAdminQuery();
        identifyAdmin.hasAnswered = Boolean.FALSE;
        space.write(identifyAdmin, SemiSpace.ONE_DAY);

        IdentifyAdminQuery iaq = new IdentifyAdminQuery();
        iaq.hasAnswered = Boolean.TRUE;

        IdentifyAdminQuery masterFound = null;
        IdentifyAdminQuery answer = null;
        long waitFor = 750;
        do {
            answer = space.take(iaq, waitFor);
            // When the first answer has arrived, the others, if any, should come close behind.
            waitFor = 250;
            if (answer != null) {
                admins.add(answer);
                if (Boolean.TRUE.equals(answer.amIAdmin)) {
                    if (masterFound != null) {
                        log.error("More than one admin found, both " + masterFound.id + " and " + answer.id);
                    }
                    masterFound = answer;
                }
            }
            // Looping until we do not find any more admins
        } while (answer != null);

        while (space.takeIfExists(new IdentifyAdminQuery()) != null) { // NOSONAR
            // Remove identity query from space as we do not need it anymore. If more than one present, we have a race condition (not likely)
        }

        return masterFound;
    }

    /**
     * The very first query may take some time (when using terracotta), and it is therefore prudent to kick start the
     * connection.
     *
     * @return Time it took in ms for an answer to be obtained.
     */
    private long fireUpConnection() {
        long bench = System.currentTimeMillis();
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "Internal admin query";
        nvq.value = "Dummy-value in order to be (quite) unique [" + bench + "]";
        space.write(nvq, SemiSpace.ONE_DAY);
        nvq = space.take(nvq, 1000);
        if (nvq == null) {
            throw new AssertionError("Unable to retrieve query which is designed to kickstart space.");
        }
        long timed = System.currentTimeMillis() - bench;
        return timed;
    }

    /**
     * Obtaining time by querying with internal query
     */
    private void queryForMasterTime() {
        TimeQuery tq = new TimeQuery();
        tq.isFinished = Boolean.FALSE;
        // Letting the query itself exist a day. This is as skew can be large.
        space.write(tq, SemiSpace.ONE_DAY);

        space.read(new TimeAnswer(), 2500);
        space.takeIfExists(tq);
    }

    /**
     *
     */
    private void notifyAboutInternalQuery(InternalQuery incoming) {
        // log.info("Incoming admin query for space "+getSpaceId()+" of type "+incoming.getClass().getName());
        if (incoming instanceof TimeQuery) {
            answerTimeQuery((TimeQuery) incoming);

        } else if (incoming instanceof IdentifyAdminQuery) {
            answerIdentityQuery((IdentifyAdminQuery) incoming);

        } else if (incoming instanceof TimeAnswer) {
            treatIncomingTimeAnswer((TimeAnswer) incoming);

        } else {
            log.warn("Unknown internal query");
        }
    }

    /**
     * A (potentially new) admin process has given time answer. Adjust time accordingly
     */
    private void treatIncomingTimeAnswer(TimeAnswer incoming) {
        if (isMaster()) {
            if (incoming.masterId != getSpaceId()) {
                String adminfo = "Got more than one space that perceives it is admin space: " + incoming.masterId
                        + " and myself: " + getSpaceId();
                if (incoming.masterId < getSpaceId()) {
                    master = false;
                    adminfo += ". Removing this space as master.";
                } else {
                    adminfo += ". Keeping this space as master.";
                }
                log.warn(adminfo);
            } else {
                clockSkew = 0;
            }

        }

        // Need to test again as we may have been reset:
        if (!isMaster()) {
            long systime = System.currentTimeMillis();
            clockSkew = systime - incoming.timeFromMaster.longValue();
            log.info("Master has " + " [" + new Date(incoming.timeFromMaster.longValue()) + "]" + ", whereas I have ["
                    + new Date(systime) + "]. This gives a skew of " + clockSkew + ".");
        }

    }

    private void answerIdentityQuery(IdentifyAdminQuery identify) {
        if (spaceId < 1) {
            return;
        }
        if (identify.hasAnswered != null && identify.hasAnswered.booleanValue()) {
            return;
        }
        IdentifyAdminQuery answer = new IdentifyAdminQuery();
        answer.amIAdmin = Boolean.valueOf(master);
        answer.hasAnswered = Boolean.TRUE;
        answer.id = Integer.valueOf(spaceId);
        log.debug("Giving identity answer for space " + spaceId + ", which is" + (master ? "" : " NOT") + " master.");
        space.write(answer, SemiSpace.ONE_DAY);
    }

    private void answerTimeQuery(TimeQuery tq) {
        if (isMaster() && !tq.isFinished.booleanValue()) {
            TimeAnswer answer = new TimeAnswer();
            answer.timeFromMaster = Long.valueOf(System.currentTimeMillis());
            answer.masterId = getSpaceId();
            space.write(answer, 1000);
            log.info("Giving answer about time (which was found to be " + answer.timeFromMaster + ", which is "
                    + new Date(answer.timeFromMaster.longValue()) + ")");
        }
    }

    @Override
    public void notifyAboutEvent(DistributedEvent event) {
        if (event.getEvent() instanceof SemiAvailabilityEvent) {
            if (InternalQuery.class.getName().equals(event.getHolderClassName()) && space instanceof SemiSpace) {
                Holder holder = ((SemiSpace) space).readHolderById(event.getEvent().getId());
                if (holder != null) {
                    notifyAboutInternalQuery((InternalQuery) new XStream().fromXML(holder.getXml()));
                }
            }
        }
    }

    /**
     * The cached thread pool has a timeout of a minute, so a shutdown is not immediate. This method will try to speed
     * up the process, but it is not mandatory to use it.
     * The method is protected for the benefit of subclasses.
     */
    protected void shutdownAndAwaitTermination() {
        if (pool.isShutdown() && periodicHarvest.isCancelled()) {
            // Already had a shutdown notification.
            return;
        }
        periodicHarvest.cancelReaper();
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.warn("Pool did not terminate");
                }
            }
        } catch (InterruptedException ignored) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Remove shutdown hook which otherwise is run when the space is shut down.
     * Primarily used when exchanging this admin with another.
     */
    public void removeShutDownHook() {
        periodicHarvest.cancelReaper();
        if (shutDownHook != null) {
            Runtime.getRuntime().removeShutdownHook(shutDownHook);
        }
    }

    private static class IdentifyAdminQueryComparator implements Comparator<IdentifyAdminQuery>, Serializable {
        @Override
        public int compare(IdentifyAdminQuery a1, IdentifyAdminQuery a2) {
            if (a1.id == null) {
                return 1;
            } else if (a2.id == null) {
                return -1;
            }
            return a2.id.intValue() - a1.id.intValue();
        }
    }
}
