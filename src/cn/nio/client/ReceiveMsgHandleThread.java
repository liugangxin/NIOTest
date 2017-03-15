package cn.nio.client;

import java.util.concurrent.LinkedBlockingQueue;

public class ReceiveMsgHandleThread extends Thread {

	private volatile boolean running = true;
	private final NIOClient clientItem;
	private LinkedBlockingQueue<String> receiveMsgQueue = new LinkedBlockingQueue<String>(10000);

	public ReceiveMsgHandleThread(NIOClient clientItem) {
		this.clientItem = clientItem;
	}

	@Override
	public void run() {
		while (running) {
			try {
				String msg = receiveMsgQueue.take();
				if (msg != null) {
					clientItem.inTextArea.append(msg + "\n");
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void addReceiveMsg(String msg) {
		if (msg != null) {
			this.receiveMsgQueue.add(msg);
		}
	}

	public String getReceiveMsg() {
		return this.receiveMsgQueue.poll();
	}

	public void shutdown() {
		this.running = false;
	}
}
