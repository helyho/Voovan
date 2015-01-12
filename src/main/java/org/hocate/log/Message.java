package org.hocate.log;

public class Message {
	private String	priority;
	private String	message;

	public Message() {

	}

	public Message(String priority, String message) {
		this.priority = priority;
		this.message = message;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String level) {
		priority = level;
	}

	public String getMessage() {
		String infoIndent = StaticParam.getConfig("InfoIndent");
		// 对缩进进行处理
		if (StaticParam.getConfig("InfoIndent") != null) {
			message = infoIndent + message;
			message = message.replaceAll("\r\n", "\r\n" + infoIndent);
			return message.replaceAll("\n", "\n" + infoIndent);
		} else {
			return message;
		}
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public static Message newInstance(String priority, String message) {
		return new Message(priority, message);
	}
}
