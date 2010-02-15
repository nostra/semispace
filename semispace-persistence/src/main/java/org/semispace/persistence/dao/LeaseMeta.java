/*
 * ============================================================================
 *
 *  File:     LeaseMeta.java
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.semispace.Holder;
import org.semispace.persistence.domain.Tag;


/**
 * Object which can contain everything needed for query or insertion into the database.
 */
public class LeaseMeta {
    //private static final Logger log = LoggerFactory.getLogger(LeaseMeta.class);
    
    private String doctype;
    private Set<Tag> tags;
    private String xml;

    private long holderId;
    
    public String getDoctype() {
        return this.doctype;
    }

    public Set<Tag> getTags() {
        return this.tags;
    }

    public String getXml() {
        return xml;
    }
    private LeaseMeta() {
        // Intentional
    }


    public static final LeaseMeta createLeaseMeta( Holder holder ) {
        LeaseMeta leaseMeta = new LeaseMeta();
        leaseMeta.doctype = holder.getClassName();
        leaseMeta.xml = holder.getXml();
        leaseMeta.tags = leaseMeta.createTagsFromMap( holder.getSearchMap());
        leaseMeta.holderId = holder.getId();
        return leaseMeta;
    }
        
    private Set<Tag> createTagsFromMap(Map<String, String> searchMap) {
        Iterator<Map.Entry<String,String>> keys = searchMap.entrySet().iterator();
        Set<Tag> ctags = new HashSet<Tag>(); 
        while ( keys.hasNext() ) {
            Map.Entry<String,String> entry = keys.next();
            String key = entry.getKey();
            String value = entry.getValue();
            Tag tag = new Tag();
            tag.setName( key );
            tag.setContent( value );
            ctags.add(tag);
        }
        return ctags;
    }
    protected long getHolderId() {
        return this.holderId;
    }
}
