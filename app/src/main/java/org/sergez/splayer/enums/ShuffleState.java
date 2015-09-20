package org.sergez.splayer.enums;

/**
 * @author Sergii Zhuk
 */
public enum ShuffleState {
  SHUFFLE_OFF("OFF"), SHUFFLE_ON("ON");   // TODO locale

  private final String label;

  ShuffleState(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

}
