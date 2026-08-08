/*
 * Copyright © 2026 Erlend Nossum
 *
 * This file is part of the semispace project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Description:  See javadoc below
 */

package org.semispace;

import org.semispace.exception.SemiSpaceObjectException;
import tools.jackson.core.JacksonException;
import static tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.dataformat.xml.XmlMapper;

public class JacksonSerializer implements SemiSpaceSerializer {
    private final ObjectMapper mapper;

    public static final JacksonSerializer jacksonSerializerFactory( boolean json) {
        if ( json ) {
            return new JacksonSerializer(JsonMapper.builder()
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                    .enable(INCLUDE_SOURCE_IN_LOCATION)
                    .activateDefaultTyping(
                            BasicPolymorphicTypeValidator.builder().build(),
                            DefaultTyping.JAVA_LANG_OBJECT
                    )
                    .build());
        } else {
            return new JacksonSerializer(XmlMapper.builder()
                    .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                    .enable(INCLUDE_SOURCE_IN_LOCATION)
                    .activateDefaultTyping(
                            BasicPolymorphicTypeValidator.builder().build(),
                            DefaultTyping.JAVA_LANG_OBJECT
                    )
                    .build());
        }
    }

    public JacksonSerializer(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public String objectToXml(Object obj) {
        if ( obj == null ) {
            return null;
        }
        try {
            JacksonObject holder = new JacksonObject();
            holder.className = obj.getClass().getName();
            holder.payload = mapper.writeValueAsString(obj);
            return mapper.writeValueAsString(holder);
        } catch (JacksonException e) {
            throw new SemiSpaceObjectException("Could not process json", e);
        }
    }

    @Override
    public Object xmlToObject(String xml) {
        if (xml == null || xml.isEmpty()) {
            return null;
        }

        try {
            JacksonObject holder = mapper.readValue( xml, JacksonObject.class);
            Class type = Class.forName(holder.className);
            return mapper.readValue(holder.payload, type);
        } catch (JacksonException e) {
            throw new SemiSpaceObjectException("Jackson could not process json", e);
        } catch (ClassNotFoundException e) {
            throw new SemiSpaceObjectException("Class not found, which implies that objects in backend storage are " +
                    "broken, or that distribution uses different versions of application", e);
        }
    }

    private static class JacksonObject {
        public String className;
        public String payload;
    }
}
