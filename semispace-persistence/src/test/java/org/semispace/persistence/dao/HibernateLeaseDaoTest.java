/*
 * ============================================================================
 *
 *  File:     HibernateLeaseDaoTest.java
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
 *  Created:      Apr 6, 2008
 * ============================================================================ 
 */

package org.semispace.persistence.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.semispace.Holder;
import org.semispace.persistence.DatabaseFactory;
import org.semispace.persistence.DatabaseService;
import org.semispace.persistence.dao.HibernateLeaseDao;
import org.semispace.persistence.dao.LeaseMeta;
import org.semispace.persistence.domain.Lease;
import org.semispace.persistence.domain.Tag;


public class HibernateLeaseDaoTest extends TestCase {
    //private static final Logger log = LoggerFactory.getLogger(HibernateLeaseDaoTest.class);
    
    private HibernateLeaseDao leaseDao = null;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        DatabaseService db = DatabaseFactory.retrieveDatabaseService();
        leaseDao = db.getLeaseDao();
    }

    /**
     * Test method for 'org.semispace.persistence.dao.HibernateLeaseDao.savelease(Lease)'
     */
    public void testSavelease() {
        Lease lease = new Lease();
        lease.setDoctype("junit type");
        lease.setActual("Actual test content");
        lease.setLiveUntil(System.currentTimeMillis() + 1000);
        Set<Tag> tags = new HashSet<Tag>();
        
        Tag tag1 = new Tag();
        tag1.setName("a");
        tag1.setContent("a - junit");
        //tag1.setLease(lease);


        Tag tag2 = new Tag();
        tag2.setName("b");
        tag2.setContent("b - junit");
        //tag2.setLease(lease);
        tags.add(tag2);
  
        tags.add(tag1);
        lease.setTags(tags);
        
        leaseDao.savelease(lease);
        Lease read = leaseDao.retrieveleaseById(lease.getId());
        assertNotNull(read);
        assertEquals(2, read.getTags().size());

        // Now add another lease
        Tag tag3 = new Tag();
        tag3.setName("c");
        tag3.setContent("c - junit");
        read.getTags().add(tag3);
        leaseDao.savelease(read);
        assertEquals(3, read.getTags().size());
                
        leaseDao.deleteleaseById(lease.getId());
        assertNull( leaseDao.retrieveleaseById(lease.getId()));
    }

    public void testSaveOnlyLease() {
        Lease lease = new Lease();
        lease.setDoctype("junit type");
        lease.setActual("Actual test content");
        lease.setLiveUntil(System.currentTimeMillis() + 1000);
        lease.setTags(new HashSet<Tag>());
                
        leaseDao.savelease(lease);
        Lease read = leaseDao.retrieveleaseById(lease.getId());
        assertNotNull(read);
        assertEquals(0, read.getTags().size());
        
        leaseDao.deleteleaseById(lease.getId());
        assertNull( leaseDao.retrieveleaseById(lease.getId()));
    }

    public void testCleanupAfterSave() {
        Lease lease = new Lease();
        lease.setDoctype("junit type");
        lease.setActual("Actual test content");
        lease.setLiveUntil(System.currentTimeMillis() + 2000);
        Set<Tag> tags = new HashSet<Tag>();
        
        Tag tag1 = new Tag();
        tag1.setName("a");
        tag1.setContent("Cleanup test");
        //tag1.setLease(lease);
        
        tags.add(tag1);
        lease.setTags(tags);
        
        leaseDao.savelease(lease);
        leaseDao.cleanup();
        Lease read = leaseDao.retrieveleaseById(lease.getId());
        assertNotNull("Lease with id "+lease.getId()+" should exist even if cleanup was just called. Leasetime: "+lease.getLiveUntil()+", system time: "+System.currentTimeMillis(), read);
        
        int leaseId = lease.getId();
        leaseDao.deleteleaseById(leaseId );
        DatabaseFactory.retrieveDatabaseService().getLeaseDao().getHibernateTemplate().flush();
        Lease reread = DatabaseFactory.retrieveDatabaseService().getLeaseDao().retrieveleaseById(leaseId);
        assertNull("Presumed not to be able to retrieve lease, but got lease with id "+leaseId+" and (old) contents "+lease.getActual(), 
                reread );
    }


    /**
     * Will only cleanup if there is something to do, of course.
     */
    public void testCleanup() {
        leaseDao.cleanup();
    }
    
    public void testInversionOfHolderId() {
        Holder holder = new Holder("Just testing", System.currentTimeMillis() + 1000, "xxx.aaa", 1000, new HashMap());
        LeaseMeta lm = LeaseMeta.createLeaseMeta(holder);
        Lease lease = leaseDao.write(lm, holder.getLiveUntil());
        leaseDao.invertHolderIds();
        lease = leaseDao.retrieveleaseById(lease.getId());
        assertEquals(-1000, lease.getHolderId());
        
        Integer[] ids = leaseDao.retriveLeaseIdsForAllNegativeHolderIds();
        boolean found = false;
        for ( int i=0 ; !found && i < ids.length ; i++) {
            Lease dblease = leaseDao.retrieveleaseById(ids[i].intValue());
            if ( dblease.getActual().equals(holder.getXml())) {
                found = true;
            }
        }
        leaseDao.deleteleaseById(lease.getId());
        if ( ! found ) {
            fail("Did not get lease with id "+lease.getId()+" as one of the elements that should be found with negative holder id.");
        }
    }
}
