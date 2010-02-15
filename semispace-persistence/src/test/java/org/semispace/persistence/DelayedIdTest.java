/*
 * ============================================================================
 *
 *  File:     DelayedIdTest.java
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
 *  Created:      Jun 12, 2008
 * ============================================================================ 
 */

package org.semispace.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.semispace.persistence.DelayedId;

import junit.framework.TestCase;

public class DelayedIdTest extends TestCase {

    public void testCompareToDelayeOnId() {
        DelayedId delayedId1 = new DelayedId(1, 1000);
        DelayedId delayedId2 = new DelayedId(2, 1000);
        DelayedId delayedId3 = new DelayedId(3, 1000);
        DelayedId arr[] = {delayedId3,delayedId1,delayedId2};
        Arrays.sort(arr );
        assertEquals(1, arr[0].getId());
        assertEquals(2, arr[1].getId());
        assertEquals(3, arr[2].getId());
    }

    public void testCompareToDelayedOnTime() {
        DelayedId delayedId1 = new DelayedId(1, 1000);
        DelayedId delayedId2 = new DelayedId(2, 2000);
        DelayedId delayedId3 = new DelayedId(3, 3000);
        DelayedId arr[] = {delayedId3,delayedId1,delayedId2};
        Arrays.sort(arr );
        assertEquals(1, arr[0].getId());
        assertEquals(2, arr[1].getId());
        assertEquals(3, arr[2].getId());
    }

    
    public void testDelayInSet() throws InterruptedException {
        Set<DelayedId> set = new HashSet<DelayedId>();
        DelayedId other = new DelayedId(2, 2000);
        Thread.sleep(10);
        set.add( new DelayedId(2, 2000) );
        assertTrue(set.remove(other));
    }

    public void testDelayInList() throws InterruptedException {
        ArrayList<DelayedId>list = new ArrayList<DelayedId>();
        list.add( new DelayedId(2, 2000) );
        Thread.sleep(10);
        DelayedId other = new DelayedId(2, 2000);
        assertTrue(list.remove(other));
    }
    
}
