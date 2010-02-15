/*
 * ============================================================================
 *
 *  File:     HibernateTagDaoTest.java
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

import org.semispace.persistence.DatabaseFactory;
import org.semispace.persistence.DatabaseService;
import org.semispace.persistence.domain.Tag;

import junit.framework.TestCase;

public class HibernateTagDaoTest extends TestCase {

    public void testSaveTag() {
        Tag tag = new Tag();
        tag.setContent("Junit - delete if found");
        tag.setName("junit");
        DatabaseService db = DatabaseFactory.retrieveDatabaseService();
        assertNotNull( db );
        assertNotNull( db.getTagDao());
        /*
        db.getTagDao().saveTag(tag);
        assertTrue( tag.getId() > 0 );
        db.getTagDao().deleteTagById( tag.getId());
        */
    }
}
