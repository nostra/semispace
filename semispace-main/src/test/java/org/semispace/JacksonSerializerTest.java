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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.semispace.actor.ActorManifest;
import org.semispace.actor.ActorMessage;
import org.semispace.actor.example.Ping;
import org.semispace.actor.example.Pong;
import static tools.jackson.core.StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * In order to test some jackson serializer settings
 */
class JacksonSerializerTest {
    private JacksonSerializer jackson = JacksonSerializer.jacksonSerializerFactory(true);

    @Test
    void objectToXml() {
        var str = """
                {"className":"org.semispace.actor.ActorManifest","payload":"{\\"holderId\\":8,\\"originatorId\\":6}"}
                """.stripIndent();
        ActorManifest actorManifest = (ActorManifest) jackson.xmlToObject(str);
        assertNotNull( actorManifest );
    }

    @Test
    void xmlToObject() {
        ActorManifest am = new ActorManifest(123L, 456L);
        var str = jackson.objectToXml(am);
        ActorManifest read = (ActorManifest) jackson.xmlToObject(str);
        assertEquals( am.getHolderId(), read.getHolderId());
        assertEquals( am.getOriginatorId(), read.getOriginatorId());
    }

    @Test
    void actorMessage() {
        ActorMessage msg = new ActorMessage();
        msg.setOriginatorId(1L);
        msg.setAddress(2L);
        msg.setPayload(new Pong());
        var str = jackson.objectToXml(msg);
        assertEquals("{\"className\":\"org.semispace.actor.ActorMessage\",\"payload\":\"{\\\"address\\\":2,\\\"originatorId\\\":1,\\\"payload\\\":[\\\"org.semispace.actor.example.Pong\\\",{}]}\"}",
                str);
    }

    @Test
    void actorMessageDirect() {
        ObjectMapper MAPPER = JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .enable(INCLUDE_SOURCE_IN_LOCATION)
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder().build(),
                        DefaultTyping.JAVA_LANG_OBJECT
                )
                .build();

        // No need to register subtypes when class is annotated
        //MAPPER.registerSubtypes(new NamedType(Ping.class, "Ping"));
        //MAPPER.registerSubtypes(new NamedType(Pong.class, "Pong"));

        Pong pong = new Pong();
        assertEquals("{\"@type\":\"Pong\"}", MAPPER.writeValueAsString(pong));

        Ping ping = new Ping();
        assertEquals("{\"@type\":\"Ping\"}", MAPPER.writeValueAsString(ping));

        List pl = new ArrayList<>();
        pl.add(pong);
        pl.add(ping);
        assertEquals("[[\"org.semispace.actor.example.Pong\",{}],[\"org.semispace.actor.example.Ping\",{}]]", MAPPER.writeValueAsString(pl));

        ActorMessage msg = new ActorMessage();
        msg.setOriginatorId(1L);
        msg.setAddress(2L);
        msg.setPayload(new Pong());
        var str = MAPPER.writeValueAsString(msg);
        assertEquals("{\"address\":2,\"originatorId\":1,\"payload\":[\"org.semispace.actor.example.Pong\",{}]}", str);
    }
}