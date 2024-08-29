package core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import exceptions.MalformedMessageException;
import generator.types.GenerationTask;
import generator.types.GenerationTask.EXERCISE_TYPE;
import generator.types.GenerationTask.SOLUTION_TYPE;
import java.util.HashMap;
import java.util.Map;

public class ControllerGenerationTask {

  public static String MESSAGE_DELIMITER = "##";

  private final String requestId;
  private final String userId;
  private final String courseId;
  private final EXERCISE_TYPE exerciseType;
  private final SOLUTION_TYPE solutionType;
  private final TASK_TYPE taskType;
  private final String origMessage;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private Map<String, Object> parameterMap;
  private TASK_STATUS taskStatus;
  private Exception failureException;
  private GenerationTask genTask;

  public ControllerGenerationTask(String message)
      throws MalformedMessageException, JsonProcessingException {
    this.origMessage = message;
    String[] parts = message.split(MESSAGE_DELIMITER, -1);
    if (parts.length != 7) {
      throw new MalformedMessageException(
          "Unable to split message according to delimiter '" + MESSAGE_DELIMITER + "'.");
    }
    requestId = parts[0];
    userId = parts[1];
    courseId = parts[2];
    String jsonParameters = parts[3];
    if (jsonParameters.equals("")) {
      parameterMap = new HashMap<>();
    } else {
      parameterMap = objectMapper.readValue(jsonParameters,
          new TypeReference<>() {
          });
    }
    fixEntryTypes(parameterMap);
    exerciseType = EXERCISE_TYPE.values()[Integer.parseInt(parts[4])];
    solutionType = SOLUTION_TYPE.values()[Integer.parseInt(parts[5])];
    taskType = TASK_TYPE.values()[Integer.parseInt(parts[6])];
  }

  // todo fix in frontend
  public void fixEntryTypes(Map<String, Object> map) {
    Object difficulty = map.get("difficulty");
    if (difficulty instanceof String) {
      map.put("difficulty", Integer.parseInt((String) difficulty));
    }
  }

  public String getRequestId() {
    return requestId;
  }

  public String getUserId() {
    return userId;
  }

  public String getCourseId() {
    return courseId;
  }

  public String getJsonParameters() throws JsonProcessingException {
    return objectMapper.writeValueAsString(parameterMap);
  }

  public Map<String, Object> getParameterMap() {
    return parameterMap;
  }

  public void setParameterMap(Map<String, Object> parameterMap) {
    this.parameterMap = parameterMap;
  }

  public TASK_TYPE getTaskType() {
    return taskType;
  }

  public String getOrigMessage() {
    return origMessage;
  }

  public EXERCISE_TYPE getExerciseType() {
    return exerciseType;
  }

  public SOLUTION_TYPE getSolutionType() {
    return solutionType;
  }

  public GenerationTask getGenTask() {
    return genTask;
  }

  public void setGenTask(GenerationTask genTask) {
    this.genTask = genTask;
  }

  public TASK_STATUS getTaskStatus() {
    return taskStatus;
  }

  public void setTaskStatus(TASK_STATUS taskStatus) {
    this.taskStatus = taskStatus;
  }

  public Exception getFailureException() {
    return failureException;
  }

  public void setFailureException(Exception failureException) {
    this.failureException = failureException;
  }

  enum TASK_TYPE {
    COURSE,
    PART_TREE,
    TOPIC,
    FDL
  }

  public enum TASK_STATUS {
    WAITING, PROCESSING, FINISHED, ERROR
  }
}
