package cn.server.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.base.MessageFormatConfig;
import cn.base.SocketItem;
//http://blog.csdn.net/chenxuegui1234/article/details/17979725
public class NIOServer extends Thread {

	ServerSocketChannel serverSocketChannel = null;
	Selector selector = null;
	// 接受和发送数据缓冲区  
    private ByteBuffer send = ByteBuffer.allocate(1024);  
    private ByteBuffer receive = ByteBuffer.allocate(1024); 
	private final ConcurrentHashMap<String, MySocketHandle> socketMap = new ConcurrentHashMap<String, MySocketHandle>();

	public NIOServer() {
		try {
			serverSocketChannel = ServerSocketChannel.open();// 打开服务器套接字通道
			serverSocketChannel.configureBlocking(false);// 服务器配置为非阻塞
			ServerSocket serverSocket = serverSocketChannel.socket();// 检索与此通道关联的服务器套接字
			serverSocket.bind(new InetSocketAddress(9998));
			selector = Selector.open();
			serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT); 
			System.out.println("服务器启动...");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void listen() throws IOException {
		while(true){
			selector.select();
			Set<SelectionKey> selectedKeys = selector.selectedKeys();
			for(SelectionKey key : selectedKeys){
				handleKey(key);
			}
			selectedKeys.clear();
		}
	}

	private void handleKey(SelectionKey selKey) throws IOException {
		String inText;
		String outText;
		// 测试此键的通道是否已准备好接受新的套接字连接。
		if(selKey.isAcceptable()){
			// 返回为之创建此键的通道。
			ServerSocketChannel server = (ServerSocketChannel) selKey.channel();
			// 此方法返回的套接字通道（如果有）将处于阻塞模式。  
			SocketChannel client = server.accept();
			// 配置为非阻塞  
            client.configureBlocking(false);
            // 注册到selector，等待读和写
            client.register(selector, SelectionKey.OP_READ  
                    | SelectionKey.OP_WRITE);
		}else if(selKey.isReadable()){
			// 返回为之创建此键的通道。
			SocketChannel client = (SocketChannel) selKey.channel();
			receive.clear();
			client.read(receive);
			System.out.println("接收到信息："+new String(receive.array()));
			selKey.interestOps(SelectionKey.OP_WRITE);
		}else if(selKey.isWritable()){
			
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
			System.out.println(String.format("Server:Socket[]关闭流错误",
					socketItem.name));
		}
	}

	@Override
	public void run() {
		try {
			
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
			socket.setKeepAlive(true);
			socket.setSoTimeout(30 * 60 * 1000);// 设置读超时时间，超过该时间没有收到消息，就会抛出异常
			wr = new PrintWriter(socket.getOutputStream());
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			String content = reader.readLine();
			if (content != null
					&& MessageFormatConfig.startWithFormat(content,
							MessageFormatConfig.LoginFormat)) {
				socketItem = new SocketItem(MessageFormatConfig.getMsgInfo(
						content, MessageFormatConfig.LoginFormat), socket,
						reader, wr);
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

	//开启检查连接线程，出现异常java.net.SocketException: Connection reset
	public void checkSocketAlice() {
		new Thread() {
			@Override
			public void run() {
				while (running) {
					for (MySocketHandle mySocket : socketMap.values()) {
						SocketItem socketItem = mySocket.socketItem;
						boolean isAlice = false;
						if (mySocket.isAlive()
								&& socketItem.socket.isConnected()) {
							try {
								//socketItem.socket.sendUrgentData(0xFF);// 检测是否断开连接,定时每5秒调用该方法出现异常java.net.SocketException: Connection reset
							} catch (Exception e) {
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
						// 返回处理结果...
						sendMsg("收到信息：" + content);
					} else {
						sleep(10);// 否则一直处于读取状态
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				this.isRunning = false;
				System.out.println(String.format("Server:Socket[%s]中断退出了",
						socketItem.name));
				return;
			}
		}

		public void sendMsg(String content) {
			socketItem.wr.println(content);
			socketItem.wr.flush();
		}

	}
	
	public static void main(String[] args) throws InterruptedException {
		NIOServer server = new NIOServer();
		server.startup();
		server.checkSocketAlice();
		Thread.sleep(2000);
		// server.shutdown();
	}
}
