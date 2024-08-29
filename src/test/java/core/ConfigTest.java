package core;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ConfigTest {

  @Test
  public void testConfigLoading() throws IOException {
    Controller dummy = new Controller();
    String name = "exampleUDS.config";
    Properties prop = dummy.loadConfig("./src/test/resources/" + name);
    assertThat("prop not null", prop, notNullValue());
  }

  @Disabled
  public void testListArgs() throws IOException {
    Controller dummy = new Controller();
    String name = "exampleUDS.config";
    Properties prop = dummy.loadConfig("./src/test/resources/" + name);
    String[] list = ((String) prop.getOrDefault("list_test", "")).split(",");
    assertThat("String is split correctly", list.length == 2);
    assertThat("String is split correctly", list[0].equals("a"));
    assertThat("String is split correctly", list[1].equals("b"));
  }
}
