package core;

public class Directive {

  private final DIRECTIVES directive;
  private final String varArg;

  public Directive(DIRECTIVES directive, String varArg) {
    this.directive = directive;
    this.varArg = varArg;
  }

  public Directive(String message) {
    String[] split = message.split("##", -1);
    this.directive = DIRECTIVES.values()[Integer.parseInt(split[1])];
    this.varArg = split[2];
  }

  public DIRECTIVES getDirective() {
    return directive;
  }

  public String getVarArg() {
    return varArg;
  }

  enum DIRECTIVES {
    RELOAD_MODEL,
    CACHE_MODEL,
    SHUTDOWN
  }
}
