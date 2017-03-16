package cn.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.nio.ReadHandleMsgThread;
import cn.nio.UserInfo;
import cn.nio.WriteMsgThread;

//http://blog.csdn.net/chenxuegui1234/article/details/17979725
public class NIOServer extends Thread {

	private volatile boolean running = true;
	private ServerSocketChannel serverSocketChannel;
	private Selector selector;
	private WriteMsgThread writeThread;
	private ReadHandleMsgThread readThread;
	// 接受和发送数据缓冲区
	private final ConcurrentHashMap<SocketChannel, UserInfo> socketMap = new ConcurrentHashMap<SocketChannel, UserInfo>();

	public NIOServer() {
		try {
			serverSocketChannel = ServerSocketChannel.open();// 打开服务器套接字通道
			serverSocketChannel.configureBlocking(false);// 服务器配置为非阻塞
			ServerSocket serverSocket = serverSocketChannel.socket();// 检索与此通道关联的服务器套接字
			serverSocket.bind(new InetSocketAddress(9998));
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
			System.out.println("服务器启动...");
			writeThread = new WriteMsgThread();
			readThread = new ReadHandleMsgThread();
			writeThread.start();
			readThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void listen() throws IOException {
		while (true) {
			selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			for (SelectionKey selKey : selectedKeys) {
				try {
					handleKey(selKey);
				} catch (Exception e) {
					if (!selKey.isAcceptable()) {
						SocketChannel socketChannel = (SocketChannel) selKey.channel();
						if (socketChannel != null) {
							UserInfo userInfo = socketMap.remove(socketChannel);
							closeSocket(userInfo);
						}
					}
				}
			}
			selectedKeys.clear();
		}
	}

	private void handleKey(SelectionKey selKey) throws IOException {
		// 测试此键的通道是否已准备好接受新的套接字连接。
		if (selKey.isAcceptable()) {
			// 返回为之创建此键的通道。
			ServerSocketChannel server = (ServerSocketChannel) selKey.channel();
			// 此方法返回的套接字通道（如果有）将处于阻塞模式。
			SocketChannel client = server.accept();
			// 配置为非阻塞
			client.configureBlocking(false);
			// 注册到selector，等待读和写
			client.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			socketMap.put(client, new UserInfo(client));
		} else if (selKey.isReadable()) {
			// 返回为之创建此键的通道。
			System.out.println("调用读...");
			SocketChannel client = (SocketChannel) selKey.channel();
			UserInfo userInfo = socketMap.get(client);
			ByteBuffer receive = ByteBuffer.allocate(1024);
			if (client.read(receive) == -1) {
				// 此处断开连接即可
				closeSocket(userInfo);
				return;
			}
			String msg = new String(receive.array()).trim();
			userInfo.addReceiveMsg(msg);
			int hadReceiveMsgNum = readThread.addReadEvent(userInfo);
			userInfo.addSendMsg(ByteBuffer.wrap(String.format("收到消息[%s]:%s", hadReceiveMsgNum, msg).getBytes()));
			writeThread.addWriteEvent(userInfo);
			selKey.interestOps(SelectionKey.OP_READ);
		} else if (selKey.isWritable()) {
			// 返回为之创建此键的通道。
			System.out.println("调用写咯...");
			selKey.interestOps(SelectionKey.OP_READ);// 好像用不着...
		}
	}

	private void shutdown() {
		this.running = false;
		if (serverSocketChannel != null) {
			try {
				serverSocketChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (UserInfo userInfo : socketMap.values()) {
			closeSocket(userInfo);
		}
		socketMap.clear();
	}

	private void offline(UserInfo userInfo) {
		socketMap.remove(userInfo.socketChannel);
		closeSocket(userInfo);
	}

	private void closeSocket(SocketChannel socketChannel) {
		UserInfo userInfo = socketMap.remove(socketChannel);
		if (userInfo != null) {
			closeSocket(userInfo);
		}
	}

	private void closeSocket(UserInfo userInfo) {
		try {
			if (userInfo.socketChannel != null) {
				userInfo.socketChannel.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			System.out.println("Server:Socket[]关闭流错误");
		}
	}

	// 开启检查连接线程，出现异常java.net.SocketException: Connection reset
	public void checkSocketAlice() {
		new Thread() {
			@Override
			public void run() {
				while (running) {
					for (UserInfo userInfo : socketMap.values()) {
						boolean isAlice = false;
						if (userInfo.socketChannel.isConnected()) {
							try {
								// socketItem.socket.sendUrgentData(0xFF);//
								// 检测是否断开连接,定时每5秒调用该方法出现异常java.net.SocketException:
								// Connection reset
							} catch (Exception e) {
								offline(userInfo);
							}
							isAlice = true;
						}
						if (!isAlice) {
							offline(userInfo);
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

	public static void main(String[] args) throws InterruptedException {
		NIOServer server = new NIOServer();
		try {
			server.listen();
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.checkSocketAlice();
		Thread.sleep(2000);
		// server.shutdown();
	}
}
