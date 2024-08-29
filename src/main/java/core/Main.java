package core;

import exceptions.ConfigException;
import fdl.exceptions.DslException;
import generator.exceptions.ModelException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Main {

  private Main() {
    throw new UnsupportedOperationException("No instances");
  }

  public static void main(String[] args)
      throws IOException, ConfigException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, DslException, ModelException, InterruptedException {
    String configPath = "genie.config";
    boolean usesEnvVars = false;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-c") || args[i].equals("--config")) {
        i++;
        if (i < args.length) {
          configPath = args[i];
        }
      } else if (args[i].equals("-e") || args[i].equals("--env")) {
        usesEnvVars = true;
      }
    }

    Controller controller = usesEnvVars ? new Controller(true) : new Controller(configPath);
    controller.runController();
  }

}
