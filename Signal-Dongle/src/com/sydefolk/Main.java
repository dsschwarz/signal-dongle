package com.sydefolk;

import javax.jms.JMSException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dan-s on 06/03/2016.
 */
public class Main {
  public static void main(String [] args) throws InterruptedException
  {
    ActiveMQDataGrahamSocket socket;
    try {
      socket = new ActiveMQDataGrahamSocket();
    } catch (JMSException e) {
      Logger.getAnonymousLogger().log(Level.INFO, "Failure during socket initialization");
      return;
    }

    CallOrchestration callOrchestration = new CallOrchestration(socket);
  }
}
