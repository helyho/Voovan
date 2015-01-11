/**
 * JavaScript 进行 socket 通信的公共类
 */
importClass(org.hocate.network.aio.AioServerSocket)
importClass(org.hocate.network.aio.AioSocket)
importClass(org.hocate.network.StringFilter)
importClass(org.hocate.network.ScriptHandler)
/**
 * TcpSever 
 * addr 监听地址
 * port 监听端口
 * timeout 监听超时事件
 */
function SocketServer(add,port,timeout){
	var serverSocket = new AioServerSocket(add,port,timeout);
	serverSocket.filterChain().add(new StringFilter());
	serverSocket.handler(new ScriptHandler(warp(this)));
	
	this.onConnect = function(session){
		return null;
	}
	
	this.onDisconnect = function(session){
		
	}
	
	this.onRecive = function(session,object){
		return object;
	}
	
	this.onSent = function(session,object){
		
	}
	
	this.onException = function(session,exception){
		
	}
	
	this.start = function(){
		serverSocket.start();
	}
}

/**
 * TcpClient 
 * addr 监听地址
 * port 监听端口
 * timeout 监听超时事件
 */
function SocketClient(add,port,timeout){
	var socket = new AioSocket(add,port,timeout);
	socket.filterChain().add(new StringFilter());
	socket.handler(new ScriptHandler(warp(this)));
	
	this.onConnect = function(session){
		return null;
	}
	
	this.onDisconnect = function(session){
		
	}
	
	this.onRecive = function(session,object){
		return object;
	}
	
	this.onSent = function(session,object){
		
	}
	
	this.onException = function(session,exception){
		
	}
	
	this.start = function(){
		socket.start();
	}
}