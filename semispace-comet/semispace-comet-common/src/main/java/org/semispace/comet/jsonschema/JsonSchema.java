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
import org.semispace.comet.jsonschema.schemabean.SchemaRoot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 *
 */
public class JsonSchema {
    private static final Logger log = LoggerFactory.getLogger(JsonSchema.class);

    private XStream xstream;

    private SchemaRoot schemaRoot;

    public SchemaRoot getSchemaRoot() {
        return schemaRoot;
    }

    private JsonSchema() {
        // Intentional - want to get invoked with factory
        xstream = new XStream();
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.alias("root", SchemaRoot.class);
        //xstream.alias()
    }

    public static JsonSchema createJsonSchema( Reader jsonSchemaSource ) {
        final String json = "{\"root\":"+readContents( jsonSchemaSource )+"}";
        return createJsonSchemaFromJsonString(json);
    }

    private void parseAndEstablish(String json) {
        //To change body of created methods use File | Settings | File Templates.
        schemaRoot = (SchemaRoot) xstream.fromXML(json);
    }

    /**
     * Return read as string. Will throw exception if source cannot be read.
     * TODO: May consider to move this method to a more clean cut utility method.
     */
    public static String readContents(Reader source) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(source);
            int len = 1;
            char[] buf = new char[4096];
            while ( len > -1 ) {
                len = br.read(buf);
                if ( len > -1 ) {
                    sb.append(buf, 0, len );
                }
            }
            return sb.toString();
        } catch (IOException e) {
            throw new JsonException("Could not read source.", e);
        } finally {
            try {
                if ( br != null ) {
                    br.close();
                }
            } catch (IOException ignored) {}
        }
    }

    public static JsonSchema createJsonSchemaFromJsonString(String json) {
        JsonSchema jsonSchema = new JsonSchema();
        jsonSchema.parseAndEstablish( json );
        return jsonSchema;
    }
}
