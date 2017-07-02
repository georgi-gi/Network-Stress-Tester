package bg.uni.sofia.fmi.java.stressTest;

public class Main {

	public static void main(String[] args) {
		try
		{
			Client client = new Client("java.voidland.org", 80);
			client.start();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
