
public class Matrix {

	public static void main(String[] args) {
		for(int w1 = 1 ; w1 <= 10; w1++)
		{
			for(int w2 = 1 ; w2 <= 10; w2++)
			{
				for(int w3 = 1 ; w3 <= 10; w3++)
				{
					for(int w4 = 1 ; w4 <= 10; w4++)
					{
						for(int w5 = 1 ; w5 <= 10; w5++)
						{
							int sum = w1+w2+w3+w4+w5;
							if(sum==10)
							{
								System.out.println(w1+" "+w2+" "+w3+" "+w4+" "+w5);
							}
						}
					}
				}
			}
		}
	}

}
