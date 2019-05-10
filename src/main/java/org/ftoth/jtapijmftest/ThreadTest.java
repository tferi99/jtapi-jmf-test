package org.ftoth.jtapijmftest;

public class ThreadTest
{
	public static void main(String[] args)
	{
		Object lock = new Object();
				
		Thread2 t2 = new Thread2(lock);
		t2.start();

		synchronized (lock) {
			try {
				lock.wait();
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("END");
	}
}


class Thread2 extends Thread
{
	private Object lock;
	
	public Thread2(Object lock)
	{
		this.lock = lock;
	}
	
	@Override
	public void run()
	{
		try {
			System.out.println("Thead2 running...");
			Thread.sleep(1000);
			
			System.out.println("Thead2 notifies main thread");
			synchronized (lock) {
				lock.notifyAll();
			}
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}