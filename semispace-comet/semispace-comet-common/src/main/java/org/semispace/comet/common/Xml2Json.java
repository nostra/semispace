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

import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Code based on information found
 * <a href="http://stackoverflow.com/questions/894625/">on stack overflow</a>.
 */
public class Xml2Json {
    private static final Logger log = LoggerFactory.getLogger(Xml2Json.class);
    private Xml2Json() {
        // Intentional
    }

    /**
     * @param xml is presumed to have been created by xstream configured with
     * <code>xstream.setMode(XStream.NO_REFERENCES);</code>
     */
    public static final String transform( String xml ) {
        final HierarchicalStreamDriver jettison = new SemiSpaceJettisonDriver();
        final XppDriver xpp = new XppDriver(new XmlFriendlyNameCoder("$","_"));

        StringReader reader = new StringReader(xml);
        HierarchicalStreamReader hsr= xpp.createReader(reader);
        StringWriter writer = new StringWriter();
        new HierarchicalStreamCopier().copy(hsr, jettison.createWriter(writer));
        try {
            writer.close();
        } catch (IOException e) {
            log.error("Unforeseen exception.", e);
        }

        return writer.toString();
    }
}
