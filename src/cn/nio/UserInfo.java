package cn.nio;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

public class UserInfo {

	public final SocketChannel socketChannel;
	private LinkedBlockingQueue<ByteBuffer> sendQueue = new LinkedBlockingQueue<ByteBuffer>(
			10000);
	private LinkedBlockingQueue<String> receiveMsgQueue = new LinkedBlockingQueue<String>(
			10000);

	public UserInfo(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}

	public void addSendMsg(ByteBuffer msg) {
		this.sendQueue.add(msg);
	}

	public ByteBuffer getSendMsg() {
		return this.sendQueue.poll();
	}

	public void addReceiveMsg(String msg) {
		this.receiveMsgQueue.add(msg);
	}

	public String getReceiveMsg() {
		return this.receiveMsgQueue.poll();
	}
}
