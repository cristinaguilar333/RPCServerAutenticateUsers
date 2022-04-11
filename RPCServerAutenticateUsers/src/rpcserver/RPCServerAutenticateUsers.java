/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rpcserver;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.*;
import org.json.JSONArray;
import org.json.JSONObject;

public class RPCServerAutenticateUsers {

    private static final String RPC_QUEUE_NAME = "rpc_queuelogin";



    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
            channel.queuePurge(RPC_QUEUE_NAME);

            channel.basicQos(1);

            System.out.println(" [x] Esperando solicitudes RPC");

            Object monitor = new Object();
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                AMQP.BasicProperties replyProps = new AMQP.BasicProperties
                        .Builder()
                        .correlationId(delivery.getProperties().getCorrelationId())
                        .build();

                String response = "";

                try {
                    String message = new String(delivery.getBody(), "UTF-8");

                    System.out.println("Recibí los siguientes datos(" + message + ")");
//                    JSONArray json = new JSONArray(message);
//                    String email = json.getJSONObject(json.length()).getString("email");
//                    String contrasenia = json.getJSONObject(json.length()).getString("contrasenia");
//                    for (int i = 0; i < json.length(); i++) {
//                        JSONObject jsonObject = json.getJSONObject(i);
//                        if (jsonObject.getString("email").equals(email) && jsonObject.getString("contrasenia").equals(contrasenia)) {
//                            response = jsonObject.toString();
//                        }
//                    }
                    
                    response = message;
                    System.out.println("Regresé los siguientes datos: "+ response);
                   
                } catch (RuntimeException e) {
                    System.out.println(" [.] " + e.toString());
                } finally {
                    channel.basicPublish("", delivery.getProperties().getReplyTo(), replyProps, response.getBytes("UTF-8"));
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    // RabbitMq consumer worker thread notifies the RPC server owner thread
                    synchronized (monitor) {
                        monitor.notify();
                    }
                }
            };

            channel.basicConsume(RPC_QUEUE_NAME, false, deliverCallback, (consumerTag -> { }));
            // Wait and be prepared to consume the message from RPC client.
            while (true) {
                synchronized (monitor) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}