package org.hocate.log;

import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class WriteThread implements Runnable {
	private LinkedBlockingDeque<String>	logDeque;
	private OutputStream[]				outputStreams;

	public WriteThread(OutputStream[] outputStreams) {
		this.logDeque = new LinkedBlockingDeque<String>();
		this.outputStreams = outputStreams;
	}

	public void addLogMessage(String string) {
		logDeque.add(string);
	}

	@Override
	public void run() {
		while (true) {
			try {
				String formatedMessage = logDeque.poll(1,TimeUnit.SECONDS);
				if (formatedMessage != null) {
					for (OutputStream outputStream : outputStreams) {
						if (outputStream != null) {
							outputStream.write(formatedMessage.getBytes());
							outputStream.flush();
						}
					}
				} else {
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
