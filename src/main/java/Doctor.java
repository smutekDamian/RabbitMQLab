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
        String EXCHANGE_NAME = "hospital1";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        channel.queueDeclare("ankle", false, false, false, null);
        channel.queueDeclare("knee", false, false, false, null);
        channel.queueDeclare("elbow", false, false, false, null);
        channel.queueDeclare("examination", false, false, false, null);
        channel.basicQos(1);

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("#####################");
                System.out.println("Received: " + message);
                System.out.println("#####################");
            }
        };

        // start listening
        System.out.println("Waiting for messages...");
        channel.basicConsume("examination", true, consumer);


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
            channel.basicPublish(EXCHANGE_NAME, typeOfExamination, null, patientName.getBytes("UTF-8"));
            System.out.println("Sent: " + typeOfExamination + " and " + patientName);
        }
    }
}
