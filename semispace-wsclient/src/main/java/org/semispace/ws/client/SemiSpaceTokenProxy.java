/*
 * ============================================================================
 *
 *  File:     SemiSpaceTokenProxy.java
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
import org.semispace.ws.TokenWsSpace;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.thoughtworks.xstream.XStream;

public class SemiSpaceTokenProxy implements SemiSpaceInterface {
    private static final Logger log = LoggerFactory.getLogger( SemiSpaceTokenProxy.class );
    
    private static SemiSpaceTokenProxy instance = null;
    private TokenWsSpace space;
    private XStream xstream;
    private String token;

    private String username;
    private String password;

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private SemiSpaceTokenProxy() {
        xstream = new XStream();
    }
    
    /**
     * Open WS connection to the defined end point. This should be 
     * something similar to <tt>http://localhost:8080/semispace-war/services/tokenspace</tt>.
     */
    public static synchronized SemiSpaceTokenProxy retrieveSpace( String endpoint) {
        if ( instance == null ) {
            instance = retrieveSpace(readSpaceServiceAsStandardPort(endpoint));
            //instance = retrieveSpace(readSpaceServiceAsSpring());
        }
        
        return instance;
    }
 
    /**
     * An alternative way of performing lookup
     */
    protected static TokenWsSpace readSpaceServiceAsSpring() {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext(new String[] {"org/semispace/ws/client/tokenspace-bean.xml"});
        TokenWsSpace hw = (TokenWsSpace) context.getBean("space");
        return hw;
    }
    
    protected static TokenWsSpace readSpaceServiceAsStandardPort() {
        return readSpaceServiceAsStandardPort("http://localhost:8080/semispace-war/services/tokenspace");
    }
    
    protected static TokenWsSpace readSpaceServiceAsStandardPort( String endpointAddress ) {
         QName SERVICE_NAME = new QName("http://ws.semispace.org/", "TokenWsSpace");
         QName PORT_NAME = new QName("http://ws.semispace.org/", "TokenWsSpacePort");
    
        Service service = Service.create(SERVICE_NAME);
        
        // Add a port to the Service
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, endpointAddress);
        
        TokenWsSpace hw = service.getPort(TokenWsSpace.class);
        return hw;
    }

    /**
     * This is a bit roundabout, but is used for junit purposes.
     */
    protected static synchronized SemiSpaceTokenProxy retrieveSpace(TokenWsSpace space ) {
        if ( instance == null ) {
            instance = new SemiSpaceTokenProxy();
            instance.space = space;
        }
        return instance; 
    }
     
    
    /**
     * <b>Notify is illegal to use in proxy</b>
     */
    public SemiEventRegistration notify(Object template, SemiEventListener listener, long duration) {
        throw new SemiSpaceProxyException("Illegal to use notify in token space proxy.", null);
    }

    public Object read(Object template, long duration) {
        String xml;
        try {
            xml = space.read( fetchToken(), toXml( template ), duration);
        } catch (Exception e) {
            token = null;
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
            xml = space.readIfExists( fetchToken(), toXml( template ));
        } catch (Exception e) {
            token = null;
            throw new SemiSpaceProxyException("Could not read due to connection error.", e);
        }
        return fromXml( xml );
    }

    public Object take(Object template, long duration) {
        String xml;
        try {
            xml = space.take(fetchToken(), toXml( template ), duration);
        } catch (RuntimeException e) {
            token = null;
            throw new SemiSpaceProxyException("Could not take due to connection error.", e);
        }
        return fromXml( xml );
    }

    public Object takeIfExists(Object template) {
        String xml;
        try {
            xml = space.takeIfExists(fetchToken(), toXml( template ));
        } catch (RuntimeException e) {
            token = null;
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
            space.write(fetchToken(), toXml(obj), duration);
        } catch (RuntimeException e) {
            token = null;
            throw new SemiSpaceProxyException("Could not write due to connection error.", e);
        }
        return null;
    }
    
    /**
     * If a token is present, you are authenticated 
     */
    public boolean hasToken() {
        return fetchToken() != null;
    }

    protected void setSpace(TokenWsSpace space) {
        this.space = space;
    }

    /**
     * If token has already been found, return it. Otherwise try to generate it
     * with the applicable call. If failure to obtain token, return null (which will
     * generate later errors.)
     */
    private String fetchToken() {
        if ( token == null ) {
            try {
                token = space.login(getUsername(), getPassword());                
            } catch (Exception e) {
                log.warn("Got exception trying to log in. Masked it was: "+e.getMessage());
            }
            if ( token == null ) {
                log.warn("Login unsuccessful with username "+username);
            } else {
                log.info("Logged in successfully with username "+username);
            }
        }
        return token;
    }

}
