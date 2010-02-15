/*
 * ============================================================================
 *
 *  File:     SemiSpacePersistentAdminTest.java
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

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.NameValueQuery;
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiLease;
import org.semispace.SemiSpace;
import org.semispace.admin.SemiSpaceAdminInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;
import org.semispace.persistence.SemiSpacePersistentAdmin;
import org.semispace.persistence.dao.HibernateLeaseDao;
import org.semispace.persistence.domain.Lease;

public class SemiSpacePersistentAdminTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(SemiSpacePersistentAdminTest.class);
    
    private SemiSpace space;
    private SemiSpaceAdminInterface semiSpaceAdmin;
    
    @Override
    public void setUp() {
        space = (SemiSpace) SemiSpace.retrieveSpace();
        semiSpaceAdmin = space.getAdmin();
    }
    
    @Override
    public void tearDown() {
        space.setAdmin(semiSpaceAdmin);
    }
    
    public class JunitEventListener implements SemiEventListener {
        private long id=0;
        public void notify(SemiEvent theEvent) {
            if ( theEvent instanceof SemiAvailabilityEvent ) {
                this.id = theEvent.getId();
            }
        }
        public long getId() {
            return this.id;
        }

    }

    public void testSimpleUse() throws InterruptedException {
        SemiSpacePersistentAdmin pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 0, false, 0);
        assertTrue(pa.hasBeenInitialized());
        HibernateLeaseDao leaseDao = pa.getDbservice().getLeaseDao();
        final long beforeSize = leaseDao.size();

        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        JunitEventListener junitEventListener = new JunitEventListener();
        SemiEventRegistration listener = space.notify(new NameValueQuery(), junitEventListener, 1000); 
        SemiLease lease = space.write(nvq, 1500);
        Thread.sleep(300);
        assertEquals(beforeSize + 1, leaseDao.size());
        assertTrue(junitEventListener.getId() > 0 );
        
        Lease leaseDto = leaseDao.retrieveleaseByHolderId(junitEventListener.getId());
        
        assertNotNull( leaseDto);
        assertEquals( NameValueQuery.class.getName(), leaseDto.getDoctype());
        
        assertTrue(lease.cancel());
        assertTrue(listener.getLease().cancel());
        Thread.sleep(100);
        assertNull("After cancellation of lease, it should not be possible to read it back.", leaseDao.retrieveleaseByHolderId(junitEventListener.getId()));
        assertEquals(beforeSize, leaseDao.size());
        pa.removeShutDownHook();
    }

    /**
     * I want insertion to lag, and this intends to test it.
     */
    public void testInsertionActuallyLag() throws InterruptedException {
        SemiSpacePersistentAdmin pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 750, false, 2000);
        assertTrue(pa.hasBeenInitialized());

        HibernateLeaseDao leaseDao = pa.getDbservice().getLeaseDao();
        final long beforeSize = leaseDao.size();

        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        nvq.value = "I want insertion to lag, and this intends to test it.";

        SemiLease lease = space.write(nvq, 5000);
        Thread.sleep(500);
        assertEquals("No increase before time has passed", beforeSize, leaseDao.size());
        Thread.sleep(750);
        assertEquals("Time has passed and number of leases should increase", beforeSize+1, leaseDao.size());
        
        assertTrue(lease.cancel());
        assertEquals(beforeSize, leaseDao.size());
        pa.removeShutDownHook();
    }

    public void testTimeout() throws InterruptedException {
        SemiSpacePersistentAdmin pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 1000, false, 1000);
        assertTrue(pa.hasBeenInitialized());
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        JunitEventListener junitEventListener = new JunitEventListener();
        SemiEventRegistration listener = space.notify(new NameValueQuery(), junitEventListener, 9000); 
        SemiLease lease = space.write(nvq, 750);
        Thread.sleep(100);
        long firstId = junitEventListener.getId();
        assertTrue(firstId > 0 );
        
        HibernateLeaseDao leaseDao = pa.getDbservice().getLeaseDao();
        Lease leaseDto = leaseDao.retrieveleaseByHolderId(firstId);
        assertNull( "Element has a too short lifetime to be stored in database, but it got stored", leaseDto);
        
        assertTrue(lease.cancel());
        
        // Now write with a long enough timeout for the space to persist it.
        lease = space.write(nvq, 3000);
        Thread.sleep(1200);
        assertTrue("Element id should increase due to new write.", firstId < junitEventListener.getId());
        
        leaseDto = leaseDao.retrieveleaseByHolderId(junitEventListener.getId());
        assertNotNull( "Element has been alive long enough for being stored.", leaseDto);
        assertEquals( NameValueQuery.class.getName(), leaseDto.getDoctype());
        
        assertTrue(lease.cancel());
        assertTrue(listener.getLease().cancel());
        Thread.sleep(100);
        assertNull("After cancellation of lease, it should not be possible to read it back.", leaseDao.retrieveleaseByHolderId(junitEventListener.getId()));
        pa.removeShutDownHook();
    }

    public void testPersistenceAfterShutDown() throws InterruptedException {
        SemiSpacePersistentAdmin pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 60*1000, false, 60*1000);
        pa.performInitialization();
        assertTrue(pa.hasBeenInitialized());
        
        HibernateLeaseDao leaseDao = pa.getDbservice().getLeaseDao();
        
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        nvq.value = "test";
        assertNull(space.takeIfExists(nvq));
        
        JunitEventListener junitEventListener = new JunitEventListener();
        SemiEventRegistration listener = space.notify(new NameValueQuery(), junitEventListener, 9000); 
        SemiLease lease = space.write(nvq, 61 * 1000);
        Thread.sleep(200);
        
        // "shutting down" admin 
        pa.shutdownAndAwaitTermination();
        pa.removeShutDownHook();
        
        assertTrue(listener.getId()> 0 );
        
        Lease leaseDto = leaseDao.retrieveleaseByHolderId(junitEventListener.getId());
        assertNotNull("Lease with id "+junitEventListener.getId()+" should be found in database as the persistence " +
        		"connection has shut down, storing all elements not yet persisted.", leaseDto);
        
        // cleanup
        space.setAdmin(semiSpaceAdmin);
        assertTrue( lease.cancel() );
        listener.getLease().cancel();
        leaseDao.deleteleaseById(leaseDto.getId());
        assertNull( "Lease for element has been cancelled, and therefore the object does not exist.", space.takeIfExists(nvq));
    }
    
    /**
     * The goal of this test is to check that elements after a shutdown is read back. 
     */
    public void testPersistenceAfterStartup() throws InterruptedException {
        SemiSpacePersistentAdmin pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 60*1000, false, 60*1000);
        assertTrue(pa.hasBeenInitialized());
        
        HibernateLeaseDao leaseDao = pa.getDbservice().getLeaseDao();
        
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        nvq.value = "persistence after startup";
        space.write(nvq, 61 * 1000);
        // Sleep in order to allow registration of object
        Thread.sleep(200);
        
        // "shutting down" admin 
        pa.shutdownAndAwaitTermination();
        pa.removeShutDownHook();
        
        // Remove element from space
        space.setAdmin(semiSpaceAdmin);
        assertNotNull(space.takeIfExists(nvq));
        assertNull(space.takeIfExists(nvq));
        Thread.sleep(100);
        
        // Register listener
        JunitEventListener junitEventListener = new JunitEventListener();
        SemiEventRegistration listener = space.notify(nvq, junitEventListener, 9000); 
        
        // Reload space - this should re-insert element
        assertEquals(0, junitEventListener.getId());
        pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 1000, false, 1000);
        assertTrue(pa.hasBeenInitialized());
        Thread.sleep(400);
        assertEquals("After startup, no old elements should be present", 0,  leaseDao.retriveLeaseIdsForAllNegativeHolderIds().length);
        
        assertTrue("After re-initialization of persistence, all old values should be inserted with relevant events triggered.", junitEventListener.getId()> 0 );
        
        final NameValueQuery read = (NameValueQuery) space.readIfExists(nvq);
        assertNotNull(read);
        assertEquals(nvq.name, read.name);
        assertEquals(nvq.value, read.value);
        
        // Need to sleep more in order to force insertion into space.
        Thread.sleep(1100);
        Lease leaseDto = leaseDao.retrieveleaseByHolderId(junitEventListener.getId());
        assertNotNull("Lease with holder id "+junitEventListener.getId()+" should be found in database as the persistence " +
                "connection has shut down, storing all elements not yet persisted.", leaseDto);
        assertNotNull( space.takeIfExists(nvq));
        assertNull( space.takeIfExists(nvq));
        listener.getLease().cancel();
        pa.shutdownAndAwaitTermination();
        pa.removeShutDownHook();
        
        assertNull("Element should have been removed.", leaseDao.retrieveleaseByHolderId(junitEventListener.getId()));
    }
    
    /**
     * Add the possibility for configuring the persistent admin to 
     * NOT insert space elements if they already exist in space.
     * */ 
    public void testThatElementsAreNotRegisteredTwice() throws InterruptedException {
        SemiSpacePersistentAdmin pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 60*1000, false, 60*1000);
        assertTrue(pa.hasBeenInitialized());
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        nvq.value = "Element not registered twice";
        assertNull( space.takeIfExists(nvq) );
        space.write(nvq, 61 * 1000);
        Thread.sleep(200);
        pa.shutdownAndAwaitTermination();
        pa.removeShutDownHook();

        // Reload space - this should not re-insert element as it is already present.
        pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 60*1000, false, 60*1000);
        pa.performInitialization();
        assertTrue(pa.hasBeenInitialized());
        Thread.sleep(100);

        assertNotNull("First take, element should exist", space.takeIfExists(nvq));
        assertNull( "Second take, element should not exist as duplicates are not inserted.", space.takeIfExists(nvq));

        pa.shutdownAndAwaitTermination();
        pa.removeShutDownHook();
    }
    
    /**
     * The space may contain eligible element present before startup of the persistent space.
     * Try to find them for inclusion in the persistent set. This is useful for
     * re-populating database after a "slight" downtime.
     */
    public void testInsertionOfQualifiedElementsAlreadyPresent() throws InterruptedException {
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        nvq.value = "persistence of elements already present";
        // Register listener
        JunitEventListener junitEventListener = new JunitEventListener();
        SemiEventRegistration listener = space.notify(nvq, junitEventListener, 9000); 
        space.write(nvq, 60 * 1000);
        Thread.sleep(50);
        assertTrue( junitEventListener.getId() > 0);
        
        SemiSpacePersistentAdmin pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 1000, false, 1000);
        assertTrue(pa.hasBeenInitialized());
        // Need to sleep in order to force insertion.
        Thread.sleep(1200);
        
        HibernateLeaseDao leaseDao = pa.getDbservice().getLeaseDao();
        Lease leaseDto = leaseDao.retrieveleaseByHolderId(junitEventListener.getId());
        assertNotNull("Lease with holder id "+junitEventListener.getId()+" should be found in database as it was " +
                "an existing element that should be inserted as part of the sync process.", leaseDto);

        assertNotNull("Element should be present for removal.", space.takeIfExists(nvq));
        assertNull( "After removal, the element is not present in DB.", leaseDao.retrieveleaseByHolderId(junitEventListener.getId()));
        
        // Shutdown
        listener.getLease().cancel();
        pa.shutdownAndAwaitTermination();
        pa.removeShutDownHook();
    }
    
    public void testRemovalAtApproximatelyTheSameTimeAsInsertion() {
        SemiSpacePersistentAdmin pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 180, false, 1000);
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        nvq.value = "mass insertion test";
        
        int counter = 0;
        long startTime = System.currentTimeMillis();
        while ( startTime > System.currentTimeMillis() - 250 ) {
            space.write(nvq, 120000);
            counter++;
        }
        log.debug("Inserted "+counter+" elements");
        int takenCounter = 0;
        while ( space.take( nvq, 200 ) != null ) {
            takenCounter++;
        }
        log.debug("Total running time "+(System.currentTimeMillis() - startTime));
        
        assertEquals("Should be able to take as many elements as was inserted.", counter, takenCounter);

        pa.shutdownAndAwaitTermination();
        pa.removeShutDownHook();
    }
    
    public void testTimeoutOfElement() throws InterruptedException {
        SemiSpacePersistentAdmin pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(space, 100, false, 300);
        HibernateLeaseDao leaseDao = pa.getDbservice().getLeaseDao();
        assertTrue(pa.hasBeenInitialized());
        NameValueQuery nvq = new NameValueQuery();
        nvq.name = "junit";
        
        final long beforeSize = leaseDao.size();
        
        for ( int i=0 ; i < 50 ; i++ ) {
            nvq.value = "Testing lease timeout - element "+i;
            space.write(nvq, 750);
        }
        // In order to let DB catch up, and begin to insert the elements
        Thread.sleep(350);
        
        long itSize = 0;
        do {
            itSize = leaseDao.size();
            Thread.sleep(250);
        } while ( itSize != leaseDao.size());
        
        assertEquals("After stabilization, elements should have been inserted, but not purged.", beforeSize + 50, leaseDao.size());
        
        Thread.sleep(350);
        space.harvest();
        Thread.sleep(350);
        assertNull(space.takeIfExists(nvq));
        assertEquals("After harvest, the elements should decrease to number before.", beforeSize, leaseDao.size());
        
        pa.removeShutDownHook();
    }

}
