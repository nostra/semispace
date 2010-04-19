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

package org.semispace.comet.common;

import org.semispace.Holder;
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

/**
 * Interim utility class
 */
public class XmlManipulation {
    private static final Logger log = LoggerFactory.getLogger(XmlManipulation.class);
    /**
     * TODO Presently duplicated from WsSpaceImpl
     * Protected for the benefit of junit test(s)
     * @return A <b>temporary</b> holder object containing the relevant elements found in the source.
     */
    public static Holder retrievePropertiesFromXml(String xmlsource, long duration) {
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
        map.put(CometConstants.OBJECT_TYPE_KEY, doctype);

        Holder holder = new Holder(xmlsource, duration, doctype, -1, map );
        return holder;
    }
}
