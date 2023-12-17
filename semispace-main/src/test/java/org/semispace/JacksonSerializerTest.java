package org.semispace;

import static com.fasterxml.jackson.core.JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.semispace.actor.ActorManifest;
import org.semispace.actor.ActorMessage;
import org.semispace.actor.example.Ping;
import org.semispace.actor.example.Pong;

import java.util.ArrayList;
import java.util.List;

/**
 * In order to test some jackson serializer settings
 */
class JacksonSerializerTest {
    private JacksonSerializer jackson = new JacksonSerializer();

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
        assertEquals("{\"className\":\"org.semispace.actor.ActorMessage\",\"payload\":\"{\\\"originatorId\\\":1,\\\"address\\\":2,\\\"payload\\\":[\\\"org.semispace.actor.example.Pong\\\",{}]}\"}",
                str);
    }

    @Test
    @Disabled("Missing type at the moment ?")
    void actorMessageDirect() throws JsonProcessingException {
        ObjectMapper MAPPER = new ObjectMapper()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS,false)
                .configure(INCLUDE_SOURCE_IN_LOCATION, true)
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfSubTypeIsArray()
                        .allowIfBaseType(Object.class)
                        .build());

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
        assertEquals("{\"originatorId\":1,\"address\":2,\"payload\":[\"org.semispace.actor.example.Pong\",{}]}", str);
    }
}