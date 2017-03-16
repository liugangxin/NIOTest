package cn.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

public class WriteMsgThread extends Thread {

	private volatile boolean running = true;
	private LinkedBlockingQueue<UserInfo> needWriteUserQueue = new LinkedBlockingQueue<UserInfo>();

	@Override
	public void run() {
		while (running) {
			try {
				UserInfo userInfo = needWriteUserQueue.take();
				if (userInfo != null) {
					doWrite(userInfo);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void doWrite(UserInfo userInfo) {
		ByteBuffer sendMsg = userInfo.getSendMsg();
		if (sendMsg != null) {
			try {
				userInfo.socketChannel.write(sendMsg);
				// sendMsg.compact();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void addWriteEvent(UserInfo userInfo) {
		this.needWriteUserQueue.add(userInfo);
	}

	public void shutdown() {
		this.running = false;
	}
}
