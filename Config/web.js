{
	// 服务 IP 地址,默认0.0.0.0
	"Host"   			: "0.0.0.0", 
	// 服务端口,默认8080
	"Port"   			: 28080,		
	// 连接超时时间(s),默认30秒
	"Timeout"			: 30,
	// 上下文路径,绝对路径 "/"起始,相对路径 非"/" 起始
	"ContextPath"		: "",// /home/helyho/HttpServerDemo/WebApp
	// 默认字符集,默认 UTF-8
	"CharacterSet"		: "GB2312",
	// Session 容器类,默认java.util.Hashtable
	"SessionContainer"  : "java.util.Hashtable",
	// Session 会话超时时间(m),默认30分钟
	"SessionTimeout"    : 30,
	// KeepAlive 超时时间(s),默认5秒 (该参数同样会被应用到 WebSocket 的连接保持上)
	"KeepAliveTimeout"  : 5,
	// 是否启用Gzip压缩
	"Gzip"              : "on",
	// 过滤器 先执行filter1, 后执行filter2
	
	"Filter":[
		          {
		        	  "Name"      : "filter1",
		        	  "ClassName" : "org.voovan.test.http.HttpBizFilterTest",
		        	  "Encoding"  : "UTF-8",
		        	  "Action"    : "pass"      },
		          {
		        	  "Name"      : "filter2",
		        	  "ClassName" : "org.voovan.test.http.HttpBizFilterTest",
		        	  "Encoding"  : "UTF-8",
		        	  "Action"    : "pass"      }
	          ]
	
}