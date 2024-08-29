package core;

import exceptions.ConfigException;
import fdl.exceptions.DslException;
import generator.Generator;
import generator.exceptions.ModelException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Thread.State;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Stream;

public class Controller {

  public static boolean DEBUG = true;
  private int MAX_THREADS;
  private int QUEUE_SIZE;
  private String LOG_DIR;
  private String[] PRELOADED_MODELS;
  private String MODEL_DIR;
  private ConnectionType CONNECTION_TYPE;
  private int REGULATOR_PORT;
  private int FRONTEND_PORT;
  private String UDSOCKETS_PATH;

  //private static final Logger LOGGER = LogManager.getLogger();

  public Controller() {
  }

  public Controller(String configName) throws IOException, ConfigException {
    Properties config = loadConfig(configName);
    checkConfig(config);
    setConfigProperties(config);
  }

  public Controller(boolean useEnvVars) throws IOException, ConfigException {
    Properties config = loadConfigFromEnvVars();
    checkConfig(config);
    setConfigProperties(config);
  }

  public void runController()
      throws IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, DslException, ModelException, InterruptedException {
    System.out.println("Starting controller...");

    Generator generator = new Generator();
    generator.setup();
    //generator.setFdlDirectory(FDL_DIR);
    for (String toLoad : PRELOADED_MODELS) {
      System.out.println("Caching model: " + toLoad);
      generator.cacheModel(MODEL_DIR, toLoad, true);
    }

    BlockingQueue<ControllerGenerationTask> regulatorQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    BlockingQueue<ControllerGenerationTask> generatorQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    BlockingQueue<ControllerGenerationTask> frontendQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);
    BlockingQueue<Directive> directiveQueue = new ArrayBlockingQueue<>(QUEUE_SIZE);

    Thread[] generatorThreads = new Thread[MAX_THREADS];
    GeneratorThread[] generatorObjects = new GeneratorThread[MAX_THREADS];
    for (int i = 0; i < generatorThreads.length; i++) {
      GeneratorThread tmp = new GeneratorThread(generator, generatorQueue, frontendQueue);
      generatorObjects[i] = tmp;
      generatorThreads[i] = new Thread(tmp);
    }
    Thread generatorWatcher = new Thread(new GenerationThreadWatcher(generatorThreads));

    Thread feHandler;
    Thread reHandler;
    if (CONNECTION_TYPE == ConnectionType.TCP) {
      feHandler = new Thread(
          new FrontendHandler(FRONTEND_PORT, regulatorQueue, frontendQueue, directiveQueue));
      System.out.println("Controller: Starting TCP server for frontend on port " + FRONTEND_PORT);

      reHandler = new Thread(new RegulatorHandler(REGULATOR_PORT, regulatorQueue, generatorQueue));
      System.out.println("Controller: Starting TCP server for regulator on port " + REGULATOR_PORT);
    } else {
      feHandler = new Thread(
          new FrontendHandler(UDSOCKETS_PATH, regulatorQueue, frontendQueue, directiveQueue));
      System.out.println("Controller: Starting UDX server for frontend in " + UDSOCKETS_PATH);

      reHandler = new Thread(new RegulatorHandler(UDSOCKETS_PATH, regulatorQueue, generatorQueue));
      System.out.println("Controller: Starting UDX server for regulator in " + UDSOCKETS_PATH);
    }
    feHandler.setDaemon(false);
    feHandler.start();
    reHandler.setDaemon(false);
    reHandler.start();

    boolean running = true;
    while (!Thread.interrupted() && running) {
      for (Thread generatorThread : generatorThreads) {
        // daemon means Threads keep running; if used add proper shutdown!
        generatorThread.setDaemon(false);
        generatorThread.start();
      }
      generatorWatcher.setDaemon(false);
      generatorWatcher.start();
      System.out.println("Controller: Started all threads.");

      while (!Thread.interrupted() && running) {
        Directive dir = directiveQueue.take();
        System.out.println("Got directive.");
        switch (dir.getDirective()) {
          case RELOAD_MODEL -> {
            System.out.println("Reloading model: " + dir.getVarArg());
            for (GeneratorThread generatorObj : generatorObjects) {
              generatorObj.setCanLoop(false);
            }

            boolean allPaused = false;
            for (int i = 0; i < 5 && !allPaused; i++) {
              allPaused = generatorThreads[0].getState() == State.WAITING;
              for (Thread generatorThread : generatorThreads) {
                allPaused = allPaused && (generatorThread.getState() == State.WAITING);
              }
              Thread.sleep(2000);
            }

            generator.reloadModel(dir.getVarArg(), true);

            for (GeneratorThread generatorObj : generatorObjects) {
              generatorObj.setCanLoop(true);
            }
            for (Thread generatorThread : generatorThreads) {
              LockSupport.unpark(generatorThread);
            }
          }
          case CACHE_MODEL -> {
            if (!generator.getCacheMap().containsKey(dir.getVarArg())) {
              generator.cacheModel(MODEL_DIR, dir.getVarArg(), true);
              System.out.println("Caching additional model: " + dir.getVarArg());
            }
          }
          case SHUTDOWN -> {
            running = false;
            System.out.println("Shutting down controller...");
            //todo: notify handlers
            for (Thread generatorThread : generatorThreads) {
              generatorThread.interrupt();
            }
            // sockets and in-/output streams are automatically closed upon leaving their try()
          }
        }
      }
      System.out.println("Controller exited main loop");

    }
    System.out.println("Controller shut down");

  }

  protected Properties loadConfig(String fileName) throws IOException {
    Properties prop = new Properties();
    FileInputStream fis = new FileInputStream(fileName);
    prop.load(fis);
    return prop;
  }

  private Properties loadConfigFromEnvVars() {
    Properties prop = new Properties();
    // since getOrDefault is based on a null check, env-vars not being set should be unproblematic
    prop.setProperty("udsockets_use_temp", System.getenv("GENIE_UDSOCKETS_USE_TEMP"));
    prop.setProperty("udsockets_path", System.getenv("GENIE_UDSOCKETS_PATH"));
    prop.setProperty("log_dir", System.getenv("GENIE_LOG_DIR"));
    prop.setProperty("max_threads", System.getenv("GENIE_MAX_THREADS"));
    prop.setProperty("queue_size", System.getenv("GENIE_QUEUE_SIZE"));
    prop.setProperty("model_dir", System.getenv("GENIE_MODEL_DIR"));
    prop.setProperty("preloaded_models", System.getenv("GENIE_PRELOADED_MODELS"));
    prop.setProperty("connection_type", System.getenv("GENIE_CONNECTION_TYPE"));
    prop.setProperty("regulator_port", System.getenv("GENIE_REGULATOR_PORT"));
    prop.setProperty("frontend_port", System.getenv("GENIE_FRONTEND_PORT"));
    return prop;
  }

  protected void checkConfig(Properties prop) throws ConfigException {
    // check for mandatory properties
    /*if(!prop.containsKey("udsocket_path")) {
      throw new ConfigException("Missing mandatory property in config: udsocket_path");
    }
    if(!prop.containsKey("log_path")) {
      throw new ConfigException("Missing mandatory property in config: log_path");
    }*/
    // check types of properties
    for (String key : prop.stringPropertyNames()) {
      switch (key) {
        case "max_threads":
          try {
            Integer.parseInt(prop.getProperty(key));
          } catch (NumberFormatException e) {
            throw new ConfigException("Property 'max_threads' must be an integer.");
          }
          break;
        case "queue_size":
          try {
            Integer.parseInt(prop.getProperty(key));
          } catch (NumberFormatException e) {
            throw new ConfigException("Property 'queue_size' must be an integer.");
          }
          break;
        case "model_dir":
          File mdir = new File(prop.getProperty(key));
          if (!mdir.exists()) {
            if (!mdir.mkdirs()) {
              throw new ConfigException(
                  "Unable to create non-existent directories specified in 'model_dir'.");
            }
          }
          if (!mdir.isDirectory()) {
            throw new ConfigException("'model_dir':'" + mdir.getAbsoluteFile()
                + "' does not point to a directory, but a file.");
          }
          if (!mdir.canRead()) {
            throw new ConfigException("No read permissions in 'model_dir'.");
          }
          break;
        case "log_dir":
          File ldir = new File(prop.getProperty(key));
          if (!ldir.exists()) {
            if (!ldir.mkdirs()) {
              throw new ConfigException(
                  "Unable to create non-existent directories specified in 'log_dir'.");
            }
          }
          if (!ldir.isDirectory()) {
            throw new ConfigException("'log_dir' does not point to a directory, but a file.");
          }
          if (!ldir.canWrite()) {
            throw new ConfigException("No write permissions in 'log_dir'.");
          }
      }
    }
  }

  private void setConfigProperties(Properties prop) {
    String workingDir = System.getProperty("user.dir");
    UDSOCKETS_PATH =
        Boolean.parseBoolean((String) prop.getOrDefault("udsockets_use_temp", "false")) ?
            System.getProperty("java.io.tmpdir")
            : (String) prop.getOrDefault("udsockets_path", System.getProperty("java.io.tmpdir"));
    LOG_DIR = (String) prop.getOrDefault("log_dir", workingDir + "/logs");
    MAX_THREADS = Integer.parseInt((String) prop.getOrDefault("max_threads", "10"));
    QUEUE_SIZE = Integer.parseInt((String) prop.getOrDefault("queue_size", "1000"));
    MODEL_DIR = (String) prop.getOrDefault("model_dir", workingDir + "/models");
    PRELOADED_MODELS = Stream.of(((String) prop.getOrDefault("preloaded_models", ""))
        .split(",")).filter(w -> !w.isEmpty()).toArray(String[]::new);
    CONNECTION_TYPE = prop.getOrDefault("connection_type", "uds").equals("tcp") ? ConnectionType.TCP
        : ConnectionType.UDS;
    REGULATOR_PORT = Integer.parseInt((String) prop.getOrDefault("regulator_port", "4242"));
    FRONTEND_PORT = Integer.parseInt((String) prop.getOrDefault("frontend_port", "4243"));
  }

  enum ConnectionType {
    TCP, UDS
  }
}
