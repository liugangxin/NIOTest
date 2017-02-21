package cn.base;

public class MessageFormatConfig {

	public static final String LoginFormat = "Login@%s";
	public static final String MsgFormat = "Msg@%s";
	
	public static String getMsgInfo(String msg, String format){
		return msg.replaceFirst(getMsgKey(format), "");
	}
	
	public static boolean startWithFormat(String msg, String format){
		return msg.startsWith(getMsgKey(format));
	}
	
	private static String getMsgKey(String format){
		return format.replaceAll("%s", "");
	}
}
