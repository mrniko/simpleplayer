package org.sergez.splayer.enums;

/**
 * @author Sergii Zhuk
 */
public enum RepeatState {
  NO_REPEAT("OFF"), REPEAT_CURRENT_TRACK("Current track"), REPEAT_ALL_FILES("All files in list"); // TODO locale

  private final String label;

  RepeatState(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }
}