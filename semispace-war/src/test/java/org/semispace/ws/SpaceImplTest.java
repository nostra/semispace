/*
 * ============================================================================
 *
 *  File:     SpaceImplTest.java
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
 *  Created:      Mar 5, 2008
 * ============================================================================ 
 */

package org.semispace.ws;

import org.semispace.Holder;
import org.semispace.SemiSpace;
import org.semispace.ws.WsSpaceImpl;

import junit.framework.TestCase;

public class SpaceImplTest extends TestCase {
    private WsSpaceImpl space;
    private SemiSpace semispace;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        space = new WsSpaceImpl();
        semispace = (SemiSpace) SemiSpace.retrieveSpace();
        space.setSpace(semispace);
    }

    public void testSimpleXml() {
        String entry = "<contents><a>a</a><b>b</b></contents>";
        String queryOk = "<contents><a>a</a></contents>";
        String queryNoMatch = "<contents><a>X</a></contents>";
        space.write(entry, 1000 );
        assertNull("No match - should not give any results.", space.readIfExists(queryNoMatch));
        assertEquals("Match - should find something which is equal to entry.", entry, space.readIfExists(queryOk));        
        assertNotNull("Should be able to be able to remove entry", space.takeIfExists(entry));
        assertNull("Should not be able to take the element twice", space.takeIfExists(entry));
    }
    
    public void testTwoDifferentXmls() {
        String entry = "<contents><a>a</a><b>b</b></contents>";
        String different = "<different><a>a</a><b>b</b></different>";
        String query = "<contents><b>b</b></contents>";
        space.write(entry, 100000 );
        space.write(different, 100000 );
        
        assertEquals("Match - should find something which is equal to entry with query: "+query, entry, space.takeIfExists(query));
        assertEquals("Should NOT find something with match twice with query "+query,null,space.takeIfExists(query));
        
        assertNotNull("Should be able to be able to remove the other entry", space.takeIfExists(different));
    }

    public void testRetrievePropertiesFromXml() {
        String entry = "<contents><a>a</a><b>b</b></contents>";
        String different = "<different><a>a</a><b>b</b></different>";

        Holder propsa = space.retrievePropertiesFromXml( entry );
        Holder propsb = space.retrievePropertiesFromXml( different );
        assertEquals("contents", propsa.getClassName());
        assertEquals("different", propsb.getClassName());
        assertFalse("This test will run red when having changed space to contain different classes in different maps.", 
                propsa.getSearchMap().entrySet().containsAll(propsb.getSearchMap().entrySet()));
    }
    
    public void testEqualityOfInterfaces() {
        JunitWsHolder holder = new JunitWsHolder();
        holder.a = "a";
        holder.b = "b";
        String entry = "<org.semispace.ws.JunitWsHolder><a>a</a><b>b</b></org.semispace.ws.JunitWsHolder>";
        space.write(entry, 100000 );
        assertNotNull(semispace.readIfExists(holder));
        assertNotNull(semispace.takeIfExists(holder));
        semispace.write( holder, 10000 );
        assertNotNull(space.takeIfExists(entry));
    }
}
