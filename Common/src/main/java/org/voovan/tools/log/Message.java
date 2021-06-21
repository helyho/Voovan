package org.voovan.tools.log;

import org.voovan.tools.TEnv;
import org.voovan.tools.TFile;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;

import java.util.Map;
import java.util.function.Function;

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
	private Object	message;
	private Object[] args;
	private Throwable throwable;

	private Map<String, String> tokens;
	private StackTraceElement stackTraceElement;

	public Message() {
		if(LoggerStatic.HAS_STACK) {
			stackTraceElement =  currentStackLine();
		}
	}

	public Message(String level, Object message, Object[] args, Throwable throwable ) {
		this.level = level;
		this.message = message;
		this.args = args;
		this.throwable = throwable;

		if(LoggerStatic.HAS_STACK) {
			stackTraceElement =  currentStackLine();
		}
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Object getMessage() {
		return message;
	}

	public void setMessage(Object message) {
		this.message = message;
	}

	public Map<String, String> getTokens() {
		return tokens;
	}

	public void setTokens(Map<String, String> tokens) {
		this.tokens = tokens;
	}

	public StackTraceElement getStackTraceElement() {
		return stackTraceElement;
	}

	public void setStackTraceElement(StackTraceElement stackTraceElement) {
		this.stackTraceElement = stackTraceElement;
	}

	public String format() {
		message = TObject.nullDefault(message, "");

		if(!(message instanceof String)) {
			Function<Object, String> jsonFormat = LoggerStatic.JSON_FORMAT ? JSON::toJSONWithFormat : JSON::toJSON;
			message = jsonFormat.apply(message);
		}

		if(throwable!=null) {
			//构造栈信息输出
			String stackMessage = "";
			do {
				stackMessage = stackMessage + throwable.getClass().getCanonicalName() + ": " +
						throwable.getMessage() + TFile.getLineSeparator() +
						TString.indent(TEnv.getStackElementsMessage(throwable.getStackTrace()), 4) +
						TFile.getLineSeparator();
				throwable = throwable.getCause();

			} while (throwable != null);

				message = (message.toString().isEmpty() ? "" : (message + " => ")) + stackMessage;
		}

		if(args!=null && args.length>0) {

			for(int i=0;i<args.length;i++){
				if( !(args[i] instanceof String) )
				args[i] = JSON.toJSON(args[i]);
			}

			message = TString.tokenReplace((String)message, args);
		}

		return (String)message;
	}

	/**
	 * 获得当前栈元素信息
	 * @return 栈信息元素
	 */
	public static StackTraceElement currentStackLine() {
		StackTraceElement[] stackTraceElements = TEnv.getStackElements();
		if(stackTraceElements.length <= 8) {
			return stackTraceElements[stackTraceElements.length - 1];
		} else {
			return stackTraceElements[8];
		}
	}

	public static Message newInstance(String level, Object message, Object[] args, Throwable throwable) {
		return new Message(level, message, args, throwable);
	}
}
