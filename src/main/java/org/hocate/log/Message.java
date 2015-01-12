package org.hocate.log;

public class Message {
	private String name;
	private String priority;
	private String message;
	
	public Message(){
		
	}
	
	public Message(String name,String priority,String message){
		this.name = name;
		this.priority = priority;
		this.message = message;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
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
	
	public static Message newInstance(String name,String priority,String message){
		return new Message(name,priority,message);
	}
}
