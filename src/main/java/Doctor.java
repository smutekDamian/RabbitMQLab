/**
 * Created by damian on 10.04.17.
 */
import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Doctor {
    public static void main(String[] argv) throws Exception {

        // info
        System.out.println("DOCTOR");

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // exchange
        String EXCHANGE_NAME = "hospital";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        channel.queueBind("ankle", EXCHANGE_NAME, "ankle.admin");
        channel.queueBind("knee", EXCHANGE_NAME, "knee.admin");
        channel.queueBind("elbow", EXCHANGE_NAME, "elbow.admin");
        channel.queueBind("examination", EXCHANGE_NAME, "examination.admin");
        channel.queueBind("admin", EXCHANGE_NAME, "*.admin");
        channel.basicQos(1);

        Channel adminChannel = connection.createChannel();
        String ADMINISTRATION_EXCHANGE_NAME = "administration";
        adminChannel.exchangeDeclare(ADMINISTRATION_EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        adminChannel.basicQos(1);
        String adminQueue = adminChannel.queueDeclare().getQueue();
        adminChannel.queueBind(adminQueue, ADMINISTRATION_EXCHANGE_NAME, "");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("#####################");
                System.out.println("Received: " + message);
                System.out.println("#####################");
            }
        };
        Consumer adminConsumer = new DefaultConsumer(channel){
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received: " + message);
            }
        };

        // start listening
        System.out.println("Waiting for messages...");
        channel.basicConsume("examination", true, consumer);
        adminChannel.basicConsume(adminQueue,true, adminConsumer);


        while (true) {
            // read msg
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter type of medical examination [ankle,knee or elbow]: ");
            String typeOfExamination = br.readLine();
            System.out.println("Enter patient Name: ");
            String patientName = br.readLine();
            // break condition
            if ("exit".equals(typeOfExamination) || "exit".equals(patientName)) {
                break;
            }
            // publish
            channel.basicPublish(EXCHANGE_NAME, typeOfExamination+".admin", null, patientName.getBytes("UTF-8"));
            System.out.println("Sent: " + typeOfExamination + " and " + patientName + " and" + typeOfExamination+".admin" );
        }
    }
}
