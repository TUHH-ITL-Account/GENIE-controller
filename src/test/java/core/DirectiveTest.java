package core;

import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import core.ControllerTest.ControllerThread;
import exceptions.MalformedMessageException;
import generator.Generator;
import java.lang.Thread.State;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DirectiveTest {

  @Test
  public void testPausingGeneratorThread()
      throws MalformedMessageException, JsonProcessingException, InterruptedException {
    Generator generator = new Generator();
    BlockingQueue<ControllerGenerationTask> generatorQueue = new ArrayBlockingQueue<>(11);
    BlockingQueue<ControllerGenerationTask> frontendQueue = new ArrayBlockingQueue<>(11);
    GeneratorThreadDummy g = new GeneratorThreadDummy(generator, generatorQueue, frontendQueue);
    Thread t = new Thread(g);

    t.start();
    // pausing
    g.setCanLoop(false);
    generatorQueue.put(new ControllerGenerationTask("1##1##1##{}##1##1##1"));
    Thread.sleep(1000);
    assertThat("Thread is waiting after parking.", t.getState() == State.WAITING);
    // unpausing + unparking
    g.setCanLoop(true);
    LockSupport.unpark(t);
    Thread.sleep(1000);
    assertThat("Thread is running again", t.getState() != State.WAITING);
  }

  @Test
  public void testReloadingDirective() throws InterruptedException {
    DummyClient feObj = new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2frontend.sock");
    Thread frontendDummy = new Thread(feObj);
    Thread regulatorDummy = new Thread(new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2regulator.sock"));
    Thread controllerThread = new Thread(
        new ControllerThread("src/test/resources/exampleUDS.config"));
    frontendDummy.start();
    regulatorDummy.start();
    controllerThread.start();

    Thread.sleep(2000);
    feObj.queueMessage("##0##TechnischeLogistikSS21");
    Thread.sleep(2000);
    //System.out.println(Arrays.toString(controllerThread.getStackTrace()));
    assertThat("Controller did not crash", controllerThread.isAlive());
  }

  @Disabled
  public void testCachingDirective() {

  }

  @Test
  public void testShutdownDirective() throws InterruptedException {
    DummyClient feObj = new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2frontend.sock");
    Thread frontendDummy = new Thread(feObj);
    Thread regulatorDummy = new Thread(new DummyClient(System.getProperty("java.io.tmpdir"),
        "junixsocket-controller2regulator.sock"));
    Thread controllerThread = new Thread(
        new ControllerThread("src/test/resources/exampleUDS.config"));
    frontendDummy.start();
    regulatorDummy.start();
    controllerThread.start();

    Thread.sleep(2000);
    feObj.queueMessage("##2##");
    Thread.sleep(2000);
    assertThat("Controller thread is not alive anymore", !controllerThread.isAlive());
  }
}
