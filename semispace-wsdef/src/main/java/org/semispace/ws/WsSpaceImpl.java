/*
 * ============================================================================
 *
 *  File:     WsSpace.java
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

package org.semispace.ws;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.semispace.Holder;
import org.semispace.SemiSpace;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Implementation of unauthenticated space access.
 *  <p>
 * You need to set the space.
 * </p>
 */
@WebService(endpointInterface = "org.semispace.ws.WsSpace")
public class WsSpaceImpl implements WsSpace {
    private static final Logger log = LoggerFactory.getLogger(WsSpaceImpl.class);
            
    private SemiSpace space;
    
    /** For the benefit of spring */
    public void setSpace(SemiSpace space) {
        this.space = space;
    }

    public void write(String contents, long timeToLiveMs) {
        makeSureSpaceIsPresent();
        Holder elem = retrievePropertiesFromXml(contents);
        space.writeToElements(elem.getClassName(),timeToLiveMs, elem.getXml(), elem.getSearchMap());
    }

    private void makeSureSpaceIsPresent() {
        if ( space == null ) {
            String error = "Erroneous initialization - need space.";
            log.error(error);
            throw new RuntimeException(error);
        }
    }

    public String read(String template, long queryLifeMs) {
        makeSureSpaceIsPresent();
        Holder elem = retrievePropertiesFromXml(template );
        String found = space.findOrWaitLeaseForTemplate(elem.getSearchMap(), queryLifeMs, false);
        return found;
    }

    public String readIfExists(String template) {
        makeSureSpaceIsPresent();
        return read( template, 0);
    }

    public String take(String template, long queryLifeMs) {
        makeSureSpaceIsPresent();
        Holder elem = retrievePropertiesFromXml(template );
        String found = space.findOrWaitLeaseForTemplate(elem.getSearchMap(), queryLifeMs, true);
        return found;
    }

    public String takeIfExists(String template) {
        makeSureSpaceIsPresent();
        return take( template, 0);
    }

    
    /**
     * Protected for the benefit of junit test(s)
     * @return A <b>temporary</b> holder object containing the relevant elements found in the source.
     * @see WsSpaceImpl#retrievePropertiesFromObject
     */
    protected Holder retrievePropertiesFromXml(String xmlsource) {
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

        Holder holder = new Holder(xmlsource, -1, doctype, -1, map );
        return holder;
    }
}
