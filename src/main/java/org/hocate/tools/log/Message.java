package org.hocate.tools.log;

public class Message {
	private String	level;
	private String	message;

	public Message() {

	}

	public Message(String level, String message) {
		this.level = level;
		this.message = message;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public static Message newInstance(String priority, String message) {
		return new Message(priority, message);
	}
}
