/*
 * ============================================================================
 *
 *  File:     HibernateTagDao.java
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.persistence.domain.Tag;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;


public class HibernateTagDao extends HibernateDaoSupport {
    private static final Logger log = LoggerFactory.getLogger(HibernateTagDao.class);
 
    public Tag saveTag(Tag tag) {
        if ( tag == null ) {
            return null;
        }
        getHibernateTemplate().saveOrUpdate(tag);
        return tag;
    }

    public void deleteTagById(int id) {
        Tag Tag = retrieveTagById(id);
        if ( Tag != null ) {
            getHibernateTemplate().delete(Tag);
        }
    }

    public Tag retrieveTagById(int id) {
        Tag fetched = null;
        try {
            fetched = (Tag) getHibernateTemplate().load(Tag.class, Integer.valueOf(id));
        } catch ( ObjectRetrievalFailureException orfe ) {
            log.debug("masking not found exception, as this may happen when searching for non-existing id. Returning null. (Masked) exception "+orfe.getLocalizedMessage());
        }
        return fetched;
    }
}
