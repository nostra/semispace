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
import org.semispace.comet.client.AlternateHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testing json to xml conversion
 */
public class Json2XmlTest {
    private static final Logger log = LoggerFactory.getLogger(Json2XmlTest.class);
    @Test
    public void testTransform() {
        XStream jsonstream = new XStream(new JettisonMappedXmlDriver());
        jsonstream.setMode(XStream.NO_REFERENCES);
        AlternateHolder ah = new AlternateHolder();
        ah.fieldA = "a";
        ah.fieldB = "b";
        String json = jsonstream.toXML(ah);
        log.debug("Got json:\n"+json);
        String xml = Json2Xml.transform(json);
        log.debug("Got xml:\n"+xml);

        XStream xstream = new XStream();
        //log.debug("XStream would like something similar to:\n"+xstream.toXML(ah));

        AlternateHolder trans = (AlternateHolder) xstream.fromXML(xml);
        Assert.assertEquals(ah.fieldA, trans.fieldA);
        Assert.assertEquals(ah.fieldB, trans.fieldB);
    }

    @Test
    public void testPotentialArrayConversionTrouble() {
        String originalJson = "{\"page\" : {\"locks\" : [{\"id\" : \"edit1\",\"user\" : \"Trygve\"}]}}";

        String xml = Json2Xml.transform(originalJson);
        log.debug("Json transformed into xml:\n"+xml+"\n");
        String toJson = Xml2Json.transform(xml);
        log.debug("Xml transformed back into originalJson:\n"+toJson);
        // TODO This test fails Assert.assertEquals(originalJson, toJson);
    }

}
