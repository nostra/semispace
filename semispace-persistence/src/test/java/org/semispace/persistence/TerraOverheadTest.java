/*
 * ============================================================================
 *
 *  File:     TerraOverheadTest.java
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
 *  Created:      Jun 15, 2008
 * ============================================================================ 
 */

package org.semispace.persistence;

import org.junit.Test;


/**
 * Test used within eclipse for testing that
 * the persistence API works with terracotta.
 * Cannot, unfortunately, be run as a stand alone
 * test.
 * 
 * <p>Extension of terracotta test in order to see an 
 * indication of how large the overhead is with persistence.
 * Note that the initialization is performed for each test, so
 * this is really just an indication - not a real result. 
 * </p>
 * 
 * <p>The
 * terracotta connection is also tested, in order to see that
 * it still works.</p> 
 */
public class TerraOverheadTest {// extends TerraSpaceTest {
    @Test
    public void testDummy() {
        // Nothing here...
    }
    //*
    /*/
    private SemiSpaceAdminInterface semiSpaceAdmin;
    private SemiSpacePersistentAdmin pa;
    
    @Override
    public void setUp()  {
        super.setUp();
        semiSpaceAdmin = getSpace().getAdmin();
        pa = SemiSpacePersistentAdmin.createConnectedAdminInstance(getSpace(), 20000, false, 60000);
        getSpace().setAdmin( pa);
        pa.performInitialization();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        getSpace().setAdmin(semiSpaceAdmin);
        pa.shutdownAndAwaitTermination();
        pa.removeShutDownHook();
    }
    // */
}
