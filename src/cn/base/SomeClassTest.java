package cn.base;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;

public class SomeClassTest {

	public static void main(String[] args) throws Exception {
		InetAddress address =InetAddress.getByName("10.18.4.162");
		URL baidu =new URL("http://www.baidu.com"); 
		URL url =new URL(baidu,"/index.html?username=tom#test");//？表示参数，#表示锚点 
		String protocol = url.getProtocol();//获取协议 
		String host = url.getHost();//获取主机 
		int port = url.getPort();//如果没有指定端口号，根据协议不同使用默认端口。此时getPort()方法的返回值为 -1 
		String path = url.getPath();//获取文件路径 
		String file = url.getFile();//文件名，包括文件路径+参数 
		String ref = url.getRef();//相对路径，就是锚点，即#号后面的内容 
		String query = url.getQuery();//查询字符串，即参数 
		
		System.out.println(protocol);
		System.out.println(host);
		System.out.println(port);
		System.out.println(path);
		System.out.println(file);
		System.out.println(ref);
		System.out.println(query);
		
		InetAddress address2 =InetAddress.getByName(host);
		System.out.println(address2.getHostAddress());
		
	}
}
