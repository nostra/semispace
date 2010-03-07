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
import org.cometd.client.BayeuxClient;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.component.LifeCycle;
import org.semispace.Holder;
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiLease;
import org.semispace.SemiSpaceInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
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
    /**
     * The call counter is used for differentiating between different calls from the same VM. 
     */
    private AtomicInteger myCallCounter = new AtomicInteger(1);

    public SemiSpaceCometProxy() {
        xstream = new XStream();
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
     * @return null TODO Presently always returning null, as lease is not yet supported.
     */
    @Override
    public SemiLease write(Object obj, long duration) {
        WriteClient write = new WriteClient();
        return null;
    }

    @Override
    public <T> T read(T template, long duration) {
        ReadClient read = new ReadClient( myCallCounter.getAndIncrement() );

        // TODO Use different method for extracting properties.
        Holder holder = retrievePropertiesFromXml(xstream.toXML(template), duration);

        holder.getSearchMap();
        Map<String, Object> param = new HashMap<String, Object>();
        param.put("searchMap", holder.getSearchMap());
        param.put("duration", Long.valueOf(duration));
        String xml = read.doRead(client, param );
        if ( xml != null ) {
            return (T) xstream.fromXML(xml);
        }

        return null;
    }

    @Override
    public <T> T readIfExists(T template) {
        return read( template, 0 );  
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


    /**
     * TODO Presently duplicated from WsSpaceImpl
     * Protected for the benefit of junit test(s)
     * @return A <b>temporary</b> holder object containing the relevant elements found in the source.
     */
    protected Holder retrievePropertiesFromXml(String xmlsource, long duration) {
        // InputStream is = new StringConverterBufferedInputStream( new FileInputStream( tmpfile ) );
        InputSource is = new InputSource( new StringReader(xmlsource));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        Document doc = null;
        try {
            doc = factory.newDocumentBuilder().parse( is );
        } catch (Exception e) {
            log.error("Returning null, due to exception parsing xml: "+xmlsource, e);
            return null;
        }
        String doctype = doc.getDocumentElement().getNodeName();

        Map<String, String> map = new HashMap<String, String>();

        NodeList children = doc.getDocumentElement().getChildNodes();
        for ( int i=0 ; i < children.getLength() ; i++) {
            Node node = children.item( i );
            //log.info("Got node "+node.getNodeName()+" which contains "+node.getNodeValue());

            if ( node.getNodeType() == Node.ELEMENT_NODE && node.getChildNodes().getLength() > 0) {
                String name = node.getNodeName();
                String value = node.getChildNodes().item(0).getNodeValue();
                //log.info("This is an element node with "+node.getChildNodes().getLength()+" children which is "+value);
                if( value != null ) {
                    map.put(name,value);
                }
            }
        }
        map.put("class", doctype);

        Holder holder = new Holder(xmlsource, duration, doctype, -1, map );
        return holder;
    }


    private Map<String, Object> uneccessary_transformHolderToMap(Holder holder) {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("class", holder.getClassName());
        map.put("id", Long.valueOf(holder.getId()));
        map.put("liveUntil", Long.valueOf(holder.getLiveUntil()));
        map.put("searchMap", holder.getSearchMap());
        map.put("xml", holder.getXml());

        return map;
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
