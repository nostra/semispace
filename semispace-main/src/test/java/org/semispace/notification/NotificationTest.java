/*
 * ============================================================================
 *
 *  File:     NotificationTest.java
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
 *  Created:      Feb 4, 2009
 * ============================================================================ 
 */

package org.semispace.notification;

import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class NotificationTest extends TestCase {
    private static final Logger log = LoggerFactory.getLogger(NotificationTest.class);
            
    public void testNotificationAndTake() throws InterruptedException {
        ToBeNotified a = new ToBeNotified(NoticeA.class, false);
        ToBeNotified b = new ToBeNotified(NoticeB.class, false);
        ToBeNotified c = new ToBeNotified(NoticeC.class, false);
        
        SemiSpaceInterface space = SemiSpace.retrieveSpace();
        SemiEventRegistration notifyA = space.notify(new NoticeA(), a, 10000);
        a.setNotify( notifyA );
        SemiEventRegistration notifyB = space.notify(new NoticeB(), b, 10000);
        b.setNotify( notifyB );
        SemiEventRegistration notifyC = space.notify(new NoticeC(), c, 10000);
        c.setNotify( notifyC );

        log.debug("Before inserting into space");
        for ( int i=0 ; i < 1500 ; i++ ) {
            insertIntoSpace(space, i);            
        }
        // May experience slight lag due to asynchronious operations.
        Thread.sleep(200);
        
        log.debug("Insertion finished");

        log.debug(a.getNotified()+" "+b.getNotified()+" "+c.getNotified());
        assertEquals( 1500, a.getNotified());
        assertEquals( 1500, b.getNotified());
        assertEquals( 1500, c.getNotified());
    }


    private void insertIntoSpace(SemiSpaceInterface space, int i) {
        NoticeA na = new NoticeA();
        na.setField("a "+i);
        space.write(na, 1000);
        
        NoticeB nb = new NoticeB();
        nb.setField("b "+i);
        space.write(nb, 1000);
        
        NoticeC nc = new NoticeC();
        nc.setField("c "+i);
        space.write(nc, 1000);
    }
}
