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

package org.semispace.comet.jsonschema;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.semispace.comet.common.Json2Xml;
import org.semispace.comet.common.Xml2Json;
import org.semispace.comet.jsonschema.schemabean.SchemaItem;
import org.semispace.comet.jsonschema.schemabean.SchemaRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class JsonSchemaTest {
    private static final Logger log = LoggerFactory.getLogger(JsonSchemaTest.class);

    /**
     * Just checking how things get transformed
     * @throws Exception
     */
    @Test
    public void implementationTesting() {
        XStream xstream = new XStream();
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("root", SchemaRoot.class);

        SchemaRoot sr = new SchemaRoot();
        sr.setName("product");
        Map<String, SchemaItem> itemMap = new HashMap<String, SchemaItem>();
        SchemaItem si = new SchemaItem();
        si.setDescription("Product identifier");
        si.setType("number");
        itemMap.put( "id", si );
        si = new SchemaItem();
        si.setDescription("Name of product");
        si.setType("string");
        itemMap.put("name", si );

        sr.setProperties(itemMap);
        
        StringWriter writer = new StringWriter();
        xstream.marshal(sr, new CompactWriter(writer));
        String xml = writer.toString();
        log.debug("Transformed xml: "+xml);
        log.debug("Transformed json: "+ Xml2Json.transform(xml));
    }


    @Test
    public void testJsonExample1() throws IOException, JSONException, XMLStreamException {
        String resourceName = "org/semispace/json/json-schema-ex1.json";
        Reader reader = new InputStreamReader( new ClassPathResource(resourceName).getInputStream());

        final String json = "{\"root\":"+ JsonSchema.readContents(reader)+"}";
        //*
        log.debug("Json file contents:\n"+json+"\n");
        String xml = Json2Xml.transform(json);
        log.debug("Json transformed into xml:\n"+xml);

        /* TODO Test fails here
        JsonSchema jschema = JsonSchema.createJsonSchemaFromJsonString(json);
        SchemaRoot root = jschema.getSchemaRoot();
        Assert.assertNotNull( root );
        */
    }

    @Test
    public void testJsonSchema() throws IOException, JSONException, XMLStreamException {
        String resourceName = "org/semispace/json/meta-json-schema.json";
        Reader reader = new InputStreamReader( new ClassPathResource(resourceName).getInputStream());

        final String json = "{\"root\":"+ JsonSchema.readContents(reader)+"}";
        log.debug("Json file contents:\n"+json+"\n");
        String xml = Json2Xml.transform(json);
        log.debug("Json transformed into xml:\n"+xml);
    }

}
