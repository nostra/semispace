/*
 * ============================================================================
 *
 *  File:     ExampleOfActor.java
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
 *  Created:      Jul 19, 2008
 * ============================================================================ 
 */

package org.semispace.actor.example;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.actor.Actor;

public class ExampleOfActor {

    /**
     * @param args
     */
    public static void main(String[] args) {
        new ExampleOfActor().doMojo();
    }

    private void doMojo() {
        SemiSpaceInterface space = SemiSpace.retrieveSpace();
        Actor pong = new PongActor(space);
        //pong.register(space);
        
        PingActor ping = new PingActor(10000, space);

        ping.fireItUp();
    }

}
