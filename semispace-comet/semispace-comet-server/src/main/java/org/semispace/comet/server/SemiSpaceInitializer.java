/*
 * Copyright 2010 Erlend Nossum
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
 */

package org.semispace.comet.server;

import org.cometd.bayeux.server.BayeuxServer;
import org.semispace.SemiSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class SemiSpaceInitializer extends GenericServlet {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceInitializer.class);
    private ReadService rs;
    private WriteService ws;
    private TakeService ts;
    private NotificationService ns;
    private LeaseCancellationService lcs;

    @Override
    public void init() throws ServletException {
        SemiSpace space = (SemiSpace) SemiSpace.retrieveSpace();
        BayeuxServer bayeux = (BayeuxServer)getServletContext().getAttribute(BayeuxServer.ATTRIBUTE);
        ts = new TakeService(bayeux, space);
        //new HelloService( bayeux );
        //log.debug("Initialization done.");
        ts.setSeeOwnPublishes(false);
        rs = new ReadService(bayeux, space);
        rs.setSeeOwnPublishes(false);

        ws = new WriteService(bayeux, space);
        ws.setSeeOwnPublishes(false);

        ns = new NotificationService(bayeux, space);
        ns.setSeeOwnPublishes(false);

        lcs = new LeaseCancellationService(bayeux);
        lcs.setSeeOwnPublishes(false);
        //bayeux.addExtension(new TimesyncExtension());
        //bayeux.addExtension(new AcknowledgedMessagesExtension());
    }

    @Override
    public void destroy() {
        super.destroy();
        /* TODO Fix disconnect
        rs.getClient().disconnect();
        ws.getClient().disconnect();
        ts.getClient().disconnect();
        ns.getClient().disconnect();
        lcs.getClient().disconnect();
        */
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        throw new ServletException(getClass().getName()+" does not support any services");
    }
}
