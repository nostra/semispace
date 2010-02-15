/*
 * ============================================================================
 *
 *  File:     HibernateLeaseDao.java
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.persistence.domain.Lease;
import org.semispace.persistence.domain.Tag;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;


public class HibernateLeaseDao extends HibernateDaoSupport {
    private static final Logger log = LoggerFactory.getLogger(HibernateLeaseDao.class);
    
    public Lease savelease(Lease lease) {
        if ( lease == null ) {
            return null;
        }
        try {
            getHibernateTemplate().saveOrUpdate(lease);
        } catch ( RuntimeException e ) {
            log.error("Could not save element.", e);
            throw e;
        }
        
        return lease;
    }

    public void deleteleaseById(int id) {
        Lease lease = retrieveleaseById(id);
        if ( lease != null ) {
            getHibernateTemplate().delete(lease);
        }
    }

    public Lease retrieveleaseById(int id) {
        Lease fetched = null;
        try {
            fetched = (Lease) getHibernateTemplate().load(Lease.class, Integer.valueOf(id));
            getHibernateTemplate().flush();
        } catch ( ObjectRetrievalFailureException orfe ) {
            log.debug("masking not found exception, as this may happen when searching for non-existing id. Returning null. (Masked) exception "+orfe.getLocalizedMessage());
        }
        return fetched;
    }

    public Lease write( LeaseMeta leaseMeta, long timeoutInSystime ) {
        Lease lease = new Lease();
        lease.setLiveUntil(timeoutInSystime);
        lease.setActual(leaseMeta.getXml());
        lease.setDoctype(leaseMeta.getDoctype());
        lease.setTags(leaseMeta.getTags());
        lease.setHolderId(leaseMeta.getHolderId());
        /*for ( Iterator it = leaseMeta.getTags().iterator() ; it.hasNext() ; ) {
            Tag next = (Tag) it.next();
            next.setLease(lease);
        }*/
        
        return savelease( lease );
    }

    /**
     * Notice that you may have more than one result, even when only one is returned
     */
    public Lease query( LeaseMeta leaseMeta ) {
        return query( leaseMeta.getTags(), leaseMeta.getDoctype(), false );
    }
    
    public Lease take( LeaseMeta leaseMeta) {
        Lease result = query( leaseMeta.getTags(), leaseMeta.getDoctype(), true );
        
        return result;
    }
    
    /**
     * Housekeeping method. Should only be called during startup.
     */
    @SuppressWarnings("unchecked")
    public void cleanup() {
        String sql = "select lease.id from Lease lease where liveUntil < :timenow";
        List<Integer> qres = getHibernateTemplate().findByNamedParam(sql, "timenow", Long.valueOf( new Date().getTime()) );
        List<Integer> dels = new ArrayList<Integer>();
        for ( Integer id : qres ) {
            try {
                deleteleaseById(id.intValue());
                dels.add(id);
            } catch ( Exception e ) {
                // Ignore
            }
        }
        if ( dels.size() > 0 ) {
            log.info("Deleted "+dels.size()+" lease elements which has either timed out or been taken / removed from the space. Id list: "+dels);
        }
    }
    
    /**
     * 
     */
    @SuppressWarnings("unchecked")
    private Lease query( Set<Tag> tags, String doctype, boolean isToTake ) {
        Set<Integer> candidates = null;

        String sql = "select lease.id from Lease as lease join lease.tags as tag where "
            +"lease.doctype=:doctype and "
            +"tag.name = :name and tag.content = :value and "
            +"lease.liveUntil >= :timenow";
        for ( Tag tag : tags ) {
             String[] paramNames = {"name", "value", "doctype", "timenow"};

            Object[] objs = { tag.getName(), tag.getContent(), doctype, Long.valueOf( new Date().getTime()) };
            List<Integer> qres = getHibernateTemplate().findByNamedParam(sql, paramNames, objs);
            // log.debug("Intermediate result: Ids: "+qres.toString());
            if ( candidates == null ) {
                candidates = new HashSet<Integer>( qres );
            } else {
                // Performing intersection
                candidates.retainAll( new HashSet<Integer>(qres ));
            }
            // log.info("Set of elements is now: "+candidates);
        }
        if ( candidates == null || candidates.isEmpty() ) {
            return null;
        }
        String leasesql = "from Lease lease where id in (:leaseids)";
        List<Lease> qres = getHibernateTemplate().findByNamedParam(leasesql, "leaseids", candidates);
        
        //Integer result = null;
        Lease resultLease = null;
        if ( qres.size() > 0 ) {
            resultLease = qres.get(0);
        }
        if ( resultLease == null ) {
            return null;
        } else  if ( isToTake ) {
            deleteleaseById(resultLease.getId());
            return resultLease;
        }
  
        return savelease(resultLease);
    }
    
    /**
     * Return the number of leases present in the database.
     */
    public long size() {
        long size = ( (Long)getHibernateTemplate().find("select count(*) from Lease").iterator().next() ).longValue();
        return size;
    }

    /**
     * @return If the holder is in the database, it is returned, even if it may have been expired.
     */
    @SuppressWarnings("unchecked")
    public Lease retrieveleaseByHolderId(long holderId) {
        String sql = "from Lease where "
            +"holderId=:holderId";
        String[] paramNames = {"holderId"};

        Object[] objs = { Long.valueOf(holderId) };
        List<Lease> qres = getHibernateTemplate().findByNamedParam(sql, paramNames, objs);
        if ( qres == null || qres.size() < 1 ) {
            return null;
        } else if ( qres.size() > 1 ) {
            throw new RuntimeException("Did not expect to have more than one of holder id "+holderId+". Database inconsistency, please clean up.");
        }
        return qres.get(0);
    }

    /**
     * Negate all holder IDs in order to "mask" them away from active use.
     */
    public int invertHolderIds() {
        String sql = "update Lease set holderId=-holderId where holderId > 0";
        int numberChanged = getHibernateTemplate().bulkUpdate(sql);
        return numberChanged;
    }
    
    @SuppressWarnings("unchecked")
    public Integer[] retriveLeaseIdsForAllNegativeHolderIds() {
        String sql = "select id from Lease where holderId < 0";
        List<Integer> qres = getHibernateTemplate().find(sql);
        return qres.toArray(new Integer[0]);
    }
}
