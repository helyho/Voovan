Voovan ( Apache v2 License )
===============
####Voovan开源项目旨在提供可靠、方便、可单元测试的代码。它是一个<font color="blue">无任何依赖</font>的独立工具包，希望能够方便广大开发者快速的实现应用。

###一、异步通信框架(AIO、NIO异步通信)

  类似 Netty 和 MINA 的异步 Socket 通信框架.但有有所不同。
  1. <font color="blue">可灵活实现Socket通信粘包的支持</font>（代码中包含 HTTP协议,字符串换行,定长报文的粘包实现）。
  1. <font color="blue">支持 SSL/TLS 加密通信。</font>
  1. <font color="blue">提供线程池依据系统负载情况自动动态调整。</font>
  1. 同时支持 NIO 和 AIO 特性。
  1. 采用非阻塞方式的异步传输。
  1. 事件驱动(Connect、Recive、Sent、Close、Exception)，采用回调的方式完成调用。
  1. <font color="blue">可灵活的加载过滤器机制。</font>

---------------------

###二、HTTP 客户端和服务端 通信实现
###客户端特性:
  1. 基于 Voovan 异步通信框架实现。
  1. 客户端可以灵活自定义请求报文。
  1. 同步实现 HTTP请求(需要同步实现可参考)。

###服务端特性:
  1. 支持 <font color="blue">WebSocket Version 13</font>，并且保持 WebSocket请求参数。
  1. <font color="blue">使用 Lambda 实现更加方便的 Http 响应实现。</font>
  1. <font color="blue">支持路径变量自动抽取</font>  当路径定义/:name,在使用/jonh地址访问时，可以通过 name 参数获取 jonh 字符串。
  1. 重定向支持。
  1. 可灵活实现session共享。
  1. <font color="blue">异常统一展示</font>支持。
  1. MIME 配置支持。

---------------------

###三、动态编译支持

  在内存中编一个保存有 java 代码的字符串,并将编译后的 byte 字节加入到 classloader 中,可灵活的动态定义类和使用。

---------------------

###四、数据库操作帮助类
提供类似Spring JDBCTemplate 的数据访问支持，但提供了更好更灵活易用的函数设计，同时提供对数据库事务的支持。

---------------------

###五、DateTime、String、Log、反射、对象工具、流操作、文件操作、异步双向通道等

  1. 日期类型和字符串和日期类型互转,日期类型加减操作。
  1. 反射的快捷实现,可直接通过反射取属性值,调用方法,实例化对象,<font color="blue">判断类的继承及实现</font>。
  1. 简单的日志记录类,支持指定输出流,日志报文自定义等。
  1. 简单的<font color="blue">自推导的对象类型强制转换</font>,根绝参数类型,运算类型自动退定强制转换类型。
  1. 流操作,读取定长、按行读取、<font color="blue">基于byte的split操作</font>等。
  1. 使用 ByteBuffer 实现的一个双向通道。
  1. 属性文件操作。
  1. 文件路径拼接、从绝对路径读取、从相对路径读取、从包路径读取、指定起始和结束位置内容读取等。
  1. List 和 Map 的快速初始化。



##包结构说明

|  包名                      | 名称           |
| -------------             |:-------------: |
|org.voovan.db              |数据库操作类      |
|org.voovan.http            |HTTP工具包       |
|org.voovan.dynamicComplier |动态编译包        |
|org.voovan.network         |网络异步通信包    |
|org.voovan.tools           |基本工具包        |
