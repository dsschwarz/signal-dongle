package com.sydefolk;

public interface Callback {
  abstract void doSomething();

  static Callback emptyCallback() {
    return new EmptyCallback();
  }
}

class EmptyCallback implements Callback {
  @Override
  public void doSomething() {

  }
}