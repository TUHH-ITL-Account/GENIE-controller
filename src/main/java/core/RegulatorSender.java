package core;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;

public class RegulatorSender implements Runnable {

  final Socket regulatorSocket;
  final BlockingQueue<ControllerGenerationTask> regulatorQueue;
  final BlockingQueue<ControllerGenerationTask> generatorQueue;

  public RegulatorSender(Socket regulatorSocket,
      BlockingQueue<ControllerGenerationTask> regulatorQueue,
      BlockingQueue<ControllerGenerationTask> generatorQueue) {
    this.regulatorSocket = regulatorSocket;
    this.regulatorQueue = regulatorQueue;
    this.generatorQueue = generatorQueue;
  }

  @Override
  public void run() {
    try (OutputStream os = regulatorSocket.getOutputStream()) {
      DataOutputStream ds = new DataOutputStream(os);
      while (!Thread.interrupted()) {
        ControllerGenerationTask task = null;
        try {
          task = regulatorQueue.take();
          int sendTo;
          Object noAI = task.getParameterMap().get("noAI");
          //Object AI = task.getParameterMap().get("AI");
          if(noAI instanceof Boolean) {
            sendTo = 0;
          } else if(noAI instanceof String) {
            sendTo = Integer.parseInt((String)noAI);
          } else if(noAI instanceof Integer) {
            sendTo = (Integer)noAI;
          } else {
            sendTo = 0;
          }
          if (sendTo == 0) {//AI == null || Regulators.values()[(int)AI] == Regulators.NONE) {
            generatorQueue.put(task);
          } else {
            if (Controller.DEBUG) {
              System.out.println("Sending message to regulator: " + task.getOrigMessage());
            }
            ds.writeUTF(task.getOrigMessage());
          }
        } catch (SocketException | InterruptedException e) {
          if (task != null) {
            regulatorQueue.put(task);
          }
          System.out.println(
              "RegulatorSender interrupted or socket closed, terminating this RegulatorSender thread.");
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      System.out.println(
          "RegulatorSender thread interrupted, terminating...");
    }
  }
}
