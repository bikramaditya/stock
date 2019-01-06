import com.rabbitmq.client.*;

public class ReceiveLogs {

	private final static String QUEUE_NAME = "hello";

	public static void main(String[] argv) throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost("localhost");

		for (int i = 0; i < 10; i++) {
			createNewSubscriber(factory, i);
		}
	}

	private static void createNewSubscriber(ConnectionFactory factory, int i) {
		Thread t = new Thread() {
			public void run() {
				setName("Subs-Thread-" + i);
				String Q = QUEUE_NAME+i;
				try {
					ConnectionFactory factory = new ConnectionFactory();
				    factory.setHost("localhost");
				    Connection connection = factory.newConnection();
				    Channel channel = connection.createChannel();

				    channel.queueDeclare(Q, false, false, false, null);
				    System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

				    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				        String message = new String(delivery.getBody(), "UTF-8");
				        System.out.println(getName()+" Received '" + message + "'");
				    };
				    channel.basicConsume(Q, true, deliverCallback, consumerTag -> { });
				    
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		t.start();

	}
}