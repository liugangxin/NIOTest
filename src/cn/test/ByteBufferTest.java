package cn.test;

import java.nio.ByteBuffer;
import java.util.Queue;

//测试ByteBuffer
public class ByteBufferTest {

	public static void main(String[] args) {
		ByteBuffer bb = ByteBuffer.allocate(1024);
		// bb.putInt(100);//开头先放入一个int
		bb.put("6666哈哈哈".getBytes());
		bb.flip();// 将postion复位到0，并将limit设为当前postion，用于之后读操作
		System.out.println("*******************1读取之前************************");
		System.out.println("remaining():" + bb.remaining());
		System.out.println("position():" + bb.position());
		System.out.println("limit():" + bb.limit());
		System.out.println("capacity():" + bb.capacity());
		System.out.println("hasRemaining():" + bb.hasRemaining());
		System.out.println("array()也能获取所有内容:" + new String(bb.array()));
		System.out.println("*******************2读取后(使用波byte数组而不是直接用array()输出)************************");
		byte[] ret = new byte[bb.remaining()];
		bb.get(ret);
		System.out.println("byte数组：" + new String(ret));
		System.out.println("remaining():" + bb.remaining());
		System.out.println("position():" + bb.position());
		System.out.println("limit():" + bb.limit());
		System.out.println("capacity():" + bb.capacity());
		System.out.println("hasRemaining():" + bb.hasRemaining());
		System.out.println("array()也能获取所有内容:" + new String(bb.array()));
		System.out.println("*******************3删除已读数据************************");
		bb.compact();// 删除已读数据
		System.out.println("remaining():" + bb.remaining());
		System.out.println("position():" + bb.position());
		System.out.println("limit():" + bb.limit());
		System.out.println("capacity():" + bb.capacity());
		System.out.println("hasRemaining():" + bb.hasRemaining());
		System.out.println("array()也能获取所有内容:" + new String(bb.array()));
		System.out.println("*******************4重新放入数据************************");
		bb.put("房顶上".getBytes());
		bb.flip();
		System.out.println("remaining():" + bb.remaining());
		System.out.println("position():" + bb.position());
		System.out.println("limit():" + bb.limit());
		System.out.println("capacity():" + bb.capacity());
		System.out.println("hasRemaining():" + bb.hasRemaining());
		System.out.println("array()也能获取所有内容:" + new String(bb.array()));
		System.out.println("*******************5删除已读数据，并重置position和limit************************");
		bb.clear();// position置为0,并未清除内容
		bb.put("好的".getBytes());
		bb.flip();
		System.out.println("remaining():" + bb.remaining());
		System.out.println("position():" + bb.position());
		System.out.println("limit():" + bb.limit());
		System.out.println("capacity():" + bb.capacity());
		System.out.println("hasRemaining():" + bb.hasRemaining());
		System.out.println("array()也能获取所有内容:" + new String(bb.array()));
	}

	/*
	 * 总结：1、compact()和clear()方法清除后，其实内容并没有删掉，只是修改了limit和position位置。
	 */
}
