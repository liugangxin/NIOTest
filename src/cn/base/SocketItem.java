package cn.base;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketItem {
	public final String name;
	public final Socket socket;
	public final BufferedReader reader;
	public final PrintWriter wr;
	public SocketItem(String name, Socket socket, BufferedReader reader, PrintWriter wr) {
		this.name = name;
		this.socket = socket;
		this.reader = reader;
		this.wr = wr;
	}
}
