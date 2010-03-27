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

import org.cometd.Bayeux;
import org.semispace.SemiSpace;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class SemiSpaceInitializer extends GenericServlet {
    private ReadService rs;
    private WriteService ws;
    private TakeService ts;
    private NotificationService ns;

    @Override
    public void init() throws ServletException {
        SemiSpace space = (SemiSpace) SemiSpace.retrieveSpace();
        Bayeux bayeux = (Bayeux)getServletContext().getAttribute(Bayeux.ATTRIBUTE);
        rs = new ReadService(bayeux, space);
        rs.setSeeOwnPublishes(false);
        ts = new TakeService(bayeux, space);
        ts.setSeeOwnPublishes(false);

        ws = new WriteService(bayeux, space);
        ws.setSeeOwnPublishes(false);

        ns = new NotificationService(bayeux, space);
        ns.setSeeOwnPublishes(false);
    }
    
    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        throw new ServletException(getClass().getName()+" does not support any services");
    }
}
