package cn.nio;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadHandleMsgThread extends Thread {

	private volatile boolean running = true;
	private AtomicInteger receiveNum = new AtomicInteger();
	private LinkedBlockingQueue<UserInfo> needReadUserQueue = new LinkedBlockingQueue<UserInfo>();

	@Override
	public void run() {
		while (running) {
			try {
				UserInfo userInfo = needReadUserQueue.take();
				if (userInfo != null) {
					doHandle(userInfo);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void doHandle(UserInfo userInfo) {
		String msg = userInfo.getReceiveMsg();
		System.out.println("收到消息：" + msg);
	}

	public int addReadEvent(UserInfo userInfo) {
		this.needReadUserQueue.add(userInfo);
		return this.receiveNum.incrementAndGet();
	}

	public void shutdown() {
		this.running = false;
	}
}
