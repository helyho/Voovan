package org.voovan.tools.log;

/**
 * 日志消息对象
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
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
