/*
 * ============================================================================
 *
 *  File:     SemiSpacePersistentAdmin.java
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
 *  Created:      May 18, 2008
 * ============================================================================ 
 */

package org.semispace.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.DistributedEvent;
import org.semispace.EventDistributor;
import org.semispace.Holder;
import org.semispace.SemiSpace;
import org.semispace.admin.SemiSpaceAdmin;
import org.semispace.admin.SemiSpaceAdminInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiExpirationEvent;
import org.semispace.event.SemiRenewalEvent;
import org.semispace.event.SemiTakenEvent;
import org.semispace.persistence.dao.HibernateLeaseDao;
import org.semispace.persistence.dao.LeaseMeta;
import org.semispace.persistence.domain.Lease;
import org.semispace.persistence.domain.Tag;
import org.springframework.beans.factory.DisposableBean;

/**
 * Administration module that is able to persist data into
 * the database. This can be used for persisting long term
 * query objects, such as cache results. 
 * 
 * <p>The benefits are
 * <ul>
 * <li>If you are not using terracotta, you still get persistence during restarts</li>
 * <li>When using terracotta, you can (more safely) upgrade</li>
 * <li>The persistence module can be hooked on and off an existing space for snapshot purposes</li>
 * </ul>
 * </p>
 * <p>Beware of delays, though: If a long term object is inserted into the space, 
 * and the space is terminated with a kill immediately after, the object is lost.
 * Your implementation must allow losing long term objects.
 * </p>
 * <p>Beware of re-insertion: You can either insert duplicates, or not. Both
 * strategies can give you mismatches. If you insert duplicates, all objects
 * are reintroduced to the space, and a repetitive restart will give the space
 * duplicates If you are not, a query is performed to see if the object is already
 * present. If it is, it is not re-introduced. If you have more than one object of 
 * the same type, one copy is lost.
 * 
 * @TODO JavaSpaces interface v.2.1 can solve the problem of loosing a copy when restarting.
 */
public class SemiSpacePersistentAdmin extends SemiSpaceAdmin implements DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(SemiSpacePersistentAdmin.class);
    private boolean ready = false;
    private DatabaseService dbservice;
    private int delayBeforePollingInMs;
    private long necessaryLifeTimeForPersistingMs;
    private DelayQueue<DelayedId> delayQueue;
    private ExecutorService pollExecutor;
    private boolean insertDuplicates;
    private Set<Long> ignored;
    private Set<Long> inQueue;
    /**
     * Read / write lock
     */
    private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();


    /**
     * The default constructor with a 20 second delay before polling, 
     * and 60 second minimum lifetime for the element to be persisted.
     * It does not insert duplicates.
     * 
     * @param space The space to be used. 
     */
    private SemiSpacePersistentAdmin(SemiSpace space) {
        super(space);
        delayBeforePollingInMs = 20 * 1000;
        necessaryLifeTimeForPersistingMs = delayBeforePollingInMs;
        delayQueue = new DelayQueue<DelayedId>();
        pollExecutor = Executors.newSingleThreadExecutor();
        insertDuplicates = false;
        ignored = new HashSet<Long>();
        inQueue = new HashSet<Long>();
    }
    
    /**
     * Create and initialize the persistent admin connection to semispace.
     * Notice that the old admin process will just be unreferenced.
     * This method is a factory method which will create a new instance for 
     * each call. 
     * 
     * @param delayBeforePollingInMs How long to wait before examining space contents. 
     * Normal value would be 20 seconds 
     * @param insertDuplicates  <b>Normally <i>false</i></b>: 
     * During startup, shall the space be searched for already existing entries?
     * If set to false, elements read from the database is examined for existence in space
     * and will not be re-introduced if it already exists. Note that 
     * this will make duplicates disappear, whereas true will give the space more duplicates than 
     * actually should exist
     * @param necessaryLifeTimeForPersistingMs How long the lifetime of an object. A normal value would
     * be 60 seconds (given as milliseconds). 
     * must be before persisting it (if still present and alive)
     */
    public static synchronized SemiSpacePersistentAdmin createConnectedAdminInstance( SemiSpace space, int delayBeforePollingInMs, boolean insertDuplicates, int necessaryLifeTimeForPersistingMs ) {
        final SemiSpaceAdminInterface oldAdmin = space.getAdmin();
        if ( necessaryLifeTimeForPersistingMs  < delayBeforePollingInMs ) {
            throw new RuntimeException("Time for persisting cannot be less than the polling time. It does not make sense as " +
            		"an element that should have been persisted is not due to that it has not yet been polled. " +
            		"necessaryLifeTimeForPersistingMs: "+necessaryLifeTimeForPersistingMs+", delayBeforePollingInMs: "+delayBeforePollingInMs );
        }
        SemiSpacePersistentAdmin persistentAdmin = new SemiSpacePersistentAdmin( space );
        persistentAdmin.delayBeforePollingInMs = delayBeforePollingInMs;
        persistentAdmin.necessaryLifeTimeForPersistingMs = necessaryLifeTimeForPersistingMs;
        persistentAdmin.insertDuplicates = insertDuplicates;
        
        log.info("Exchanging admin instance for space with this.");
        persistentAdmin.getSpace().setAdmin(persistentAdmin);
        if ( oldAdmin instanceof SemiSpaceAdmin ) {
            ((SemiSpaceAdmin)oldAdmin).removeShutDownHook();
            if ( oldAdmin instanceof SemiSpacePersistentAdmin ) {
                log.info("Old admin was of persistent admin type. Removing shutdown hook.");
                ((SemiSpacePersistentAdmin)oldAdmin).shutdownAndAwaitTermination();
            }
        }
        persistentAdmin.performLocalInitialization();
        return persistentAdmin;
    }
    
    /**
     * Override and broaden return value.
     * @see org.semispace.admin.SemiSpaceAdmin#getSpace()
     */
    @Override
    public SemiSpace getSpace() {
        return (SemiSpace) super.getSpace();
    }
    
    @Override
    public void notifyAboutEvent(DistributedEvent event) {
        super.notifyAboutEvent(event);
        if ( ! super.hasBeenInitialized() || !ready ) {
            log.debug("Ignoring event as not having been initialized. Holder id: "+event.getEvent().getId()+", event class "+event.getEvent().getClass().getName());
            return;
        }

        //log.debug("Got incoming event. Holder id: "+event.getEvent().getId()+", event class "+event.getEvent().getClass().getName());
        if ( event.getEvent() instanceof SemiAvailabilityEvent ) {
            treatEvent((SemiAvailabilityEvent)event.getEvent());
            
        } else if ( event.getEvent() instanceof SemiExpirationEvent ) {
            treatEvent((SemiExpirationEvent)event.getEvent());
            
        } else if ( event.getEvent() instanceof SemiTakenEvent ) {
            treatEvent((SemiTakenEvent)event.getEvent());

        } else if ( event.getEvent() instanceof SemiRenewalEvent ) {
            treatEvent((SemiRenewalEvent)event.getEvent());

        } else {
            throw new RuntimeException("Unexpected event type of class "+event.getEvent().getClass().getName());
        }
    }

    private void treatEvent(SemiRenewalEvent event) {
        //log.debug("Update object in database");
        rwl.writeLock().lock();
        try {
            Lease leaseDto = dbservice.getLeaseDao().retrieveleaseByHolderId(event.getId());
            if ( leaseDto != null ) {
                leaseDto.setLiveUntil(event.getLiveUntil());
                dbservice.getLeaseDao().savelease(leaseDto);
            } else {
                // Need to reintroduce element in case it is eligible for storing now
                SemiAvailabilityEvent fake = new SemiAvailabilityEvent( event.getId());
                treatEvent(fake);
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }

    private void treatEvent(SemiAvailabilityEvent event ) {
        DelayedId delayed = new DelayedId( event.getId(), delayBeforePollingInMs);
        rwl.writeLock().lock();
        try {
            if ( inQueue.contains(Long.valueOf( delayed.getId()))) {
                log.error("It is not expected that an object shall be registered twice with the same identity.");
                throw new RuntimeException("Consistency error. Element with id already exists: "+event.getId());
            }
            inQueue.add(Long.valueOf( delayed.getId()));
            delayQueue.add(delayed);
        } finally {
            rwl.writeLock().unlock();
        }
        //log.debug("Added element to delayQueue. Id "+delayed.getId());
        pollExecutor.execute(new Runnable() {
            @SuppressWarnings("synthetic-access")
            public void run() {
                DelayedId elem = null;
                try {
                    //log.debug("Polling delayQueue which now has "+delayQueue.size()+" elements with delay "+delayBeforePollingInMs);
                    // Double the wait time in order to be "certain" something is read. Will break during extreme load, probably. Do not case...
                    elem = delayQueue.poll(delayBeforePollingInMs*2, TimeUnit.MILLISECONDS);
                    //log.debug("Poll finished on delayQueue which now has "+delayQueue.size()+" elements, got "+(elem==null?"null":"an object"));
                    if ( elem != null ) {
                        rwl.writeLock().lock();
                        try {
                            // If the element is not in the inQueue, it has, well, been removed.
                            if ( inQueue.remove(Long.valueOf( elem.getId()))) {
                                writeElementToDatabase(elem.getId());
                            }
                        } finally {
                            rwl.writeLock().unlock();
                        }
                        
                    } else {
                        //log.debug("No object found when polling. delayQueue size "+delayQueue.size());
                    }
                } catch (InterruptedException e) {
                    log.error("Got, and ignored, exception, which probably is related to shutdown of space. Masked, the exception is: "+ e);
                }
            }
        });            
    }
    
    /**
     * Read element out of space, and write it to the database. 
     */
    private void writeElementToDatabase(long id ) {
        rwl.writeLock().lock();
        try {
            Holder holder = getSpace().readHolderById(id);
            if ( holder == null ) {
                //log.debug("Object expired before being eligible for persistence.");
                return;
            }
            //log.info("===> "+(holder.getLiveUntil() - calculateTime())+" > "+necessaryLifeTimeForPersistingMs+" ??");
            if ( holder.getLiveUntil() - calculateTime() > necessaryLifeTimeForPersistingMs ) {
                // Only write if lifetime is long enough that it should be persisted
                LeaseMeta leaseMeta = LeaseMeta.createLeaseMeta(holder);
                try {
                    dbservice.getLeaseDao().write(leaseMeta, holder.getLiveUntil());
                } catch (Exception e) {
                    log.error("Could not persist element. Adding it to ignore delayQueue. XML\n"+holder.getXml(), e);
                    ignored.add( Long.valueOf(id));
                }
            } else {
                ignored.add( Long.valueOf(id));
            }
            
        } finally {
            rwl.writeLock().unlock();
        }
    }

    
    private void treatEvent(SemiExpirationEvent event) {
        //log.debug("Remove object from database (immediate / expiration)");
        removeElement(event.getId());
    }

    private void removeElement(long id) {
        rwl.writeLock().lock();
        try {
            if ( ignored.remove(Long.valueOf(id) )) {
                //log.debug("Message resided in ignore set, and was just removed from it.");
            } else if ( inQueue.remove( Long.valueOf(id))) {
                //log.debug("Message resided in inQueue, and just removed from it.");
            } else {
                //log.debug("(Trying) to remove element from DB");
                        
                removeFromDb(id);
            }
        } finally {
            rwl.writeLock().unlock();
        }
    }
    
    private void treatEvent(SemiTakenEvent event) {
        //log.debug("Remove object from database (immediate / taken)");
        removeElement(event.getId());
    }

    /**
     * NB rwl presumed locked upon entry. 
     */
    private void removeFromDb(long holderId) {
        Lease leaseDto = dbservice.getLeaseDao().retrieveleaseByHolderId(holderId);
        if ( leaseDto != null ) {
            //log.debug("Deleting holder with holder id "+holderId+".");
            dbservice.getLeaseDao().deleteleaseById(leaseDto.getId());
        } else {
            if ( log.isDebugEnabled()) {
                StringBuffer info = new StringBuffer();
                info.append("delayQueue{");
                for ( Long did : inQueue) {
                    info.append(" "+did);
                    if ( did.longValue() == holderId ) {                    
                        info.append("Strange: Found holder id in delayQueue.");
                    }
                }
                info.append("} ignored {");
                for ( Long id : ignored) {
                    info.append(" "+id);
                    if ( id.longValue() == holderId ) {
                        info.append("Strange: Found holder id in ignored list");                    
                    }
                }
                info.append("} ");
                
                log.debug("Could not find holder with holder id "+holderId+" in database. This could be due to resynchronization when having elements of same type and searchmap. " +
                		"It could also be that the element was removed before it was even picked by the admin. "+info);
                // Can consider to throw exception. Does not really matter though, as this problem will not be a consistency error.
                //throw new RuntimeException("Tried to delete "+holderId+"\nList of ignored: "+ignored+" queued: "+delayQueue+"\nChecked against "+new DelayedId( holderId, delayBeforePollingInMs));
            }
        }
    }

    /**
     * This method presumes that the SemiSpace has been set on this object
     * @see org.semispace.admin.SemiSpaceAdmin#performInitialization()
     */
    private void performLocalInitialization() {
        if ( ! super.hasBeenInitialized()) {
            super.performInitialization();
        }
        log.info("Initializing persistent connection");
        
        dbservice = DatabaseFactory.retrieveDatabaseService();
        dbservice.getLeaseDao().cleanup();
        // First invert all IDs.
        HibernateLeaseDao leaseDao = dbservice.getLeaseDao();
        leaseDao.invertHolderIds();
        Long[] presentHolderIds = getSpace().findAllHolderIds();
        ready = true;
        // Take all elements and re-insert them into the space. 
        insertElementsWithNegativeIdIntoSpace(leaseDao);
        // Take elements already present, which may have not been added already, and add them
        insertElementsWithNonPresentHolderId( presentHolderIds, leaseDao );
    }

    private void insertElementsWithNonPresentHolderId(Long[] presentHolderIds, HibernateLeaseDao leaseDao) {
        for ( Long hid : presentHolderIds ) {
            if ( leaseDao.retrieveleaseByHolderId(hid.longValue()) == null ) {
                // Element not yet registered - faking availability event
                SemiAvailabilityEvent fake = new SemiAvailabilityEvent(hid.longValue());
                treatEvent(fake);
            }
        }
    }

    /**
     * Notice that if the space is not set to insert duplicates, copies
     * of an object that resides in the persistent storage gets lost.
     */
    private void insertElementsWithNegativeIdIntoSpace(HibernateLeaseDao leaseDao) {
        Integer[] ids = leaseDao.retriveLeaseIdsForAllNegativeHolderIds();
        // For each element, reintroduce into space
        for (Integer id : ids) {
            rwl.writeLock().lock();
            try {
                Lease dblease = leaseDao.retrieveleaseById(id.intValue());
                boolean shallInsert = true;
                final Map<String, String> searchMap = transformTagsToMap(dblease.getTags());
                if (!insertDuplicates && getSpace().findOrWaitLeaseForTemplate(searchMap, 0, false) != null) {
                    // Element already present
                    shallInsert = false;
                }
                if (shallInsert) {
                    getSpace().writeToElements(dblease.getDoctype(), dblease.getLiveUntil() - System.currentTimeMillis(), dblease.getActual(), searchMap);
                }
                // Removing lease from DB as it has been re-inserted into space.
                leaseDao.deleteleaseById(dblease.getId());
            } finally {
                rwl.writeLock().unlock();
            }
        }
    }

    private Map<String, String> transformTagsToMap(Set<Tag> tags) {
        Map<String, String> searchMap = new HashMap<String, String>();
        for ( Tag tag : tags ) {
            searchMap.put(tag.getName(), tag.getContent());
        }
        return searchMap;
    }

    /**
     * Method exists and is protected for the benefit of junit tests.
     */
    protected DatabaseService getDbservice() {
        return this.dbservice;
    }

    
    @Override
    protected void shutdownAndAwaitTermination() {
        // Cancel poll executor
        pollExecutor.shutdown(); 

        log.debug("Shall (potentially) persist "+inQueue.size()+" elements.");
        Long[] elements = inQueue.toArray(new Long[0]);
        
        rwl.writeLock().lock();
        try {
             Iterator<DelayedId> dit = delayQueue.iterator();
            while ( dit.hasNext() ) {
                delayQueue.remove(dit.next());
            }
        } finally {
            rwl.writeLock().unlock();
        }
    
        for ( Long id : elements ) {
            rwl.writeLock().lock();
            try {
                writeElementToDatabase(id.longValue());
            } finally {
                rwl.writeLock().unlock();
            }
        }
        try {
            if (!pollExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                pollExecutor.shutdownNow(); 
                if (!pollExecutor.awaitTermination(60, TimeUnit.SECONDS))
                    log.warn("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            pollExecutor.shutdownNow();
            log.debug("Ignoring exception during shutdown.", ie);
        }

        ignored.clear();
        
        super.shutdownAndAwaitTermination();
    }

    /**
     * In order to let the object support shutdown from spring framework. 
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() {
        log.debug("Spring calls destroy.");
        if ( !pollExecutor.isShutdown()) {
            shutdownAndAwaitTermination();            
        }
    }
}
