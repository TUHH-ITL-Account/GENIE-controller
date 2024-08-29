package core;

import static org.hamcrest.MatcherAssert.assertThat;

import exceptions.ConfigException;
import fdl.exceptions.DslException;
import generator.exceptions.ModelException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

public class ControllerTest {

  @Test
  public void testControllerRunningUDS() throws InterruptedException {
    Thread frontendDummy = new Thread(new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2frontend.sock"));
    Thread regulatorDummy = new Thread(new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2regulator.sock"));
    Thread controllerThread = new Thread(
        new ControllerThread("src/test/resources/exampleUDS.config"));
    frontendDummy.start();
    regulatorDummy.start();
    controllerThread.start();
    Thread.sleep(20000);
    controllerThread.interrupt();
  }

  @Test
  public void testControllerReconnectFrontend() throws InterruptedException {
    Thread frontendDummy = new Thread(new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2frontend.sock"));
    Thread regulatorDummy = new Thread(new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2regulator.sock"));
    Thread controllerThread = new Thread(
        new ControllerThread("src/test/resources/exampleUDS.config"));
    frontendDummy.start();
    regulatorDummy.start();
    controllerThread.start();
    Thread.sleep(5000);
    frontendDummy.interrupt();
    Thread.sleep(5000);
    frontendDummy = new Thread(new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2frontend.sock"));
    frontendDummy.start();
    Thread.sleep(10000);
    controllerThread.interrupt();
  }

  @Test
  public void testControllerRunningTCP() throws InterruptedException {
    Thread frontendDummy = new Thread(new DummyClient(4243));
    Thread regulatorDummy = new Thread(new DummyClient(4242));
    Thread controllerThread = new Thread(
        new ControllerThread("src/test/resources/exampleTCP.config"));
    frontendDummy.start();
    regulatorDummy.start();
    controllerThread.start();
    Thread.sleep(20000);
    controllerThread.interrupt();
  }

  @Disabled
  public void testControllerMain()
      throws ConfigException, IOException, DslException, InvocationTargetException,
      NoSuchMethodException, InstantiationException, IllegalAccessException, ModelException, InterruptedException {

    Main.main(new String[]{"-c", "./src/test/resources/example.config"});
  }

  @Disabled
  public void testControllerShutdown() throws IOException, InterruptedException {

    Thread regulatorDummy = new Thread(new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2regulator.sock"));
    regulatorDummy.start();
    Thread controllerThread = new Thread(
        new ControllerThread("src/test/resources/exampleUDS.config"));
    controllerThread.start();

    final File frontendSocketFile = new File(new File(System.getProperty("java.io.tmpdir")),
        "junixsocket-controller2frontend.sock");
    try (AFUNIXSocket sock = AFUNIXSocket.newInstance()) {
      while (!Thread.interrupted()) {
        try {
          sock.connect(AFUNIXSocketAddress.of(frontendSocketFile));
          break;
        } catch (SocketException e) {
          System.out.println("Cannot connect to server. Have you started it?");
          System.out.println();
          Thread.sleep(3000);
        }
      }
      System.out.println("Connected");

      try (OutputStream os = sock.getOutputStream()) {
        String outMessage = "#2#";
        os.write(ByteBuffer.allocate(4).putInt(outMessage.length()).array());
        os.flush();
        os.write(outMessage.getBytes(StandardCharsets.UTF_8));
      }
    }
    Thread.sleep(5000);
    assertThat("Controller thread was interrupted via message.", !controllerThread.isAlive());
  }

  @Disabled
  public void testControllerGenerationProcess()
      throws IOException {

    Thread controllerThread = new Thread(
        new ControllerThread("src/test/resources/exampleUDS.config"));
    controllerThread.start();

    //new Thread(new DummyClient()).start();

    final File frontendSocketFile = new File(new File("src/test/resources/sockets/"),
        "junixsocket-controller2frontend.sock");
    try (AFUNIXSocket sock = AFUNIXSocket.newInstance()) {
      try {
        sock.connect(AFUNIXSocketAddress.of(frontendSocketFile));
      } catch (SocketException e) {
        System.out.println("Cannot connect to server. Have you started it?");
        System.out.println();
        throw e;
      }
      System.out.println("Connected");

      try (InputStream is = sock.getInputStream();
          OutputStream os = sock.getOutputStream()) {
        //String outMessage = "3#2022-12-12-22:22:22:1234#bsp0264#TechnischeLogistikSS22#{difficulty:5;}#1#Unstetigförderer";
        String outMessage = "0#3";
        os.write(ByteBuffer.allocate(4).putInt(outMessage.length()).array());
        os.write(outMessage.getBytes(StandardCharsets.UTF_8));

        byte[] lengthBuffer = new byte[4];
        int lengthRead = is.read(lengthBuffer);
        int messageLength = ByteBuffer.wrap(lengthBuffer).getInt();
        byte[] messageBuffer = new byte[messageLength];
        int messageRead = is.read(messageBuffer, 0, messageLength);
        //String inMessage = new String(messageBuffer, 0, messageRead, StandardCharsets.UTF_8);
        //assertThat("Response message is not null.", inMessage, notNullValue());
        //assertThat("Response message is not empty.", inMessage, not(""));
      }
    }

  }

  static class ControllerThread implements Runnable {

    private final String configPath;

    public ControllerThread(String configPath) {
      this.configPath = configPath;
    }

    @Override
    public void run() {
      try {
        Controller controller = new Controller(configPath);
        controller.runController();
      } catch (IOException | ConfigException | InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException | DslException | ModelException | InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

}
