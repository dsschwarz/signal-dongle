package com.sydefolk;

import javax.jms.*;

import com.audiointerface.DataGrahamSocket;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.omg.CORBA.OBJ_ADAPTER;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Julian on 3/22/2016.
 */
public class ActiveMQDataGrahamSocket extends DataGrahamSocket {

    protected Connection connection;
    protected Session session;
    protected MessageConsumer consumer;
    protected MessageProducer producer;
    protected String brokerUrl;
    protected static final String PHONE_TO_DONGLE_TOPIC = "phone_to_dongle";
    protected static final String DONGLE_TO_PHONE_TOPIC = "dongle_to_phone";
    protected static final long TIMEOUT = 5000;

    private Message latestMessage = null;
    final private Object lock;

    public ActiveMQDataGrahamSocket() throws JMSException{
        super();

        brokerUrl = "";
        lock = new Object();
        setupActiveMQ();
        consumer.setMessageListener(message -> {
            latestMessage = message;
            lock.notifyAll();
        });
    }

    protected void setupActiveMQ() throws JMSException {
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic phoneToDongleTopic = session.createTopic(PHONE_TO_DONGLE_TOPIC);
            Topic dongleToPhoneTopic = session.createTopic(DONGLE_TO_PHONE_TOPIC);
            // reverse of phone. Publish to dongle to phone, read from phone to dongle
            consumer = session.createConsumer(phoneToDongleTopic);
            producer = session.createProducer(dongleToPhoneTopic);
        } catch(JMSException e){
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void send(byte[] data) {

        try {
            BytesMessage msg = session.createBytesMessage();
            msg.writeBytes(data);
            producer.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] receive() {
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            Logger.getAnonymousLogger().log(Level.INFO, "Received ActiveMQ message");
            if(latestMessage instanceof BytesMessage){
                byte[] bytes = new byte[(int)((BytesMessage) latestMessage).getBodyLength()];
                ((BytesMessage) latestMessage).readBytes(bytes);
                return bytes;
            } else {
                Logger.getAnonymousLogger().log(Level.WARNING, "Wrong message type");
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close(){
        try {
            consumer.close();
            producer.close();
            session.close();
            connection.close();
        } catch(JMSException e){
            e.printStackTrace();
        }
    }
}
