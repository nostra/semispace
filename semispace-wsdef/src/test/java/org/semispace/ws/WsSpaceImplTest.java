/*
 * ============================================================================
 *
 *  File:     WsSpaceImplTest.java
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
 *  Created:      Jun 18, 2008
 * ============================================================================ 
 */

package org.semispace.ws;

import junit.framework.TestCase;

import org.semispace.SemiSpace;
import org.semispace.ws.WsSpaceImpl;

public class WsSpaceImplTest extends TestCase {
    WsSpaceImpl wsSpace;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        wsSpace = new WsSpaceImpl();
        wsSpace.setSpace((SemiSpace) SemiSpace.retrieveSpace());
    }

    public void testRetrievePropertiesFromXml() {
        
        // Just using a generated test element found elsewhere
        String xml = "<org.semispace.space.semispaceTest-FieldHolder>"
                +"<fieldA>a</fieldA>"
                +"<fieldB>b</fieldB>"
                +"<nested>"
                +"<fName>testWrite</fName>"
                +"</nested>"
                +"</org.semispace.space.semispaceTest-FieldHolder>";

        wsSpace.write(xml,  2500 );
        
        String q = "<org.semispace.space.semispaceTest-FieldHolder>"
            +"<fieldA>a</fieldA>"    
            +"</org.semispace.space.semispaceTest-FieldHolder>";
        
        String qres = wsSpace.readIfExists(q);
        assertNotNull("Expected to be able to query and get results with \n"+q, qres);
        String q3 = "<org.semispace.space.semispaceTest-FieldHolder>"
            +"<fieldA>NOT CONTAINED</fieldA>"    
            +"</org.semispace.space.semispaceTest-FieldHolder>";
        assertNull("Element should not have been registered.", wsSpace.readIfExists(q3));
        assertNotNull(wsSpace.takeIfExists(xml));
    }

    public void testXmlWith2Elements() {
        String prefix = "<org.semispace.space.semispaceTest-FieldHolder>"
                +"<fieldA>a</fieldA>"
                +"<fieldB>";
        String postfix = "</fieldB>"
                +"<nested>"
                +"<fName>testWrite</fName>"
                +"</nested>"
                +"</org.semispace.space.semispaceTest-FieldHolder>";

        String xml1 = prefix+"b"+postfix;
        String xml2 = prefix+"X"+postfix;
        
        wsSpace.write(xml2, 2500);
        wsSpace.write(xml1, 2500);
        
        String q1 = "<org.semispace.space.semispaceTest-FieldHolder>"
            +"<fieldA>a</fieldA>"    
            +"<fieldB>b</fieldB>"    
            +"</org.semispace.space.semispaceTest-FieldHolder>";
        String q2 = "<org.semispace.space.semispaceTest-FieldHolder>"
            +"<fieldA>a</fieldA>"    
            +"<fieldB>X</fieldB>"    
            +"</org.semispace.space.semispaceTest-FieldHolder>";
        
        String qres1 = wsSpace.readIfExists(q1);
        String qres2 = wsSpace.readIfExists(q2);
        assertNotNull(qres1);
        assertNotNull(qres2);
        assertTrue( "Expecting qres1 to contain fieldB b, got "+qres1, qres1.indexOf("fieldB>b") > 0 );
        assertTrue( "Exected content o contain X in fieldB, got: "+qres2, qres2.indexOf("fieldB>X") > 0 );
        // Testing again in case there is some problem with the sequence.
        qres2 = wsSpace.readIfExists(q2);
        assertTrue( "Expecting qres2 to contain fieldB X, got "+qres2, qres2.indexOf("fieldB>X") > 0 );
        
        assertNotNull(wsSpace.readIfExists(q1));
        assertNotNull(wsSpace.readIfExists(q2));
        assertNotNull(wsSpace.takeIfExists(q1));
        assertNotNull(wsSpace.takeIfExists(q2));
    }

    public void testQueryNonExisting() {
        // Just using a generated test element found elsewhere
        String xml = "<org.semispace.space.semispaceTest-FieldHolder>"
                +"<fieldA>a</fieldA>"
                +"<fieldB>b</fieldB>"
                +"</org.semispace.space.semispaceTest-FieldHolder>";

        for ( int i = 0 ; i < 10 ; i++ ) {
            assertNull( "Did not expect any residue in the space. Got "+xml, wsSpace.takeIfExists(xml ));
        }
    }

}
