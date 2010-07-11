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

package org.semispace.comet.jsonschema.schemabean;

import java.util.Map;

/**
 *
 */
public class SchemaRoot {
    private String name;
    private Map<String, SchemaItem> properties;

    public Map<String, SchemaItem> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, SchemaItem> properties) {
        this.properties = properties;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
