/*
 * ============================================================================
 *
 *  File:     NotifyFromSpace.java
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
 *  Created:      Mar 30, 2008
 * ============================================================================ 
 */

package org.semispace.space.tutorial;

import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.event.SemiAvailabilityEvent;
import org.semispace.event.SemiEvent;

public class NotifyFromSpace implements SemiEventListener {
    public static void main(String[] args) {
        System.out.println("Awaiting notification of all objects of type Element");
        System.out.println("Is to be stopped with CTRL-C");

        new NotifyFromSpace().startUpAndWait();

    }

// START SNIPPET: exampleOfNotification
    public void notify(SemiEvent theEvent) {
        if ( theEvent instanceof SemiAvailabilityEvent) {
            System.out.println("Incoming element which concurs with template has arrived.");
            Element element = (Element) SemiSpace.retrieveSpace().takeIfExists( new Element());
            if ( element ==null ) {
                System.out.println("Could not take element that was flagged as available");
            } else {
                System.out.println("Read element from space: "+element.getName()+"="+element.getValue());
            }
        }
    }
// END SNIPPET: exampleOfNotification

    private void startUpAndWait() {
        try {
// START SNIPPET: startNotification
            SemiSpaceInterface space = SemiSpace.retrieveSpace();
            SemiEventRegistration eventRegistration = space.notify(new Element(), this, 60 * 1000 * 60);
// END SNIPPET: startNotification
// If this comment is seen in the doc, it is because mavens apt book generator has
// become confused with the code snippets.

            for ( int i=0 ; i < 10 ; i++) {
                try {
                    Thread.sleep(1000 * 10);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
            /* If you like to cancel the notification, perform
               the following: */
            eventRegistration.getLease().cancel();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }

    }

}
