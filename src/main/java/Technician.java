/**
 * Created by damian on 10.04.17.
 */
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

public class Technician {
    public static void main(String[] argv) throws Exception {

        // info
        System.out.println("TECHNICIAN");
        System.out.println("Enter your specialization [Two from (ankle,knee,elbow)]");
        System.out.println("First: ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String specialization1 = br.readLine();
        System.out.println("Second: ");
        String specialization2 = br.readLine();

        // connection & channel
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        final Channel channel = connection.createChannel();

        // exchange
        final String EXCHANGE_NAME = "hospital";
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

        channel.basicQos(1,false);

        // queue & bind
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(specialization1, EXCHANGE_NAME, specialization1+".admin");
        channel.queueBind(specialization2, EXCHANGE_NAME, specialization2+".admin");
        channel.queueBind("examination", EXCHANGE_NAME, "examination.admin");
        channel.queueBind("admin", EXCHANGE_NAME, "examination.admin");
        channel.queueBind(channel.queueDeclare().getQueue(), EXCHANGE_NAME, "admin.tech");
        System.out.println("created queue: " + queueName);


        Channel adminChannel = connection.createChannel();
        String ADMINISTRATION_EXCHANGE_NAME = "administration";
        adminChannel.exchangeDeclare(ADMINISTRATION_EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        adminChannel.basicQos(1);
        String adminQueue = adminChannel.queueDeclare().getQueue();
        adminChannel.queueBind(adminQueue, ADMINISTRATION_EXCHANGE_NAME, "");


        final Random generator = new Random();

        // consumer (message handling)
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received: " + message);
                try {
                    Thread.sleep(generator.nextInt(4) * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                channel.basicPublish(EXCHANGE_NAME, "examination.admin", null, (message + " badanie").getBytes("UTF-8"));
                channel.basicAck(envelope.getDeliveryTag(),false);

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
        channel.basicConsume(specialization1, false, consumer);
        channel.basicConsume(specialization2, false, consumer);
        adminChannel.basicConsume(adminQueue, true, adminConsumer);
    }
}
