package com.sydefolk.audio;

/**
 * An exception related to the com.sydefolk.audio subsystem.
 *
 * @author Stuart O. Anderson
 */
public class AudioException extends Exception {
  private final String clientMessage;

  public AudioException(String clientMessage) {
    this.clientMessage = clientMessage;
  }

  public AudioException(AudioException cause) {
    super(cause);
    this.clientMessage = cause.clientMessage;
  }

  public String getClientMessage() {
    return clientMessage;
  }
}
