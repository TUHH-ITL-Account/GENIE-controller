package core;

import java.io.IOException;
import org.junit.jupiter.api.Disabled;

public class FrontendToControllerTest {


  @Disabled
  public void testCommunication() throws IOException {
    DummyServer server = new DummyServer();
    server.main();
  }
}
