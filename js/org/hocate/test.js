
/**
 * 测试对象
 */
function testObj() {
	this.msg="property message";
	this.printMsg = function(){
		print("//-----------------------");
		print("this is test script.");
		print("//-----------------------");
	}
	
	this.DatabaseTest = function(){
		var db = new Database();
		var items = db.query("SELECT * FROM sc_script where version>=:1 and PackagePath like :2",1,'org.%')
		print(typeof(items))
		for(var item in items){
			print(items[item])
		}
	}
	
	/**
	 * Tcp 服务器测试
	 */
	this.TcpServerSocketTest = function(){
		var ss = new SocketServer("127.0.0.1",1031,60000);
		 ss.onRecive = function(session,object){
			print("Server Recive:"+object)
			var msg = object+" server return ";
			print("Server Send: "+msg);
			return msg;
		}
		
		ss.onSent = function(session,obj){
			session.close();
		}
		
		ss.start();
	}
	
	/**
	 * Tcp 连接测试
	 */
	this.TcpClient = function(){
		var ss = new SocketClient("127.0.0.1",1031,60000);
		
		ss.onConnect = function(session){
			return "test msg\r\n";
		}
		
		ss.onRecive = function(session,obj){
			print("Client Recive:"+obj);
			session.close();
		}
		ss.start();
	}
}

/*
 * 测试执行代码
 */  
print("=============================================")
	var t = new testObj();
	try{
		t.TcpServerSocketTest()
		t.TcpClient()
	}
	catch(e){
		print(e)
	}
	//db = t.DatabaseTest()