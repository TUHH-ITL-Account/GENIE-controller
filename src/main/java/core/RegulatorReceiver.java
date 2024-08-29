package core;

import exceptions.MalformedMessageException;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

public class RegulatorReceiver implements Runnable {

  final Socket regulatorSocket;
  final BlockingQueue<ControllerGenerationTask> generatorQueue;

  public RegulatorReceiver(Socket socket, BlockingQueue<ControllerGenerationTask> generatorQueue) {
    this.regulatorSocket = socket;
    this.generatorQueue = generatorQueue;
  }

  @Override
  public void run() {
    try (InputStream is = regulatorSocket.getInputStream()) {
      DataInputStream din = new DataInputStream(is);
      while (!Thread.interrupted()) {
        try {
          String message = din.readUTF();
          if (Controller.DEBUG) {
            System.out.println("Received message from regulator: " + message);
          }
          ControllerGenerationTask cTask = new ControllerGenerationTask(message);
          generatorQueue.put(cTask);
        } catch (SocketException e) {
          System.out.println(
              "Read end of stream, terminating this RegulatorReceiver thread.");
          break;
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    } catch (IOException | MalformedMessageException e) {
      e.printStackTrace();
    }
  }
}
