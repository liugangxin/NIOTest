package cn.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

import cn.base.MessageFormatConfig;
import cn.base.SocketItem;

public class Client {

	public static void main(String[] args) {
		new MyClient("刘").start();
	}

	static class MyClient extends Thread {
		String name;
		SocketItem clientItem;

		private MyClient(String name) {
			this.name = name;
		}

		@Override
		public void run() {
			clientItem = connect(name);
			if (clientItem != null) {
				testSendMsg();
				try {
					while (true) {
						String content = clientItem.reader.readLine();// 无消息时阻塞
						if (content != null) {
							System.out.println(content);
						} else {
							sleep(10);
						}
					}
				} catch (Exception e) {
				}
				System.out.println(String.format("Client:线程[%s]结束了", name));
			}
		}
		
		public void sendMsg(String msg){
			clientItem.wr.println(msg);
			clientItem.wr.flush();
		}

		private SocketItem connect(String name) {
			Socket socket = null;
			PrintWriter wr = null;
			BufferedReader reader = null;
			try {
				socket = new Socket("10.18.4.44", 9998);
				reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				wr = new PrintWriter(socket.getOutputStream());
				// Thread.sleep(100);
				wr.println(String.format(MessageFormatConfig.LoginFormat, name));
				wr.flush();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
			return new SocketItem(name, socket, reader, wr);
		}
		
		public void testSendMsg(){
			new Thread() {
				@Override
				public void run() {
					try {
						int i = 1;
						while (true) {
							Thread.sleep(2000);
							sendMsg(String.format("发送第[%s]次消息", i));//另外，一般发送消息，会收到response，超时没收到就尝试重连即可
							i++;
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					System.out.println(String.format("Client:发送消息线程[%s]结束", name));
				}
			}.start();
		}
	}
}
