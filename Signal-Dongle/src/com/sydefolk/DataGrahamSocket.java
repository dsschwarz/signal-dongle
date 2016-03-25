package com.sydefolk;

/**
 * Created by gblea on 2016-03-05.
 */
public abstract class DataGrahamSocket {
  public abstract void send(byte[] data);

  public abstract byte[] receive();
}