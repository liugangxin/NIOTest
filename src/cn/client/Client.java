package cn.client;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import cn.base.MessageFormatConfig;
import cn.base.SocketItem;

public class Client extends JFrame {
	public static void main(String[] args) {
		Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
		Client client = new Client("刘2");
		client.init();
	}

	private static final long serialVersionUID = -1888024005502800127L;
	private final MyClient myClient;

	public Client(String name) throws HeadlessException {
		this.myClient = new MyClient(name, this);
	}

	private JButton connectButton;
	private JButton disConnectButton;
	private JButton sendMsgButton;
	private JTextArea inTextArea;// 接受消息内容
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
					JOptionPane.showMessageDialog(null, "已连接...", "提示",
							JOptionPane.CANCEL_OPTION);
					return;
				}
				try {
					myClient.connect();
				} catch (IOException e1) {
					e1.printStackTrace();
					inTextArea.append("连接失败。。。" + "\n");
				}
				if (myClient.isConnecting()) {
					readMsgFromServer();
				}
			}

		});
		disConnectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO

			}
		});
		sendMsgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String text = outTextArea.getText();
				if (text.length() == 0) {
					JOptionPane.showMessageDialog(null, "消息为空", "错误",
							JOptionPane.ERROR_MESSAGE);
				}
				if (text.length() > 1000) {
					JOptionPane.showMessageDialog(null, "消息太长", "错误",
							JOptionPane.ERROR_MESSAGE);
				}
				myClient.sendMsg(text);
			}
		});
	}

	private void readMsgFromServer() {
		new Thread(this.myClient).start();
	}

	static class MyClient implements Runnable {
		private final String name;
		private final Client client;
		SocketItem socketItem;

		private MyClient(String name, Client client) {
			this.name = name;
			this.client = client;
		}

		public void sendMsg(String msg) {
			socketItem.wr.println(msg);
			socketItem.wr.flush();
		}

		public String readLineMsg() throws IOException {
			return socketItem.reader.readLine();
		}

		public boolean isConnecting() {
			if (this.socketItem != null && this.socketItem.socket.isConnected()) {
				return true;
			}
			return false;
		}

		private SocketItem connect() throws UnknownHostException, IOException {
			Socket socket = null;
			PrintWriter wr = null;
			BufferedReader reader = null;
			socket = new Socket("10.18.4.44", 9998);
			reader = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			wr = new PrintWriter(socket.getOutputStream());
			// Thread.sleep(100);
			wr.println(String.format(MessageFormatConfig.LoginFormat, name));
			wr.flush();
			this.socketItem = new SocketItem(name, socket, reader, wr);
			return socketItem;
		}

		public void testSendMsg() {
			new Thread() {
				@Override
				public void run() {
					try {
						int i = 1;
						while (true) {
							Thread.sleep(2000);
							sendMsg(String.format("发送第[%s]次消息", i));// 另外，一般发送消息，会收到response，超时没收到就尝试重连即可
							i++;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println(String.format("Client:发送消息线程[%s]结束",
							name));
				}
			}.start();
		}

		@Override
		public void run() {
			if (socketItem != null) {
				try {
					while (true) {
						String content = readLineMsg();// 无消息时阻塞
						if (content != null) {
							client.inTextArea.append(content + "\n");
						} else {
							Thread.sleep(10);
						}
					}
				} catch (Exception e) {
				}
				client.inTextArea.append(String
						.format("Client:线程[%s]结束了", name) + "\n");
			}
		}

	}

}
