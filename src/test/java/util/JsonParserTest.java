package util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class JsonParserTest {

  @Test
  public void testJsonParser() throws JsonProcessingException {
    String jsonString = "{ \"someKey\" : \"someValue\", \"foo\":42, \"bar\":12.34, \"noAI\":true }";
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> map;
    map = objectMapper.readValue(jsonString, new TypeReference<>() {
    });
    assertThat("Map exists", map, notNullValue());
    assertThat("someKey is in map", map.containsKey("someKey"));
    assertThat("someKey has correct value", (String) map.get("someKey"), is("someValue"));
    assertThat("foo is in map", map.containsKey("foo"));
    assertThat("foo has correct value", (int) map.get("foo"), is(42));
    assertThat("bar is in map", map.containsKey("bar"));
    assertThat("bar has correct value", (double) map.get("bar"), is(12.34));
    assertThat("noAI is in map", map.containsKey("noAI"));
    assertThat("noAI has correct value", (boolean) map.get("noAI"));
  }
}
