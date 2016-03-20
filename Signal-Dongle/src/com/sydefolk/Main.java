package com.sydefolk;

/**
 * Created by dan-s on 06/03/2016.
 */
public class Main {
  public static void main(String [] args) throws InterruptedException
  {
    FakeDataGrahamSocket initiatingSocket = new FakeDataGrahamSocket();
    FakeDataGrahamSocket receivingSocket = new FakeDataGrahamSocket();

    initiatingSocket.registerOtherSocket(receivingSocket);
    receivingSocket.registerOtherSocket(initiatingSocket);

    CallOrchestration initiatorOrchestration = new CallOrchestration(initiatingSocket);
    CallOrchestration receiverOrchestration = new CallOrchestration(receivingSocket);

    initiatorOrchestration.initiateCall();
    receiverOrchestration.incomingCall();
    receiverOrchestration.acceptCall();
  }
}
