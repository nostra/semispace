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
import com.thoughtworks.xstream.io.xml.CompactWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Code based on <a href="http://xstream.codehaus.org/faq.html#JSON">XStream FAQ - Stream fails to unmarshal my JSON
 * string and I do not know why?</a>
 */
public class Json2Xml {
    private static final Logger log = LoggerFactory.getLogger(Json2Xml.class);
    private Json2Xml() {
        // Intentional
    }

    /**
     * Create XML out of json
     * @param json To convert into XML
     * @return XML representation
     */
    public static final String transform( String json ) {
        HierarchicalStreamDriver driver = new DashifyJettisonDriver();
        StringReader reader = new StringReader(json);
        HierarchicalStreamReader hsr = driver.createReader(reader);
        StringWriter writer = new StringWriter();
        new HierarchicalStreamCopier().copy(hsr, new CompactWriter(writer));
        try {
            writer.close();
        } catch (IOException e) {
            log.error("Unforeseen exception.", e);
        }
        return writer.toString();
    }
}
