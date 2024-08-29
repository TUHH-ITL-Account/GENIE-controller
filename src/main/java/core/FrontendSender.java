package core;

import com.fasterxml.jackson.core.JsonProcessingException;
import core.ControllerGenerationTask.TASK_STATUS;
import generator.types.GenerationTask.EXERCISE_TYPE;
import generator.types.GenerationTask.SOLUTION_TYPE;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

public class FrontendSender implements Runnable {

  //private static final Logger LOGGER = LogManager.getLogger();

  private static final String PART_DELIMITER = "##";
  private static final String MESSAGE_TERMINATOR = "$$$";

  final Socket frontendSocket;
  final BlockingQueue<ControllerGenerationTask> frontendQueue;

  public FrontendSender(Socket socket, BlockingQueue<ControllerGenerationTask> receivedQueue) {
    this.frontendSocket = socket;
    this.frontendQueue = receivedQueue;
  }

  @Override
  public void run() {
    try (OutputStream os = frontendSocket.getOutputStream()) {
      DataOutputStream ds = new DataOutputStream(os);
      while (!Thread.interrupted()) {
        ControllerGenerationTask cTask = null;
        try {
          cTask = frontendQueue.take();
          String message = taskToMessage(cTask) + MESSAGE_TERMINATOR;
          byte[] msgInBytes = message.getBytes(StandardCharsets.UTF_8);
          ds.write(msgInBytes, 0, msgInBytes.length);
          if (Controller.DEBUG) {
            System.out.println("Sent message to frontend: " + message);
          }
          //LOGGER.info("Sent message: "+message);
        } catch (SocketException | InterruptedException e) {
          if (cTask != null) {
            frontendQueue.put(cTask);
          }
          System.out.println(
              "FrontendSender interrupted or socket closed, terminating this FrontendSender thread.");
          break;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      System.out.println(
          "FrontendSender thread interrupted, terminating...");
    }
  }

  private String taskToMessage(ControllerGenerationTask cTask) throws JsonProcessingException {
    if (cTask.getTaskStatus() == TASK_STATUS.FINISHED) {
      String exercise = "";
      if (cTask.getExerciseType() == EXERCISE_TYPE.FULL_HTML) {
        exercise = cTask.getGenTask().getFullHtmlExercise();
      } else if (cTask.getExerciseType() == EXERCISE_TYPE.PART_HTML) {
        exercise = cTask.getGenTask().getPartHtmlExercise();
      } //else if(cTask.getExerciseType() == EXERCISE_TYPE.TEX) {}
      String solution = "";
      if (cTask.getSolutionType() == SOLUTION_TYPE.FULL_HTML) {
        solution = cTask.getGenTask().getFullHtmlExercise();
      } else if (cTask.getSolutionType() == SOLUTION_TYPE.PART_HTML) {
        solution = cTask.getGenTask().getPartHtmlSolution();
      } else if (cTask.getSolutionType() == SOLUTION_TYPE.JSON) {
        solution = cTask.getGenTask().getJsonSolution();
      }
      return cTask.getRequestId() + PART_DELIMITER + cTask.getJsonParameters() + PART_DELIMITER
          + exercise + PART_DELIMITER
          + solution;
    } else if (cTask.getTaskStatus() == TASK_STATUS.ERROR) {
      return "ERROR" + PART_DELIMITER + cTask.getRequestId() + PART_DELIMITER
          + cTask.getFailureException().getMessage();
    } else {
      throw new IllegalStateException();
    }
  }
}
