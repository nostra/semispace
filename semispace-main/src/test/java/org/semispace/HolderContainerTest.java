/*
 * ============================================================================
 *
 *  File:     HolderContainerTest.java
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
 *  Created:      Apr 27, 2008
 * ============================================================================ 
 */

package org.semispace;

import junit.framework.TestCase;

public class HolderContainerTest extends TestCase {
    private long id = 0;
    
    public void testAlmostEmptyHolderContainer() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        Holder a = createHolder();
        final int orgsize = hc.size();
        hc.addHolder(a);
        assertEquals(orgsize+1, hc.size());
        
        assertNull(hc.findById(id+100, a.getClassName()));
        assertNotNull(hc.findById(a.getId(), a.getClassName()));
        
        assertNotNull( hc.removeHolderById(a.getId(), a.getClassName()) );
        assertEquals(orgsize, hc.size());
        assertNull( hc.removeHolderById(id+100, a.getClassName()) );
    }

    public void testHolderContainerWith2Elements() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        Holder a = createHolder();
        Holder b = createHolder();
        final int orgsize = hc.size();
        
        hc.addHolder(a);
        hc.addHolder(b);
        assertEquals(orgsize+2, hc.size());
        
        assertNotNull(hc.findById(a.getId(), a.getClassName()));
        assertNotNull(hc.findById(b.getId(), b.getClassName()));

        assertTrue(a.getId() != b.getId());
        
        assertTrue(hc.next(a.getClassName()) != null);
        
        assertNotNull( hc.removeHolderById(a.getId(), a.getClassName()) );
        assertEquals(orgsize+1, hc.size());
        assertTrue("Size 1 indicates that next is present.", hc.next(b.getClassName()) != null);
        assertEquals(b.getId(), hc.next(b.getClassName()).getHolder().getId());
        assertNotNull( hc.removeHolderById(b.getId(), b.getClassName()) );
        assertTrue(hc.next(b.getClassName()) == null );
        assertEquals(orgsize, hc.size());

        assertNull(hc.findById(a.getId(), b.getClassName()));
        assertNull(hc.findById(b.getId(), b.getClassName()));
        
    }

    
    public void testHolderContainer() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        Holder a = createHolder();
        Holder b = createHolder();
        Holder c = createHolder();
        Holder d = createHolder();
        final int orgsize = hc.size();
        
        hc.addHolder(a);
        hc.addHolder(b);
        hc.addHolder(c);
        hc.addHolder(d);
        assertEquals(orgsize + 4, hc.size());
        
        assertNull(hc.findById(id+100, a.getClassName()));
        assertNotNull(hc.findById(a.getId(), a.getClassName()));
        assertNotNull(hc.findById(b.getId(), a.getClassName()));
        assertNotNull(hc.findById(c.getId(), a.getClassName()));
        assertNotNull(hc.findById(d.getId(), a.getClassName()));
        
        assertNotNull( hc.removeHolderById(b.getId(), a.getClassName()) );
        assertEquals(orgsize + 3, hc.size());
        
        assertNotNull(hc.findById(a.getId(), a.getClassName()));
        assertNull(hc.findById(b.getId(), a.getClassName()));
        assertNotNull(hc.findById(c.getId(), a.getClassName()));
        assertNotNull(hc.findById(d.getId(), a.getClassName()));
        
        assertNotNull( hc.removeHolderById(d.getId(), a.getClassName()) );

        assertEquals(orgsize + 2, hc.size());
        assertNotNull(hc.findById(a.getId(), a.getClassName()));
        assertNull(hc.findById(b.getId(), a.getClassName()));
        assertNotNull(hc.findById(c.getId(), a.getClassName()));
        assertNull(hc.findById(d.getId(), a.getClassName()));
        
        assertNull( hc.removeHolderById(d.getId(), a.getClassName()) );
        
        assertNotNull( hc.removeHolderById(a.getId(), a.getClassName()) );
        assertNotNull( hc.removeHolderById(c.getId(), a.getClassName()) );
        
        assertEquals(orgsize, hc.size());
        
    }

    private Holder createHolder() {
        Holder holder = new Holder("<x>dummy-"+id+"</x>", System.currentTimeMillis()+(60*60*1000), "x", id, null);
        id++;
        return holder;
    }

    public void testIdentityOfRemovedElement() {
        HolderContainer hc = HolderContainer.retrieveContainer();
        Holder a = createHolder();
        Holder b = createHolder();
        Holder c = createHolder();
        Holder d = createHolder();
        final int orgsize = hc.size();
        
        hc.addHolder(a);
        hc.addHolder(b);
        hc.addHolder(c);
        hc.addHolder(d);
        assertEquals(orgsize + 4, hc.size());
        
        Holder x = hc.removeHolderById(b.getId(), b.getClassName());
        assertEquals(b.getId(), x.getId());
        Holder y = hc.removeHolderById(a.getId(), a.getClassName());
        assertEquals(a.getId(), y.getId());
        Holder z = hc.removeHolderById(d.getId(), d.getClassName());
        assertEquals(d.getId(), z.getId());
        
        assertEquals(orgsize + 1, hc.size());
        Holder h = hc.removeHolderById(c.getId(), c.getClassName());
        assertEquals(c.getId(), h.getId());
        assertEquals(orgsize, hc.size());
    }
}
