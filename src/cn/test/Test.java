package cn.test;

import java.nio.ByteBuffer;
import java.util.Queue;

public class Test {

	public static void main(String[] args) {
		ByteBuffer bb=ByteBuffer.allocate(1024);
		bb.put("哈哈哈".getBytes());
		System.out.println(bb.hasArray());
		System.out.println(new String(bb.array()));
		bb.compact();
		System.out.println(bb.hasArray());
		System.out.println(new String(bb.array()));
	}
}
