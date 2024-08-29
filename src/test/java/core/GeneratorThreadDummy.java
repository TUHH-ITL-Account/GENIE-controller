package core;

import core.ControllerGenerationTask.TASK_STATUS;
import generator.Generator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;

public class GeneratorThreadDummy extends GeneratorThread {

  private final Generator generator;
  private final BlockingQueue<ControllerGenerationTask> generatorQueue;
  private final BlockingQueue<ControllerGenerationTask> frontendQueue;

  private boolean canLoop = true;
  private boolean isPaused = false;

  public GeneratorThreadDummy(Generator generator,
      BlockingQueue<ControllerGenerationTask> generatorQueue,
      BlockingQueue<ControllerGenerationTask> frontendQueue) {
    super(generator, generatorQueue, frontendQueue);
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

        System.out.println("Found message");

        while (!canLoop) {
          isPaused = true;
          LockSupport.park();
        }
        isPaused = false;

        System.out.println("Processing generation task for: " + cTask.getOrigMessage());

        cTask.setTaskStatus(TASK_STATUS.PROCESSING);
        try {
          System.out.println("Generator Thread Main Part");
          Thread.sleep(10000);
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
