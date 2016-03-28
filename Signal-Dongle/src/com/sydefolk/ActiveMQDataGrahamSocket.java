package com.sydefolk;

import javax.jms.*;

import org.apache.activemq.ActiveMQConnectionFactory;

import java.util.Arrays;
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
    Topic phoneToDongleTopic;
    protected String brokerUrl;
    protected final int INSTANCE = 1;
    protected final String PHONE_TO_DONGLE_TOPIC = INSTANCE == 1 ? "phone_to_dongle" : "best_phone_to_dongle_topic";
    protected final String DONGLE_TO_PHONE_TOPIC = INSTANCE == 1 ? "dongle_to_phone" : "best_dongle_to_phone_topic";
    protected static final long TIMEOUT = 5000;

    private Message latestMessage = null;
    final private Object lock;

    public ActiveMQDataGrahamSocket() throws JMSException{
        super();

        brokerUrl = "tcp://192.168.0.187:61616";
        lock = new Object();
        setupActiveMQ();
        consumer.setMessageListener(message -> {
            try {
                latestMessage = message;
                synchronized (lock) {
                    lock.notifyAll();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    protected void setupActiveMQ() throws JMSException {
        try {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(brokerUrl);
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            phoneToDongleTopic = session.createTopic(PHONE_TO_DONGLE_TOPIC);
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
            Logger.getAnonymousLogger().log(Level.INFO, "Received ActiveMQ message (" + phoneToDongleTopic.getTopicName() + ")");
            if(latestMessage instanceof BytesMessage){
                byte[] bytes = new byte[(int)((BytesMessage) latestMessage).getBodyLength()];
                ((BytesMessage) latestMessage).readBytes(bytes);
//                System.out.println(Arrays.toString(bytes));
//                System.out.println(bytes.length);
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
