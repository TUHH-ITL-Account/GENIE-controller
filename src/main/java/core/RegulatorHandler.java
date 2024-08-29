package core;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

public class RegulatorHandler implements Runnable {

  private final BlockingQueue<ControllerGenerationTask> regulatorQueue;
  private final BlockingQueue<ControllerGenerationTask> generatorQueue;
  ServerSocket regulatorServer;

  /**
   * Use this constructor to utilize Unix Domain Sockets
   *
   * @param regulatorSocketPath path to the sock file
   */
  public RegulatorHandler(String regulatorSocketPath,
      BlockingQueue<ControllerGenerationTask> regulatorQueue,
      BlockingQueue<ControllerGenerationTask> generatorQueue)
      throws IOException {
    final File regulatorSocketFile = new File(new File(regulatorSocketPath),
        "junixsocket-controller2regulator.sock");
    AFUNIXServerSocket regulatorServer = AFUNIXServerSocket.newInstance();
    regulatorServer.bind(AFUNIXSocketAddress.of(regulatorSocketFile));
    this.regulatorServer = regulatorServer;

    this.regulatorQueue = regulatorQueue;
    this.generatorQueue = generatorQueue;
  }

  /**
   * Use this constructor to utilize TCP/IP sockets
   *
   * @param port the port on which the controller-to-regulator socket is hosted
   */
  public RegulatorHandler(int port, BlockingQueue<ControllerGenerationTask> regulatorQueue,
      BlockingQueue<ControllerGenerationTask> generatorQueue) throws IOException {
    this.regulatorServer = new ServerSocket(port);

    this.regulatorQueue = regulatorQueue;
    this.generatorQueue = generatorQueue;
  }

  @Override
  public void run() {
    boolean running = true;
    while (!Thread.interrupted() && running) {
      System.out.println("Regulator connected.");
      try (Socket regulatorSocket = regulatorServer.accept()) {
        Thread regulatorReceiver = new Thread(
            new RegulatorReceiver(regulatorSocket, generatorQueue));
        Thread regulatorSender = new Thread(
            new RegulatorSender(regulatorSocket, regulatorQueue, generatorQueue));

        regulatorReceiver.setDaemon(false);
        regulatorSender.setDaemon(false);

        regulatorReceiver.start();
        regulatorSender.start();

        while (!Thread.interrupted() && running) {
          Thread.sleep(60000);
          boolean recAlive = regulatorReceiver.isAlive();
          boolean senAlive = regulatorSender.isAlive();
          if (!recAlive || !senAlive) {
            System.out.println(
                "Something happened to the regulator socket? Attempting reconnect...");
            if (recAlive) {
              regulatorReceiver.interrupt();
            }
            if (senAlive) {
              regulatorSender.interrupt();
            }
            break;
          }
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
      System.out.println(
          "RegulatorHandler encountered an exception. Awaiting reconnection on regulator socket...");
    }
    System.out.println("RegulatorHandler exited all loops.");
  }
}
