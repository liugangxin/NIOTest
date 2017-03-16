package cn.nio.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class NIOClient extends JFrame {
	public static void main(String[] args) {
		Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
		NIOClient client = new NIOClient("刘");
		client.init();
	}

	private static final long serialVersionUID = -1888024005502800127L;
	private final MyClient myClient;

	public NIOClient(String name) throws HeadlessException {
		this.myClient = new MyClient(name, this);
	}

	private JButton connectButton;
	private JButton disConnectButton;
	private JButton sendMsgButton;
	protected JTextArea inTextArea;// 接受消息内容
	private JTextArea outTextArea;// 待发送消息内容

	private void init() {
		this.setTitle("客户端");
		this.setSize(800, 700);
		this.setLocation(100, 100);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		connectButton = new JButton("连接");
		disConnectButton = new JButton("断开连接");
		sendMsgButton = new JButton("发送");
		inTextArea = new JTextArea(40, 60);
		outTextArea = new JTextArea(10, 60);
		this.inTextArea.setEditable(false);
		JLabel inLabel = new JLabel("聊天内容:");
		inLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
		JLabel outLabel = new JLabel("编辑消息:");
		outLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
		// 初始化布局
		setLayout(new BorderLayout());
		JPanel panel1 = new JPanel();
		JPanel panel2 = new JPanel();
		JPanel panel3 = new JPanel();

		panel1.setLayout(new FlowLayout());
		panel2.setLayout(new BorderLayout());
		panel3.setLayout(new BorderLayout());
		this.add(panel1, BorderLayout.NORTH);
		this.add(panel2, BorderLayout.CENTER);
		this.add(panel3, BorderLayout.SOUTH);
		panel1.add(connectButton);
		panel1.add(disConnectButton);

		panel2.add(inLabel, BorderLayout.NORTH);
		panel2.add(new JScrollPane(inTextArea), BorderLayout.CENTER);

		JPanel panel4 = new JPanel();
		JPanel panel5 = new JPanel();
		panel4.setLayout(new BorderLayout());
		panel5.setLayout(new FlowLayout(FlowLayout.RIGHT));
		panel3.add(panel4, BorderLayout.CENTER);
		panel3.add(panel5, BorderLayout.SOUTH);

		panel4.add(outLabel, BorderLayout.NORTH);
		panel4.add(new JScrollPane(outTextArea), BorderLayout.CENTER);
		panel5.add(sendMsgButton);

		initListens();

		this.setVisible(true);
	}

	private void initListens() {
		connectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (myClient.isConnecting()) {
					JOptionPane.showMessageDialog(null, "已连接...", "提示", JOptionPane.CANCEL_OPTION);
					return;
				}
				try {
					myClient.connect();
				} catch (IOException e1) {
					e1.printStackTrace();
					inTextArea.append("连接失败。。。" + "\n");
				}
				clientStart();
				JOptionPane.showMessageDialog(null, "连接成功！！！", "提示", JOptionPane.CANCEL_OPTION);
			}

		});
		disConnectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				disConnect();
				inTextArea.append("断开连接。。。" + "\n");
			}
		});
		sendMsgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = outTextArea.getText();
				if (text.length() == 0) {
					JOptionPane.showMessageDialog(null, "消息为空", "错误", JOptionPane.ERROR_MESSAGE);
				}
				if (text.length() > 1000) {
					JOptionPane.showMessageDialog(null, "消息太长", "错误", JOptionPane.ERROR_MESSAGE);
				}
				myClient.sendMsg(text);
			}
		});
	}

	private void clientStart() {
		new Thread(this.myClient).start();
	}
	
	public void readNewMsg(String msg){
		this.inTextArea.append(msg + "\n");
	}
	
	public void disConnect(){
		this.myClient.shutDown();
	}
	
	public void errorServerClose(){
		JOptionPane.showMessageDialog(null, "服务器关闭！！！", "提示", JOptionPane.CANCEL_OPTION);
	}

	static class MyClient implements Runnable {
		private volatile boolean running = true;
		private final String name;
		private SocketChannel socketChannel;
		private Selector selector;
		private final ReceiveMsgHandleThread receiveMsgThread;

		private MyClient(String name, NIOClient client) {
			this.name = name;
			receiveMsgThread = new ReceiveMsgHandleThread(client);
			receiveMsgThread.start();
		}

		public void sendMsg(String msg) {
			try {
				ByteBuffer msgWrap = ByteBuffer.wrap(msg.getBytes());
				this.socketChannel.write(msgWrap);
			} catch (IOException e) {
				System.out.println("Client发送消息失败：");
				e.printStackTrace();
			}
		}

		public boolean isConnecting() {
			if (!running) {
				return false;
			}
			if (this.socketChannel != null && this.socketChannel.isConnected()) {
				return true;
			}
			return false;
		}

		private boolean connect() throws UnknownHostException, IOException {
			socketChannel = SocketChannel.open(new InetSocketAddress("10.18.4.44", 9998));
			selector = Selector.open();
			socketChannel.configureBlocking(false);
			socketChannel.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			return true;
		}

		public void listen() throws IOException {
			while (running) {
				selector.select();
				Set<SelectionKey> selectedKeys = selector.selectedKeys();
				for (SelectionKey key : selectedKeys) {
					handleKey(key);
				}
				selectedKeys.clear();
			}
		}

		private void handleKey(SelectionKey selKey) throws IOException {
			if (selKey.isReadable()) {
				// 返回为之创建此键的通道。
				System.out.println("调用读...");
				SocketChannel client = (SocketChannel) selKey.channel();
				String receiveMsg = readMsgByNewByteBuff(client);
				if (receiveMsg == null) {
					// 读不到数据，此处断开连接即可
					return;
				}
				// 两种方案处理，1放到队列一个一个处理服务端消息，有效率且便于操作消息处理顺序等;2接受一个处理一个
				System.out.println(receiveMsg);
				receiveMsgThread.addReceiveMsg(receiveMsg);
				selKey.interestOps(SelectionKey.OP_READ);
			} else if (selKey.isWritable()) {
				// 返回为之创建此键的通道。
				System.out.println("调用写咯...");
				selKey.interestOps(SelectionKey.OP_READ);
			}
		}

		private String readMsgByNewByteBuff(SocketChannel client) throws IOException {
			ByteBuffer receive = ByteBuffer.allocate(1024);
			if (client.read(receive) == -1) {
				// 此处断开连接即可
				return null;
			}
			return new String(receive.array()).trim();
		}

		private String readMsg(SocketChannel client) throws IOException {
			ByteBuffer receive = ByteBuffer.allocate(1024);// 假如要重用该对象
			if (client.read(receive) == -1) {
				// 此处断开连接即可
				return null;
			}
			receive.flip();// 蛋疼，写后一定需要反转，转为适合读的状态
			int len = receive.getInt();// 这里用remaining()不可行，获取不了需要对方前几位发消息长度
			byte[] msg = new byte[len];
			receive.get(msg);
			receive.compact();// 删除已读数据
			return new String(msg);
		}

		public void shutDown() {
			this.running = false;
			this.receiveMsgThread.shutdown();
			try {
				this.socketChannel.close();
				this.selector.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				listen();
			} catch (IOException e) {
				e.printStackTrace();
				receiveMsgThread.clientItem.errorServerClose();
			}
		}

	}

}
