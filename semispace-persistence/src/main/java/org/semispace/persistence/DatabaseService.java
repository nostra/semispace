/*
 * ============================================================================
 *
 *  File:     DatabaseService.java
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

package org.semispace.persistence;

import org.semispace.persistence.dao.HibernateLeaseDao;
import org.semispace.persistence.dao.HibernateTagDao;

/**
 * 
 */
public class DatabaseService {
    private HibernateTagDao tagDao;
    private HibernateLeaseDao leaseDao;
    
    public void setLeaseDao(HibernateLeaseDao leaseDao) {
        this.leaseDao = leaseDao;
    }
    public HibernateLeaseDao getLeaseDao() {
        return this.leaseDao;
    }
    public HibernateTagDao getTagDao() {
        return this.tagDao;
    }

    public void setTagDao(HibernateTagDao tagDao) {
        this.tagDao = tagDao;
    }
}
