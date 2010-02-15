/*
 * ============================================================================
 *
 *  File:     WsSpace.java
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
 *  Created:      Mar 4, 2008
 * ============================================================================ 
 */

package org.semispace.ws;

import javax.jws.WebService;

/**
 * Proxy interface for SemiSpace. Everything
 * defined as string is supplied as XML, instead of 
 * Objects. Otherwise it is equal to the SemiSpace 
 * interface.
 */
@WebService
public interface WsSpace {
    /**
     * Put XML structured data into space with given lifetime
     * @param contents XML data
     */
    public void write( String contents, long timeToLiveMs );
    /**
     * Supply an XML template, which will be matched on
     * <b>the first</b>. In other words, the following template
     * will be matched on <i>firstname</i> only:
     * <pre>
     * <person>
     *   <firstname>Erlend</firstname>
     *   <projects>
     *      <project>SemiSpace</project>
     *   </projects>
     * </pre>
     * The entry is left in the space
     * @param template XML template
     * @param queryLifeMs Life of query in milliseconds. Notice that
     * you will want to repeatedly read instead of having
     * a very long timeout (i.e. more than 30 seconds)
     */
    public String read( String template, long queryLifeMs );
    /**
     * Read without any timeout; You get an answer immediately
     * @see #read(String, long)
     */
    public String readIfExists( String template );
    /**
     * Same as read, except that the entry is removed from the space.
     * @see #read(String, long)
     */
    public String take( String template, long queryLifeMs );
    /**
     * @see #take(String, long)
     */
    public String takeIfExists( String template );
}
