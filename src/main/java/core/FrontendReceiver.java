package core;

import exceptions.MalformedMessageException;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

public class FrontendReceiver implements Runnable {

  //private static final Logger LOGGER = LogManager.getLogger();

  final Socket frontendSocket;
  final BlockingQueue<ControllerGenerationTask> receivedQueue;
  final BlockingQueue<Directive> directiveQueue;

  public FrontendReceiver(Socket socket, BlockingQueue<ControllerGenerationTask> receivedQueue,
      BlockingQueue<Directive> directiveQueue) {
    this.frontendSocket = socket;
    this.receivedQueue = receivedQueue;
    this.directiveQueue = directiveQueue;
  }

  @Override
  public void run() {
    try (InputStream is = frontendSocket.getInputStream()) {
      DataInputStream din = new DataInputStream(is);
      StringBuilder runningMessage = new StringBuilder();
      while (!Thread.interrupted()) {
        try {
          byte[] messageBuffer = new byte[2048];
          int messageRead = din.read(messageBuffer);
          if (messageRead == -1) {
            System.out.println("Read end of stream, terminating this FrontendReceiver thread.");
            break;
          }
          String inMessage = new String(messageBuffer, 0, messageRead, StandardCharsets.UTF_8);
          if (Controller.DEBUG) {
            System.out.println("Received message from frontend: " + inMessage);
          }
          runningMessage.append(inMessage);
          if (runningMessage.toString().contains("$$$")) {
            String[] splits = runningMessage.toString().split("\\$\\$\\$", -1);
            for (int i = 0; i < splits.length - 1; i++) {
              String message = splits[i];
              System.out.println(message);
              if (message.charAt(0) == '#' && message.charAt(1) == '#') {
                if (!directiveQueue.offer(new Directive(message))) {
                  System.out.printf(
                      "Queueing a directive failed! Remaining capacity: %d ; Queue size: %d",
                      directiveQueue.remainingCapacity(), directiveQueue.size());
                }
              } else {
                try {
                  ControllerGenerationTask task = new ControllerGenerationTask(message);
                  receivedQueue.put(task);
                  //LOGGER.info("Received and processed message: "+message);
                  if (Controller.DEBUG) {
                    System.out.println("Queued task for: " + message);
                  }
                } catch (MalformedMessageException e) {
                  //todo: send back error
                }
              }
            }
            runningMessage = new StringBuilder();
            runningMessage.append(splits[splits.length - 1]);
          }
        } catch (SocketException e) {
          System.out.println(
              "Stream closed while waiting for read. Terminating FrontendReceiver thread...");
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      System.out.println(
          "FrontendReceiver thread interrupted, terminating...");
    }
  }
}
