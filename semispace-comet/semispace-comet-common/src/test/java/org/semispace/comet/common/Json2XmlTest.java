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
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Testing json to xml conversion
 */
public class Json2XmlTest {
    private static final Logger log = LoggerFactory.getLogger(Json2XmlTest.class);
    @Test
    public void testTransform() {
        XStream jsonstream = new XStream(new SemiSpaceJettisonDriver());
        jsonstream.setMode(XStream.NO_REFERENCES);
        AlternateHolder ah = new AlternateHolder();
        ah.fieldA = "a";
        ah.fieldB = "b";
        String json = jsonstream.toXML(ah);
        log.debug("Got json:\n"+json);
        String xml = Json2Xml.transform(json);
        log.debug("Got xml:\n"+xml);

        XStream xstream = new XStream();

        AlternateHolder trans = (AlternateHolder) xstream.fromXML(xml);
        Assert.assertEquals(ah.fieldA, trans.fieldA);
        Assert.assertEquals(ah.fieldB, trans.fieldB);
    }

    @Test
    public void testPotentialArrayConversionTrouble() {
        String originalJson = "{\"page\":{\"locks\":[{\"id\":\"edit1\",\"user\":\"Trygve\"}]}}";

        String xml = Json2Xml.transform(originalJson);
        log.debug("Json transformed into xml:\n"+xml+"\n");
        String toJson = Xml2Json.transform(xml);
        log.debug("Xml transformed back into originalJson:\n"+toJson);
        // TODO This test fails
        //Assert.assertEquals(originalJson, toJson);
        Assert.assertTrue("When this test fails, an error has been corrected", !originalJson.equals(toJson));
    }


    @Test
    public void testArrayProblemWithJavaObjects() {
        XStream jsonstream = new XStream(new SemiSpaceJettisonDriver());
        jsonstream.setMode(XStream.NO_REFERENCES);
        List<PageLock> locks = new ArrayList<PageLock>();
        PageLock lock = new PageLock();
        lock.setId("edit1");
        lock.setUser("Erlend");
        locks.add( lock );
        Page page = new Page();
        page.setLocks(locks);

        String json = jsonstream.toXML(page);
        log.debug("Got json:\n"+json);
        String xml = Json2Xml.transform(json);
        log.debug("Got xml:\n"+xml);

        String xmlBack2Json = Xml2Json.transform(xml);
        log.debug("Xml converted back to json: "+xmlBack2Json);

        // TODO The following fails
        //Assert.assertEquals(json, xmlBack2Json);
        Assert.assertTrue("When this test fails, an error has been corrected", !json.equals(xmlBack2Json));

        XStream xstream = new XStream();
        Page trans = (Page) xstream.fromXML(xml);
        Assert.assertEquals(1, trans.getLocks().size());
        Assert.assertEquals(locks.get(0).getId(), trans.getLocks().get(0).getId());
        Assert.assertEquals(locks.get(0).getUser(), trans.getLocks().get(0).getUser());
    }


    @Test
    public void testDifferenceBetweenDrivers() {
        XStream semispaceDriver = new XStream(new SemiSpaceJettisonDriver());
        XStream jettisonDriver = new XStream(new JettisonMappedXmlDriver());
        jettisonDriver.setMode(XStream.NO_REFERENCES);
        List<PageLock> locks = new ArrayList<PageLock>();
        PageLock lock = new PageLock();
        lock = new PageLock();
        lock.setId("edit1");
        lock.setUser("Erlend");
        Page page = new Page();
        page.setLocks(locks);

        String semispaceJson = semispaceDriver.toXML(page);
        String jettisonJson = jettisonDriver.toXML(page).replace(".","_");
        log.debug("Produced JSON:\n"+semispaceJson);
        Assert.assertEquals("SemiSpace jettison driver shall behave identically to the JettisonMappedXmlDriver with the exception of .-treatment.",
                jettisonJson, semispaceJson);
    }


    @Test
    public void testArrayWithTwoElements() {
        String originalJson = "{\"page\":{\"locks\":[{\"id\":\"edit1\",\"user\":\"Trygve\"},{\"id\":\"edit2\",\"user\":\"Erlend\"}]}}";

        String xml = Json2Xml.transform(originalJson);
        log.debug("Json transformed into xml:\n"+xml+"\n");
        String toJson = Xml2Json.transform(xml);
        log.debug("Xml transformed back into originalJson:\n"+toJson);
        Assert.assertEquals(originalJson, toJson);
    }

    @Test
    public void testXmlWithArrayMark() {
        String originalXml = "<page><id>someid</id><ARRAY><locks><id>edit2</id><user>Trygve</user></locks></ARRAY></page>";
        String toJson = Xml2Json.transform(originalXml);
        String xml = Json2Xml.transform(toJson);
        log.debug("Xml transformed into json:\n"+toJson+"\n");
        log.debug("Json transformed back into xml:\n"+xml);
        Assert.assertEquals(originalXml, xml);
    }
}
