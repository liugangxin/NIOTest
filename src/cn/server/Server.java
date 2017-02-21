package cn.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import cn.base.MessageFormatConfig;
import cn.base.SocketItem;

public class Server extends Thread {

	ServerSocket serverSocket = null;
	private volatile boolean running = true;
	private final ConcurrentHashMap<String, MySocketHandle> socketMap = new ConcurrentHashMap<String, MySocketHandle>();

	public static void main(String[] args) throws InterruptedException {
		Server server = new Server();
		server.startup();
		server.checkSocketAlice();
		Thread.sleep(2000);
		// server.shutdown();
	}

	public void startup() {
		this.start();
	}

	private void shutdown() {
		this.running = false;
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (MySocketHandle mySocket : socketMap.values()) {
			offline(mySocket);
		}
		socketMap.clear();
	}

	private void offline(MySocketHandle mySocket) {
		socketMap.remove(mySocket.socketItem.name);
		closeSocket(mySocket.socketItem);
	}

	private void closeSocket(SocketItem socketItem) {
		try {
			if (socketItem.reader != null) {
				socketItem.reader.close();
			}
			if (socketItem.wr != null) {
				socketItem.wr.close();
			}
			if (socketItem.socket != null) {
				socketItem.socket.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println(String.format("Server:Socket[]关闭流错误", socketItem.name));
		}
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(9998);
			System.out.println("服务器启动...");
			while (running) {
				Socket socket = serverSocket.accept();
				createNewSocketConnect(socket);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Server:服务器关闭...");
		}
	}

	private void createNewSocketConnect(Socket socket) {
		SocketItem socketItem = null;
		BufferedReader reader;
		PrintWriter wr;
		try {
			// socket.setKeepAlive(true);
			socket.setSoTimeout(1 * 1000);// 设置超时时间，超过该时间没有收到消息，就会抛出异常
			wr = new PrintWriter(socket.getOutputStream());
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String content = reader.readLine();
			if (content != null && MessageFormatConfig.startWithFormat(content, MessageFormatConfig.LoginFormat)) {
				socketItem = new SocketItem(MessageFormatConfig.getMsgInfo(content, MessageFormatConfig.LoginFormat),
						socket, reader, wr);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		if (socketItem != null) {
			MySocketHandle mySocket = new MySocketHandle(socketItem);
			socketMap.put(mySocket.socketItem.name, mySocket);
			mySocket.start();
			mySocket.sendMsg("连接成功~");
		} else {
			closeSocket(new SocketItem("", socket, reader, wr));
		}
	}

	public void checkSocketAlice() {
		new Thread() {
			@Override
			public void run() {
				while (running) {
					for (MySocketHandle mySocket : socketMap.values()) {
						SocketItem socketItem = mySocket.socketItem;
						boolean isAlice = false;
						if (mySocket.isAlive() && socketItem.socket.isConnected()) {
							try {
								socketItem.socket.sendUrgentData(0xFF);// 检测是否断开连接
							} catch (IOException e) {
								offline(mySocket);
							}
							isAlice = true;
						}
						if (!isAlice) {
							offline(mySocket);
						}
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		}.start();
	}

	class MySocketHandle extends Thread {
		final SocketItem socketItem;
		private boolean isRunning = true;

		public MySocketHandle(SocketItem socketItem) {
			this.socketItem = socketItem;
		}

		@Override
		public void run() {
			try {
				while (this.isRunning) {
					String content = socketItem.reader.readLine();// 无消息时阻塞
					if (content != null) {
						System.out.println("handle:" + content);
						// handle TODO
					} else {
						sleep(10);// 否则一直处于读取状态
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				this.isRunning = false;
				System.out.println(String.format("Server:Socket[%s]中断退出了", socketItem.name));
				return;
			}
		}

		public void sendMsg(String string) {
			socketItem.wr.println("连接成功~");
			socketItem.wr.flush();
		}

	}
}
