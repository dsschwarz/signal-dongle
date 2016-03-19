package com.sydefolk;

/**
 * Created by dan-s on 06/03/2016.
 */
public class Main {
  public static void main(String [] args) throws InterruptedException
  {
    FakeDataGrahamSocket dataGrahamSocket = new FakeDataGrahamSocket();
    CallOrchestration callOrchestration = new CallOrchestration(dataGrahamSocket);

//    Thread.sleep(2000);
    callOrchestration.customSocket.initiateCall();
  }
}
