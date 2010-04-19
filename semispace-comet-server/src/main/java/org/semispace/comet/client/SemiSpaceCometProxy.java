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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.component.LifeCycle;
import org.semispace.Holder;
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiLease;
import org.semispace.SemiSpaceInterface;
import org.semispace.comet.common.CometConstants;
import org.semispace.comet.common.Json2Xml;
import org.semispace.comet.common.Xml2Json;
import org.semispace.comet.common.XmlManipulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client side of comet proxy
 */
public class SemiSpaceCometProxy implements SemiSpaceInterface {
    private static final Logger log = LoggerFactory.getLogger(SemiSpaceCometProxy.class);
    private BayeuxClient client;
    private HttpClient httpClient;
    private XStream xstream;
    // private XStream jsonstream;
    /**
     * The call counter is used for differentiating between different calls from the same VM. 
     */
    private AtomicInteger myCallCounter = new AtomicInteger(1);

    public SemiSpaceCometProxy() {
        //JettisonMappedXmlDriver driver = new JettisonMappedXmlDriver();
        xstream = new XStream();
        // Conversion to / from JSON necessitates no references:
        xstream.setMode(XStream.NO_REFERENCES);
        //jsonstream = new XStream(driver);
        //jsonstream.setMode(XStream.NO_REFERENCES);
    }

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

    /**
     * @return null Always returning null, as lease is not supported for written objects.
     */
    @Override
    public SemiLease write(Object obj, long timeToLiveMs) {
        final String xml = toXML(obj);
        Holder holder = XmlManipulation.retrievePropertiesFromXml(xml, timeToLiveMs);

        Map<String, Object> param = new HashMap<String, Object>();
        param.put("timeToLiveMs",""+timeToLiveMs);
        param.put(CometConstants.PAYLOAD_MARKER, Xml2Json.transform(xml));

        WriteClient write = new WriteClient(myCallCounter.getAndIncrement());
        try {
            write.doWrite(client, param);
        } catch ( Throwable t ) {
            log.error("Unforeseen error occurred publishing.", t);
        }
        return null;
    }

    private String toXML(Object obj) {
        StringWriter writer = new StringWriter();
        xstream.marshal(obj, new CompactWriter(writer));
        return writer.toString();
    }

    private Object fromXML(String xml) {
        return xstream.fromXML(xml);
    }

    @Override
    public <T> T read(T template, long duration) {
        return readOrTake( template, duration, false);
    }

    private <T> T readOrTake(T template, long duration, boolean shallTake) {
        try {
            final ReadOrTake readOrTake;
            if ( shallTake ) {
                readOrTake = new TakeClient( myCallCounter.getAndIncrement());
            } else {
                readOrTake = new ReadClient( myCallCounter.getAndIncrement());
            }

            // TODO Use different method for extracting properties.
            Holder holder = XmlManipulation.retrievePropertiesFromXml(toXML(template), duration);

            Map<String, Object> param = new HashMap<String, Object>();
            param.put("searchMap", holder.getSearchMap());
            param.put("duration", ""+duration);
            String json = readOrTake.doReadOrTake(client, param, duration );
            if ( json != null ) {
                return (T) fromXML(Json2Xml.transform(json));
            }
        } catch ( Throwable t ) {
            log.error("Unforeseen error occurred publishing "+(shallTake?"take":"read")+" query.", t);
        }

        return null;
    }

    @Override
    public <T> T readIfExists(T template) {
        return read( template, 0 );  
    }

    @Override
    public <T> T take(T template, long duration) {
        return readOrTake(template, duration, true);
    }

    @Override
    public <T> T takeIfExists(T template) {
        return take( template, 0);  
    }


    /**
     * @return The resulting lease, or null if lease were not obtained
     */
    @Override
    public SemiEventRegistration notify(Object template, SemiEventListener listener, long duration) {
        // TODO Use different method for extracting properties.
        final String xml = toXML(template);
        Holder holder = XmlManipulation.retrievePropertiesFromXml(xml, duration);

        Map<String, Object> param = new HashMap<String, Object>();
        param.put("searchMap", holder.getSearchMap());
        param.put("duration",""+duration);
        param.put(CometConstants.PAYLOAD_MARKER, Xml2Json.transform(xml));
        param.put(CometConstants.OBJECT_TYPE_KEY, holder.getClassName());

        NotificationClient notification = new NotificationClient(myCallCounter.getAndIncrement(), listener);
        try {
            return notification.doNotify(client, param);
        } catch ( Throwable t ) {
            log.error("Unforeseen error occurred publishing.", t);
        }
        return null;
    }


    private static class ProxyLifeCycle implements LifeCycle.Listener {
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
