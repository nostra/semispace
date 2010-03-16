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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.junit.Assert;
import org.junit.Test;
import org.semispace.comet.client.AlternateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Testing xml2json
 */
public class Xml2JsonTest {
    private static final Logger log = LoggerFactory.getLogger(Xml2JsonTest.class);

    @Test
    public void testTransform()  {
        XStream xstream = new XStream(); 
        xstream.setMode(XStream.NO_REFERENCES);

        AlternateHolder ah = new AlternateHolder();
        ah.fieldA = "a";
        ah.fieldB = "b";
        StringWriter writer = new StringWriter();
        // Emulating how proxy will transform objects
        xstream.marshal(ah, new CompactWriter(writer));
        String xml = writer.toString();
        log.debug("Got xml:\n"+xml);

        String fromJsonToXml = Xml2Json.transform(xml);
        log.debug("fromJsonToXml :\n"+fromJsonToXml);

        XStream json = new XStream(new JettisonMappedXmlDriver());
        xstream.setMode(XStream.NO_REFERENCES);
        log.debug("Should be rather similar to :\n"+json.toXML(ah));
        Assert.assertEquals(json.toXML(ah).replace('.','-'), fromJsonToXml );
    }

    @Test
    public void testRoundTrip()  {
        XStream xstream = new XStream(); // new XStream(new StaxDriver());
        xstream.setMode(XStream.NO_REFERENCES);

        AlternateHolder ah = new AlternateHolder();
        ah.fieldA = "a";
        ah.fieldB = "b";

        StringWriter writer = new StringWriter();
        // Emulating how proxy will transform objects
        xstream.marshal(ah, new CompactWriter(writer));
        String xml = writer.toString();
        log.debug("Got xml:\n"+xml);

        String fromXml2Json = Xml2Json.transform(xml);
        String fromJson2Xml = Json2Xml.transform(fromXml2Json);
        String roundTrip = Json2Xml.transform(Xml2Json.transform(fromJson2Xml));
        log.debug("roundTrip:\n"+roundTrip );

        AlternateHolder back = (AlternateHolder) xstream.fromXML(roundTrip );
        Assert.assertEquals("Got some problem with transformed object: "+back, ah.fieldA, back.fieldA);
        Assert.assertEquals(ah.fieldB, back.fieldB);
    }

    /**
     * Just an implementation help. Retained as it may be useful if xstream gets updated
     */
    @Test
    public void testRawFunctionality() throws IOException {
        XStream xstream = new XStream(); // new XStream(new StaxDriver());
        xstream.setMode(XStream.NO_REFERENCES);

        AlternateHolder ah = new AlternateHolder();
        ah.fieldA = "a";
        ah.fieldB = "b";

        StringWriter ww = new StringWriter();
        // Emulating how proxy will transform objects
        xstream.marshal(ah, new CompactWriter(ww));
        final String xml = ww.toString();
        log.debug("Got xml:\n"+xml);

        final HierarchicalStreamDriver jettison = new DashifyJettisonDriver();
        
        final XppDriver xpp = new XppDriver();

        StringReader reader = new StringReader(xml);
        HierarchicalStreamReader hsr= xpp.createReader(reader);
        StringWriter writer = new StringWriter();
        new HierarchicalStreamCopier().copy(hsr, jettison.createWriter(writer));
        writer.close();
        String json = writer.toString();
        Assert.assertEquals("Should not have any dots present in writer: "+json, -1, json.indexOf("."));
        log.debug("Json is: "+json);

        //
        // Transform back
        //

        reader = new StringReader(json);
        hsr = jettison.createReader(reader);
        writer = new StringWriter();
        new HierarchicalStreamCopier().copy(hsr, new CompactWriter(writer));
        try {
            writer.close();
        } catch (IOException e) {
            log.error("Unforeseen exception.", e);
        }
        log.debug("After transforming json back, I got: "+writer);
    }

}