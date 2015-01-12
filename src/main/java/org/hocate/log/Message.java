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
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public static Message newInstance(String priority, String message) {
		return new Message(priority, message);
	}
}
