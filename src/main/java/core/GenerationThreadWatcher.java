package core;

import java.util.Arrays;

public class GenerationThreadWatcher implements Runnable {

  Thread[] generatorThreads;

  public GenerationThreadWatcher(Thread[] generatorThreads) {
    this.generatorThreads = generatorThreads;
  }

  @Override
  public void run() {
    try {
      while (!Thread.interrupted()) {
        for (Thread genThread : generatorThreads) {
          if(!genThread.isAlive()) {
            System.out.println("### Generator Thread Crash ###");
            System.out.println(Arrays.toString(genThread.getStackTrace()));
            genThread.start();
          }
        }
        Thread.sleep(5000);
      }
    } catch (Exception e) {
      System.out.println("### Generator Watcher Exception ###");
      e.printStackTrace();
    }
  }
}
