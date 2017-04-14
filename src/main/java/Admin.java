import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by damian on 14.04.17.
 */
public class Admin {
    public static void main(String[] argv) throws Exception {

        // info
        System.out.println("ADMINISTRATOR");

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // exchange
        String EXCHANGE_NAME = "hospital";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        channel.queueDeclare("ankle", false, false, false, null);
        channel.queueDeclare("knee", false, false, false, null);
        channel.queueDeclare("elbow", false, false, false, null);
        channel.queueDeclare("examination", false, false, false, null);
        channel.queueDeclare("admin", false, false, false, null);
        channel.basicQos(1);

        Channel adminChannel = connection.createChannel();
        String ADMINISTRATION_EXCHANGE_NAME = "administration";
        adminChannel.exchangeDeclare(ADMINISTRATION_EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        adminChannel.basicQos(1);

//        channel.queueBind("ankle", EXCHANGE_NAME, "ankle.admin");
//        channel.queueBind("knee", EXCHANGE_NAME, "knee.admin");
//        channel.queueBind("elbow", EXCHANGE_NAME, "elbow.admin");
//        channel.queueBind("examination", EXCHANGE_NAME, "examination.admin");
//        channel.queueBind("admin", EXCHANGE_NAME, "*.admin");
//        channel.queueBind("doctor", EXCHANGE_NAME, "admin.*");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (!consumerTag.equals("admin.*")) {
                    String message = new String(body, "UTF-8");
                    System.out.println("ADMIN Received: " + message);
                }
            }
        };

        // start listening
        System.out.println("Waiting for messages...");
        channel.basicConsume("admin", true, "*.admin", consumer);


        while (true) {
            // read msg
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            System.out.println("Enter message: ");
            String message = br.readLine();
            // break condition
            if ("exit".equals(message)) {
                break;
            }
            String resultMsg = "##ADMIN : " + message + " ##";
            // publish
            adminChannel.basicPublish(ADMINISTRATION_EXCHANGE_NAME, "", null, resultMsg.getBytes("UTF-8"));
            System.out.println("Sent: " + message);
        }
    }
}
