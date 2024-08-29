package core;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

public class FrontendHandler implements Runnable {

  private final BlockingQueue<ControllerGenerationTask> regulatorQueue;
  private final BlockingQueue<ControllerGenerationTask> frontendQueue;
  private final BlockingQueue<Directive> directiveQueue;
  ServerSocket frontendServer;

  /**
   * Use this constructor to utilize Unix Domain Sockets
   *
   * @param frontendSocketPath path to the sock file
   */
  public FrontendHandler(String frontendSocketPath,
      BlockingQueue<ControllerGenerationTask> regulatorQueue,
      BlockingQueue<ControllerGenerationTask> frontendQueue,
      BlockingQueue<Directive> directiveQueue)
      throws IOException {
    final File frontendSocketFile = new File(new File(frontendSocketPath),
        "junixsocket-controller2frontend.sock");
    AFUNIXServerSocket frontendServer = AFUNIXServerSocket.newInstance();
    frontendServer.bind(AFUNIXSocketAddress.of(frontendSocketFile));
    this.frontendServer = frontendServer;
    this.regulatorQueue = regulatorQueue;
    this.frontendQueue = frontendQueue;
    this.directiveQueue = directiveQueue;
  }

  /**
   * Use this constructor to utilize TCP/IP sockets
   *
   * @param port the port on which the controller-to-frontend socket is hosted
   */
  public FrontendHandler(int port, BlockingQueue<ControllerGenerationTask> regulatorQueue,
      BlockingQueue<ControllerGenerationTask> frontendQueue,
      BlockingQueue<Directive> directiveQueue) throws IOException {
    this.frontendServer = new ServerSocket(port);

    this.regulatorQueue = regulatorQueue;
    this.frontendQueue = frontendQueue;
    this.directiveQueue = directiveQueue;
  }

  @Override
  public void run() {
    boolean running = true;
    while (!Thread.interrupted() && running) {
      try (Socket frontendSocket = frontendServer.accept()) {
        System.out.println("Frontend connected.");
        Thread frontendReceiver = new Thread(
            new FrontendReceiver(frontendSocket, regulatorQueue, directiveQueue));
        Thread frontendSender = new Thread(new FrontendSender(frontendSocket, frontendQueue));

        frontendReceiver.setDaemon(false);
        frontendSender.setDaemon(false);

        frontendReceiver.start();
        frontendSender.start();

        while (!Thread.interrupted() && running) {
          Thread.sleep(60000);
          boolean recAlive = frontendReceiver.isAlive();
          boolean senAlive = frontendSender.isAlive();
          if (!recAlive || !senAlive) {
            System.out.println(
                "Something happened to the frontend socket? Attempting reconnect...");
            if (recAlive) {
              frontendReceiver.interrupt();
            }
            if (senAlive) {
              frontendSender.interrupt();
            }
            break;
          }
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println(
          "FrontendHandler encountered an exception. Awaiting reconnection on frontend socket...");
    }
    System.out.println("FrontendHandler exited all loops.");
  }
}
