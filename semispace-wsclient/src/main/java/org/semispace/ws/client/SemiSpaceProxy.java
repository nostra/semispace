/*
 * ============================================================================
 *
 *  File:     SemiSpaceProxy.java
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
 *  Created:      Mar 4, 2008
 * ============================================================================ 
 */

package org.semispace.ws.client;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.SemiEventListener;
import org.semispace.SemiEventRegistration;
import org.semispace.SemiLease;
import org.semispace.SemiSpaceInterface;
import org.semispace.ws.WsSpace;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.thoughtworks.xstream.XStream;

/**
 * Client side proxy of SemiSpace web services
 */
public class SemiSpaceProxy implements SemiSpaceInterface {
    private static final Logger log = LoggerFactory.getLogger( SemiSpaceProxy.class );
    
    private static SemiSpaceProxy instance = null;
    private WsSpace space;
    private XStream xstream;
    
    private SemiSpaceProxy() {
        xstream = new XStream();
    }
    
    /**
     * Open WS connection to the defined end point. This should be 
     * something similar to <tt>http://localhost:8080/semispace-war/services/space</tt>.
     */
    public static synchronized SemiSpaceInterface retrieveSpace( String endpoint) {
        if ( instance == null ) {
            instance = retrieveSpace(readSpaceServiceAsStandardPort(endpoint));
            //instance = retrieveSpace(readSpaceServiceAsSpring());
        }
        
        return instance;
    }
 
    /**
     * An alternative way of performing lookup
     */
    protected static WsSpace readSpaceServiceAsSpring() {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext(new String[] {"org/semispace/ws/client/space-bean.xml"});
        WsSpace hw = (WsSpace) context.getBean("space");
        return hw;
    }
    
    protected static WsSpace readSpaceServiceAsStandardPort() {
        return readSpaceServiceAsStandardPort("http://localhost:8080/semispace-war/services/space");
    }
    
    protected static WsSpace readSpaceServiceAsStandardPort( String endpointAddress ) {
         QName SERVICE_NAME = new QName("http://ws.semispace.org/", "WsSpace");
         QName PORT_NAME = new QName("http://ws.semispace.org/", "WsSpacePort");
    
        Service service = Service.create(SERVICE_NAME);
        
        // Add a port to the Service
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, endpointAddress);
        
        WsSpace hw = service.getPort(WsSpace.class);
        return hw;
    }

    /**
     * This is a bit roundabout, but is used for junit purposes.
     */
    protected static synchronized SemiSpaceProxy retrieveSpace(WsSpace space ) {
        if ( instance == null ) {
            instance = new SemiSpaceProxy();
            instance.space = space;
        }
        return instance; 
    }
    
    /**
     * <b>Notify is illegal to use in proxy</b>
     */
    public SemiEventRegistration notify(Object template, SemiEventListener listener, long duration) {
        throw new SemiSpaceProxyException("Illegal to use notify in space proxy.", null);
    }

    public Object read(Object template, long duration) {
        String xml;
        try {
            xml = space.read( toXml( template ), duration);
        } catch (Exception e) {
            throw new SemiSpaceProxyException("Could not read due to connection error.", e);
        }
        return fromXml( xml );
    }

    private Object fromXml(String xml) {
        if ( xml == null ) {
            return null;
        }
        return xstream.fromXML(xml);
    }

    private String toXml(Object template) {
        return xstream.toXML(template);
    }

    public Object readIfExists(Object template) {
        String xml;
        try {
            xml = space.readIfExists( toXml( template ));
        } catch (Exception e) {
            throw new SemiSpaceProxyException("Could not read due to connection error.", e);
        }
        return fromXml( xml );
    }

    public Object take(Object template, long duration) {
        String xml;
        try {
            xml = space.take(toXml( template ), duration);
        } catch (RuntimeException e) {
            throw new SemiSpaceProxyException("Could not take due to connection error.", e);
        }
        return fromXml( xml );
    }

    public Object takeIfExists(Object template) {
        String xml;
        try {
            xml = space.takeIfExists(toXml( template ));
        } catch (RuntimeException e) {
            throw new SemiSpaceProxyException("Could not take due to connection error.", e);
        }
        return fromXml( xml );
    }

    /**
     * TODO Consider fixing that null is always returned.
     * @return will presently ALWAYS return null, even when operation was a success.
     * @see org.semispace.SemiSpaceInterface#write(java.lang.Object, long)
     */
    public SemiLease write(Object obj, long duration) {
        try {
            space.write(toXml(obj), duration);
        } catch (RuntimeException e) {
            throw new SemiSpaceProxyException("Could not write due to connection error.", e);
        }
        return null;
    }

    protected void setSpace(WsSpace space) {
        this.space = space;
    }

}
