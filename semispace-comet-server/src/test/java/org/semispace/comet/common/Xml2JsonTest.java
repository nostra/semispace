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
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import org.junit.Assert;
import org.junit.Test;
import org.semispace.comet.client.AlternateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;

/**
 * Testing xml2json
 */
public class Xml2JsonTest {
    private static final Logger log = LoggerFactory.getLogger(Xml2JsonTest.class);

    @Test
    public void testTransform()  {
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

        String fromJsonToXml = Xml2Json.transform(xml);
        log.debug("fromJsonToXml :\n"+fromJsonToXml);

        XStream json = new XStream(new JettisonMappedXmlDriver());
        xstream.setMode(XStream.NO_REFERENCES);
        log.debug("Should be rather similar to :\n"+json.toXML(ah));
        Assert.assertEquals(json.toXML(ah), fromJsonToXml );

        AlternateHolder back = (AlternateHolder) json.fromXML(fromJsonToXml);
        Assert.assertEquals("Got some problem with transformed object: "+back, ah.fieldA, back.fieldA);
        Assert.assertEquals(ah.fieldB, back.fieldB);      
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

        String fromJsonToXml = Xml2Json.transform(xml);
        String fromJson2Xml = Json2Xml.transform(fromJsonToXml);
        String roundTrip = Json2Xml.transform(Xml2Json.transform(fromJson2Xml));
        log.debug("roundTrip:\n"+roundTrip );

        AlternateHolder back = (AlternateHolder) xstream.fromXML(roundTrip );
        Assert.assertEquals("Got some problem with transformed object: "+back, ah.fieldA, back.fieldA);
        Assert.assertEquals(ah.fieldB, back.fieldB);
    }

}
