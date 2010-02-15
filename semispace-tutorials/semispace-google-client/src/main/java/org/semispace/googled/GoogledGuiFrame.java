/*
 * ============================================================================
 *
 *  File:     GoogledGuiFrame.java
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
 *  Created:      7. des.. 2008
 * ============================================================================ 
 */

package org.semispace.googled;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.semispace.SemiSpaceInterface;
import org.semispace.googled.external.ExternalSearchActor;
import org.semispace.googled.external.TokenProxyActor;
import org.semispace.googled.internal.SwingActorLogin;
import org.semispace.googled.internal.SwingActorSearch;
import org.semispace.ws.client.SemiSpaceTokenProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swixml.SwingEngine;

public class GoogledGuiFrame {
    private static final Logger log = LoggerFactory.getLogger(GoogledGuiFrame.class);
    
    private JFrame frame;
    public JPanel cardPanel;
    private JTextField username;
    private JTextField password;
    private JTextField searchField;
    private JTextArea searchResult;
    
    public Action quit = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            frame.dispose();
            System.exit(0);
        }        
    };
    
    public Action login= new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            log.info("Login pressed - got "+username.getText()+" "+password.getText());
            SwingActorLogin enabler = new SwingActorLogin( this, cardPanel );
            enabler.performLogin(username.getText(), password.getText());
        }        
    };

    public Action search= new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            log.info("Search pressed - searching for "+searchField.getText());
            SwingActorSearch searchActor = new SwingActorSearch( this, searchResult );
            searchActor.doSearch(searchField.getText());
        }        
    };

    /**
     * Open window and start app
     */
    protected void openAndStart(final SemiSpaceInterface externalSpace ) {
        if ( externalSpace == null ) {
            log.error("Need a semispace connection to be useful.");
            return;
        }
        SwingEngine engine = new SwingEngine(this);
        Container container;
        try {
            container = engine.render("swixml/googled.xml");
        } catch (Exception e) {
            log.error("Got exception starting with defined window - just returning", e);
            return;
        }

        Container root = engine.getRootComponent();
        if (root instanceof JFrame) {
            final JFrame frame = (JFrame) root;
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent evt) {
                    frame.dispose();
                    System.exit(0);
                }
            });

        } else {
            log.warn("The root element is not not JFrame as presumed. Cannot add closing event.");
        }

        if ( externalSpace instanceof SemiSpaceTokenProxy ) {
            new TokenProxyActor( (SemiSpaceTokenProxy) externalSpace );
        } else {
            ((CardLayout)cardPanel.getLayout() ).show(cardPanel, "searchPanel");
        }
        new ExternalSearchActor( externalSpace );
        
        
        container.setVisible(true);
    }

}
