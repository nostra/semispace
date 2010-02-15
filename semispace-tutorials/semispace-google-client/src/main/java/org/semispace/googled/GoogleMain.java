/*
 * ============================================================================
 *
 *  File:     GoogleMain.java
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
 *  Created:      Oct 19, 2008
 * ============================================================================ 
 */

package org.semispace.googled;

import javax.swing.SwingUtilities;

import org.semispace.SemiSpace;
import org.semispace.SemiSpaceInterface;
import org.semispace.google.transport.AddressQuery;
import org.semispace.ws.client.SemiSpaceTokenProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GoogleMain {
    private static final Logger log = LoggerFactory.getLogger(GoogleMain.class);

    /**
     * Internal holder of space 
     */
    private SemiSpaceInterface space;
    
    /**
     * If you supply an argument, the webservices-version will be used
     */
    public static void main(String[] args) {
        log.info("Starting");
        try {
            final GoogleMain main = new GoogleMain();
            if ( args.length > 0 ) {
                
                log.info("Using the following endpoint: "+args[0]);
                main.space = SemiSpaceTokenProxy.retrieveSpace(args[0]);
                if ( args.length > 2 ) {
                    ((SemiSpaceTokenProxy)main.space).setUsername(args[1]);
                    ((SemiSpaceTokenProxy)main.space).setPassword(args[2]);
                }
                
            } else {
                log.info("Presuming we have been started as Terracotta DSO app");
                main.space = SemiSpace.retrieveSpace();
            }
            //main.performTestQuery();
            main.startInThreadGroup();
        } catch (Exception e) {
            log.error("Got exception", e);
        }
    }

    private void startInThreadGroup() {
        ThreadGroup exceptionThreadGroup = new GuiThreadGroup();
        new Thread(exceptionThreadGroup, "Init thread") {
            @SuppressWarnings("synthetic-access")
            public void run() {
                try {
                    new GoogledGuiFrame().openAndStart(space);
                } catch (Exception e) {
                    throw new RuntimeException("Problems starting", e);
                }
            }
        }.start();
    }
    
    private void performTestQuery() {
        AddressQuery aq = new AddressQuery();
        aq.setAddress("Kongensgate 14, Oslo, Norway");
        space.write(aq, 2500);
    }



}
