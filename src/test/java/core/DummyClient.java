package core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

public class DummyClient implements Runnable {

  private final BlockingQueue<String> messagesToSend = new ArrayBlockingQueue<>(10);
  private String socketPath;
  private String socketName;
  private int port = -1;

  public DummyClient(String socketPath, String socketName) {
    this.socketPath = socketPath;
    this.socketName = socketName;
  }

  public DummyClient(int port) {
    this.port = port;
  }

  public void main() throws IOException, InterruptedException {
    if (port == -1) {
      final File socketFile = new File(new File(socketPath),
          socketName);

      try (AFUNIXSocket sock = AFUNIXSocket.newInstance()) {
        while (!Thread.interrupted()) {
          try {
            sock.connect(AFUNIXSocketAddress.of(socketFile));
            break;
          } catch (SocketException e) {
            System.out.println("DummyClient: Cannot connect to server. Have you started it?");
            System.out.println();
            Thread.sleep(2000);
          }
        }
        System.out.println("DummyClient: Connected @ " + socketName);

        try (InputStream is = sock.getInputStream(); //
            OutputStream os = sock.getOutputStream();) {

          DataInputStream din = new DataInputStream(is);
          DataOutputStream dout = new DataOutputStream(os);

          while (!Thread.interrupted()) {
            String msg = messagesToSend.take() + "$$$";
            byte[] msgInBytes = msg.getBytes(StandardCharsets.UTF_8);
            dout.write(msgInBytes, 0, msgInBytes.length);
          }
        }
      }
    } else {
      Socket sock = null;
      while (!Thread.interrupted()) {
        try {
          sock = new Socket((String) null, port);
          break;
        } catch (SocketException e) {
          System.out.println("DummyClient: Cannot connect to server. Have you started it?");
          System.out.println();
          Thread.sleep(5000);
        }
      }
      System.out.println("DummyClient: Connected @ port " + port);

      try (InputStream is = sock.getInputStream(); //
          OutputStream os = sock.getOutputStream();) {

        DataInputStream din = new DataInputStream(is);
        DataOutputStream dout = new DataOutputStream(os);

        while (!Thread.interrupted()) {
          String msg = messagesToSend.take() + "$$$";
          byte[] msgInBytes = msg.getBytes(StandardCharsets.UTF_8);
          dout.write(msgInBytes, 0, msgInBytes.length);
        }
      }

    }
    System.out.println("DummyClient: End of communication.");
  }

  @Override
  public void run() {
    try {
      main();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void queueMessage(String message) throws InterruptedException {
    messagesToSend.put(message);
  }
}
