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

package org.semispace.comet.client;

import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.component.LifeCycle;
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiLease;
import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SemiSpaceCometProxy implements SemiSpaceInterface {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceCometProxy.class);
    private BayeuxClient client;
    private HttpClient httpClient;

    // "http://localhost:8080/semispace-comet-server/cometd/"
    public void init(String endpoint) {
        httpClient = new HttpClient();
        try {
            httpClient.start();
            client = new SemiSpaceBayeuxClient(httpClient, endpoint);
            client.addLifeCycleListener(new ProxyLifeCycle());
            client.start();
        } catch (Exception e) {
            throw new RuntimeException("Could not start client", e);
        }
    }

    public void destroy() {
        client.disconnect();
        if ( httpClient != null ) {
            try {
                httpClient.stop();
            } catch (Exception e) {
                log.error("Problem stopping httpClient", e);
            }
        }

    }

    @Override
    public SemiLease write(Object obj, long duration) {        
        return null;
    }

    @Override
    public <T> T read(T template, long duration) {
        ReadClient read = new ReadClient();
        read.doRead(client);

        return null;
    }

    @Override
    public <T> T readIfExists(T template) {
        return null;  
    }

    @Override
    public <T> T take(T template, long duration) {
        return null;  
    }

    @Override
    public <T> T takeIfExists(T template) {
        return null;  
    }

    @Override
    public SemiEventRegistration notify(Object template, SemiEventListener listener, long duration) {
        return null;  
    }

    private class ProxyLifeCycle implements LifeCycle.Listener {
        @Override
        public void lifeCycleStarting(LifeCycle event) {
            log.debug("Starting "+event.toString());
        }

        @Override
        public void lifeCycleStarted(LifeCycle event) {
            log.debug("Started "+event.toString());
        }

        @Override
        public void lifeCycleFailure(LifeCycle event, Throwable cause) {
            log.debug("Failure "+event.toString());
        }

        @Override
        public void lifeCycleStopping(LifeCycle event) {
            log.debug("Stopping "+event.toString());
        }

        @Override
        public void lifeCycleStopped(LifeCycle event) {
            log.debug("Stopped "+event.toString());
        }
    }
}
