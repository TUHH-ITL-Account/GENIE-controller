package core;

import core.ControllerGenerationTask.TASK_STATUS;
import core.ControllerGenerationTask.TASK_TYPE;
import generator.Generator;
import generator.exceptions.UnfulfillableException;
import generator.types.GenerationTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;

public class GeneratorThread implements Runnable {

  private final Generator generator;
  private final BlockingQueue<ControllerGenerationTask> generatorQueue;
  private final BlockingQueue<ControllerGenerationTask> frontendQueue;

  private boolean canLoop = true;
  private boolean isPaused = false;

  public GeneratorThread(Generator generator,
      BlockingQueue<ControllerGenerationTask> generatorQueue,
      BlockingQueue<ControllerGenerationTask> frontendQueue) {
    this.generator = generator;
    this.generatorQueue = generatorQueue;
    this.frontendQueue = frontendQueue;
  }

  @Override
  public void run() {
    while (!Thread.interrupted()) {
      while (!canLoop) {
        isPaused = true;
        LockSupport.park();
      }
      isPaused = false;
      try {
        ControllerGenerationTask cTask = generatorQueue.take();
        while (!canLoop) {
          isPaused = true;
          LockSupport.park();
        }
        isPaused = false;
        if (Controller.DEBUG) {
          System.out.println("Processing generation task for: " + cTask.getOrigMessage());
        }
        cTask.setTaskStatus(TASK_STATUS.PROCESSING);
        try {
          GenerationTask gTask = new GenerationTask(cTask.getCourseId());
          gTask.setParameters(cTask.getParameterMap());
          gTask.setExType(cTask.getExerciseType());
          gTask.setSolType(cTask.getSolutionType());
          gTask.initMissingWithDefaults();
          cTask.setGenTask(gTask);
          System.out.println("Generating with c parameters:" + cTask.getJsonParameters());

          if (cTask.getTaskType() == TASK_TYPE.COURSE) {
            generator.generateFromCourse(cTask.getCourseId(), cTask.getGenTask());
          } else if (cTask.getTaskType() == TASK_TYPE.PART_TREE) {
            if (!cTask.getGenTask().getParameters().containsKey("course_part")) {
              throw new UnfulfillableException("Missing parameter 'course_part'");
            }
            // todo
          } else if (cTask.getTaskType() == TASK_TYPE.TOPIC) {
            String topic = (String) cTask.getGenTask().getParameters().get("topic_actual");
            if (topic == null) {
              throw new UnfulfillableException("Missing parameter 'topic_actual'");
            }
            cTask.getGenTask().setTopicName(topic);
            generator.generateFromTopic(cTask.getCourseId(), topic, cTask.getGenTask());
          } else if (cTask.getTaskType() == TASK_TYPE.FDL) {
            String topic = (String) cTask.getGenTask().getParameters().get("topic_actual");
            if (topic == null) {
              throw new UnfulfillableException("Missing parameter 'topic_actual'");
            }
            String fdl = (String) cTask.getGenTask().getParameters().get("fdl_actual");
            if (fdl == null) {
              throw new UnfulfillableException("Missing parameter 'fdl_actual'");
            }
            generator.generateFromFdl(cTask.getCourseId(), topic, fdl, cTask.getGenTask());
          }
          cTask.setTaskStatus(TASK_STATUS.FINISHED);
          //cTask.setParameterMap(cTask.getGenTask().getParameters()); //notwendig?
          frontendQueue.put(cTask);
        } catch (Exception e) {
          cTask.setTaskStatus(TASK_STATUS.ERROR);
          cTask.setFailureException(e);
          frontendQueue.put(cTask);
        }
      } catch (InterruptedException e) {
        System.out.println(
            "Generator thread interrupted, terminating...");
        break;
      }
    }
  }

  public boolean canLoop() {
    return canLoop;
  }

  public void setCanLoop(boolean canLoop) {
    this.canLoop = canLoop;
  }

  public boolean isPaused() {
    return isPaused;
  }
}
