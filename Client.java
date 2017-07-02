package bg.uni.sofia.fmi.java.stressTest;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

public class Client{

	String request;
	AtomicInteger cntSuccessfulRequests;
	CyclicBarrier barrier;
	Long longestTimeForResponse;
	Long barrierReleased;
	
	private String host = null;
	private int port = 0;
	
	public Client(String host, int port) throws UnknownHostException, IOException
	{
		this.host = host;
		this.port = port;
		this.cntSuccessfulRequests = new AtomicInteger(0);
		longestTimeForResponse = (long) 0;
	}
	
	public void start()
	{
		
		String line1 = null;
		String line2 = null;
		
		try(BufferedReader readRequest = new BufferedReader(new FileReader("example_request.txt")))
		{
			line1 = readRequest.readLine();
			line2 = readRequest.readLine();			
		} catch (IOException e1) {
			System.out.println("Problem with the file with the sample request.");
			e1.printStackTrace();
		}
		
		
		System.out.println("Client connected to server");
			
		this.request = line1 + "\r\n" + line2 + "\r\n\r\n";
			
		int numThreads = 20;
		cntSuccessfulRequests.set(20);
		while (cntSuccessfulRequests.get() == numThreads)
		{
			this.barrier = new CyclicBarrier(numThreads);
			
			cntSuccessfulRequests.set(0);
			startThreads(numThreads);
			if (cntSuccessfulRequests.get() == numThreads)
				System.out.println(numThreads + " requests. All successful");
			else
			{
				System.out.println("Server down with " + numThreads + " requests");
				System.out.println("Longest time to response: " + (longestTimeForResponse - barrierReleased) + " milliseconds");
				break;
			}
			numThreads += 20;
			cntSuccessfulRequests.set(numThreads);
			
			this.barrier.reset();
		}
		
			
		System.out.println("Client stopped");
		System.out.println("Successful requests: " + cntSuccessfulRequests);
	}
	
	private void startThreads (int number)
	{
		Thread threads[] = new Thread[number];
		for (int i = 0; i < number; ++i)
		{
			threads[i] = new Thread(new Request(request, host, port));
			threads[i].start();
		}
		
		for (Thread thread : threads)
		{
			try 
			{
				thread.join();
			} 
			catch (InterruptedException e) 
			{
				System.out.println("join error");
				e.printStackTrace();
			}
		}
	}
	
	class Request extends Thread
	{
		private String request;
		private String host;
		private int port;
		
		public Request(String request, String host, int port)
		{
			this.request = request;
			this.host = host;
			this.port = port;
		}

		@Override
		public void run() {
			long released = 0;
			long successfulResponseGet;

			//Wait on the barrier
			try 
			{
				//	System.out.println(this.getName() + " waiting at barrier");
				barrier.await();
				released = System.currentTimeMillis();
				//	System.out.println(this.getName() + " over the barrier");
			} 
			catch (InterruptedException | BrokenBarrierException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			
			try (Socket socket = new Socket(host, port);
				 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				 PrintWriter out = new PrintWriter(socket.getOutputStream());)
			{
			
				out.print(request);
				out.flush();
				
//				System.out.println(this.getName() + " : sent");
				
				// Read the response from the server
				String response = null;
//				String everything = null;
				try 
				{
					response = in.readLine();
					
				//	System.out.println(response);
//					while ((everything = in.readLine()) != null)
//						System.out.println(this.getName() + "           " + everything);
				} 
				catch (IOException e) 
				{
					System.out.println("reading line error");
					e.printStackTrace();
				}
				//System.out.println(this.getName() + " : " + response);
				
				if (response.equals("HTTP/1.1 200 OK"))
				{
					successfulResponseGet = System.currentTimeMillis();
				//	System.out.println(this.getName() + " successful");
					cntSuccessfulRequests.incrementAndGet();
					
					barrierReleased = released;
					
					synchronized (longestTimeForResponse) {
						if (successfulResponseGet > longestTimeForResponse)
							longestTimeForResponse = successfulResponseGet;
					}
				}
				else
					return;
				//	System.out.println(this.getName() + " UNSUCCESSFUL");
			} 
			catch (IOException e1) 
			{
				return;
//				System.out.println("request problem");
//				e1.printStackTrace();
			}
			
			
		}
	}
}
