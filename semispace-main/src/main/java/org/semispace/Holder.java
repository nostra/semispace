/*
 * ============================================================================
 *
 *  File:     Holder.java
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
 *  Created:      24. des.. 2007
 * ============================================================================ 
 */

package org.semispace;

import java.util.Map;

/**
 * Holder of an entry written into the space.
 */
public class Holder {

    private long liveUntil;
    private String xml;
    private long id;
    private Map<String, String> searchMap;
    private String className;
    public long getId() {
        return this.id;
    }

    public Holder(String xml, long liveUntil, String className, long id, Map<String, String> map) {
        this.xml = xml;
        this.liveUntil = liveUntil;
        this.id = id;
        this.searchMap = map;
        this.className = className;
    }

    public String getXml() {
        return this.xml;
    }

    public synchronized long getLiveUntil() {
        return this.liveUntil;
    }

    public Map<String, String> getSearchMap() {
        return this.searchMap;
    }

    public String getClassName() {
        return className;
    }

    protected synchronized void setLiveUntil(long liveUntil) {
        this.liveUntil = liveUntil;
    }

}
