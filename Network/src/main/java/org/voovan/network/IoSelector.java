package org.voovan.network;

import org.voovan.network.tcp.SelectionKeySet;
import org.voovan.tools.ByteBufferChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * 选择器抽象类
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public abstract class IoSelector<C, S> {

	protected Selector selector;
	protected SocketContext socketContext;
	protected ByteBufferChannel netByteBufferChannel;
	protected ByteBufferChannel appByteBufferChannel;
	protected ByteBuffer readTempBuffer;

	protected S session;
	protected SelectionKeySet selectionKeys = new SelectionKeySet(1024);

	public abstract void eventChose();
	public abstract int readFromChannel() throws IOException;
	public abstract int writeToChannel(ByteBuffer buffer) throws IOException;
	public abstract C getChannel(SelectionKey selectionKey) throws IOException;
	public abstract void release();
}
