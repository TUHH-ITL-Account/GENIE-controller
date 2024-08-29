package core;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

public class CommsTest {

  @Test
  public void testIntToByteArray() {
    byte[] bytes = ByteBuffer.allocate(4).putInt(164).array();

    for (byte b : bytes) {
      System.out.format("0x%x ", b);
    }
  }

  @Test
  public void testMsg() {
    String outMessage = "0#2#";
    byte[] encoded = outMessage.getBytes(StandardCharsets.UTF_8);
    String decoded = new String(encoded, 0, outMessage.length(), StandardCharsets.UTF_8);
    assertThat("Decoded message equals original message.", decoded, is(outMessage));
  }

  @Test
  public void testUmlaut() {
    String msg = "ü";
    byte[] inBytes = msg.getBytes(StandardCharsets.UTF_8);
    String backToString = new String(inBytes);

    assertThat("Umlaut transformation successful.", backToString, is(msg));
  }
}
