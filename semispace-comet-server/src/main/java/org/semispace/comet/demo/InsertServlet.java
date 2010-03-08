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

package org.semispace.comet.demo;

import org.semispace.comet.client.SemiSpaceCometProxy;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Just insert something into the space
 */
public class InsertServlet extends HttpServlet {
    private SemiSpaceCometProxy space;

    @Override
    public void destroy() {
        super.destroy();
        space.destroy();
    }

    @Override
    public void init() throws ServletException {
        super.init();
        space = new SemiSpaceCometProxy();
        space.init("http://localhost:8080/semispace-comet-server/cometd/");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String param = req.getParameter("param");
        if ( param == null ) {
            param = "param not set";
        }
        FieldHolder fh = new FieldHolder();
        fh.setFieldA("InsertServlet");
        fh.setFieldB(param);
        space.write(fh, 15000);
        resp.setContentType("text/plain");
        resp.getWriter().write("Inserted following into space with a lifetime of 15 seconds:\n"+fh);
    }
}
