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
        boolean shallDisableWrite = checkContextForWriteDisabling("disableWrite", "write");
        boolean shallDisableTake = checkContextForWriteDisabling("disableTake", "take");
        SemiSpace space = (SemiSpace) SemiSpace.retrieveSpace();
        BayeuxServer bayeux = (BayeuxServer)getServletContext().getAttribute(BayeuxServer.ATTRIBUTE);

        rs = new ReadService(bayeux, space);
        rs.setSeeOwnPublishes(false);

        if ( !shallDisableTake) {
            ts = new TakeService(bayeux, space);
            ts.setSeeOwnPublishes(false);
        }

        if ( !shallDisableWrite ) {
            ws = new WriteService(bayeux, space);
            ws.setSeeOwnPublishes(false);
        }

        ns = new NotificationService(bayeux, space);
        ns.setSeeOwnPublishes(false);

        lcs = new LeaseCancellationService(bayeux);
        lcs.setSeeOwnPublishes(false);
        // Does not really seem like the extensions give anything I need
        //bayeux.addExtension(new TimesyncExtension());
        //bayeux.addExtension(new AcknowledgedMessagesExtension());
    }

    private boolean checkContextForWriteDisabling(String bs, String serviceName) {
        String disable = getInitParameter(bs);
        if ( "true".equalsIgnoreCase( disable )) {
            log.info("Parameter "+bs+" is true, and "+serviceName+" service will be disabled, i.e. no external clients can invoke "+serviceName+".");
            return true;
        } else if ( disable != null && !"false".equalsIgnoreCase("false")) {
            log.warn("Parameter "+bs+" is set, but with an illegal value ("+disable+"). Use true or false as values. Service "+serviceName+" is enabled.");
        } else {
            log.info("Parameter "+bs+" is not set or false, and "+serviceName+" is enabled. This is the default behaviour.");
        }
        return false;
    }

    @Override
    public void destroy() {
        super.destroy();
        if ( ws != null ) {
            ws.getServerSession().disconnect();
        }
        if ( ts != null ) {
            ts.getServerSession().disconnect();
        }
        rs.getServerSession().disconnect();
        ns.getServerSession().disconnect();
        lcs.getServerSession().disconnect();
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        throw new ServletException(getClass().getName()+" does not support any services");
    }
}
