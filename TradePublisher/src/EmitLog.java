import com.rabbitmq.client.*;

public class EmitLog {

  //private static final String EXCHANGE_NAME = "logs";
  private final static String QUEUE_NAME = "hello";
  public static void main(String[] argv) throws Exception {

    try 
    {
    	ConnectionFactory factory = new ConnectionFactory();
    	factory.setHost("localhost");
    	Connection connection = factory.newConnection();

		for(int i = 0 ; i < 10; i++)
		{
			Channel channel = connection.createChannel();
			String Q = QUEUE_NAME+i;
			channel.queueDeclare(Q, false, false, false, null);
			
			if(i%2==0)
			{
				String message = "Hello"+i+" World!";
				channel.basicPublish("", Q, null, message.getBytes());
				//System.out.println(" [x] Sent '" + message + "'");
					
			}
			
		}
	    
		
    } catch(Exception e)
    {
    	e.printStackTrace();
    }
  }
}