###1.0-beta-1:###
- 发布日期:2016-09-02
####增加的内容:####
HttpServer 增加模块功能
在 web.json增加 IndexFiles 参数定义首页索引文件的名称.
增加对HttpServer.socket方法 Stream 操作支持
在 web.json 中对 https 参数独立处理
增加直接将参数转换成自定义对象的方法以及取特定类型参数的方法
在 web.json 中增加路由处理器的配
在 web.json 中修改 Filter 节点名称为 Filters
TString 增加正则匹配函数
TFile增加了类搜索函数
在 HttpBizFilter 类的 onRequest 方法对请求重定向.
JSON 增加格式化输出 JSON 的函数
增加获取自定义对象模型的方法 TRefletc.getClazzJSONModel(Class clazz)
增加基本类型转换成其包装的对象类型方法 TString.toObject
HttpServer 增加忽略大小写匹配路由
添加@NotJSON注解,过滤掉一些不想解析进 JSON 字符串的字段

####BUG修复:####
修复 TZip 中 Zip 压缩的错误
修复 Http 消息截断器对于有保内容不解析的 bug
增加对HttpServer 请求路径中的错误兼容.目前兼容多个/////的错误
Mime 类型为空的处理.
TRefletc.getObjectFromMap传入空的 Map 参数的处理
HttpServer 路径匹配*匹配人多个任意字符的逻辑bug修复
HttpServer注册 handler 时的顺序问题
某些平台的默认编码不是 UTF-8导致代码不能正常运行
JSON补充缺少的函数元素的解析,解析成字符串

###优化内容:####
HttpClient 代码优化
并发性能调优
SSL证书管理优化
HttpServer加载优化
将HttpServer性能监控模块分离成独立项目
增加Base64算法,应为 jdk1.7不支持
由于增加JSON对换行的支持,性能监控代码做响应的调整
重构HttpServer代码,将配置文件和上下文类移动到 context 目录.
增加 HttpServer 的 main 方法,默认加载 web.json 中的配置信息
JSON转换中对\r \n \的处理
Socket 通信代码优化
类重命名WebSocketBizHandler->WebSocketRoute
类重命名HttpBizHandler->HttpRoute
类重命名HttpBizFilter -> HttpFilter
将 Http 请求的路径匹配规则修改的更加严格
WebContext类何 WebServerConfig 暴露的方法处理,减少了一些可能导致风险的暴露方法
优化 class 和 jar 加载方法
WebSocketDispatcher优化
优化 HttpServerFilter 中对HTTP 报文的判断方式的优化
Mime 为极少变动的静态信息,调整到应用jar的静态资源中.但同时保留了对自定义配置的支持,自定义配置会覆盖系统默认配置
优化了 Error 的错误信息提示

###1.0-alpha-2:###

- 发布日期:2016-06-30
 
####BUG修复:####

 - 修复关闭 JDBC 连接时出现的 null 错误
 - HttpServer 无法正确加载错误展示页面
 - Http 请求中的If-Modified-Since头出现乱码导致解析异常
 - 在对方发送完报文后立即关闭连接,会导致无法继续收取缓冲区中的数据
 - TStream.read 方法读取后入参流的游标位置不正确
 - Socket 同步通信时SSL握手失败

###优化内容:####

 -  TString 增加字符串翻转函数
 -  数据库操作,对 commit/rollback 方法提供不关闭连接和可选关闭连接两种方式
 -  JSON 工具对 null 的兼容
 -  解析 SQL 查询条件函数优化
 -  TFile 函数修改,文件游标位置类型由 int 改为 long
 -  为 Socket 异步通信工具提供同步方法
 
###1.0-alpha:###

- 发布日期:2016-05-10
 
####BUG修复:####

 - 使用监视过滤器的时候,统计的处理时间没有包含过滤器处理的时间,将监视过滤器(HttpMonitorFilter)从过滤器链的最后一个调整到第一个
 - Http服务的过滤器对象无法区分是请求还是响应的调用,将原来的 doFilter方法删除,并增加 onRequest 和 onResponse 方法,参数相同.
 - HttpServer 静态文件 ETag 在文件路径不改变时不会发生变化的不过,导致文件一但被加载就会一直返回304的问题
 - 修复 Connection reset by peer 错误后,导致的死循环线程无法结束的问题
 - 多个filter执行顺序的问题
 - getObjectFromMap方法复杂对象时对 Map 的 value 为 null 的 bug 处理
 - getObjectFromMap方法重构并优化,对基本类型(int,float,long)的支持,以及 Date 方法 bug 处理
 - Http 消息截断器 的 boundary 判断报文结束判断的问题
 - 当日志文件目录不存在时自动创建,且日志文件不存在时提示,并只输出其他日志
 - 解决 SSL 连接 handshake 线程同步的问题
 - 解决发送是缓冲区满,后续字节没有继续发送的 bug
 - Logger.java中simple方法存在递归死循环的风险
 - KeepAlive 定时器对象不销毁的问题
 - WEBSOCKET两次关闭事件的 bug 处理
 - JSON 解析的数据为 long 时没有返回正确的数值
 - 解决JdbcOperate异常后连接池泄露的问题
 - 对SQL 操作空结果集(null)的异常
 - JdbcOperator 函数冲突
 - 当同一进程内多个 socket 同时工作,会在 socket 关闭时关闭线程池,下一个 socket 则无法访问到线程池
 - Socket SSL通信在握手没有完成时开始接受消息导致异常
 - 每次访问后 Session 的超时时间没有刷新 
 - JSON数组解析最后一个元素和"]"直接会被误解析为一个元素
 - 换行消息截断实现类处理异常原始判断逻辑\r 或者\n 判断为换行. 但 window 以\r\n换行,导致断包下一个消息的起始字符多了一个\n
 - HTTP Chunked响应报文结束判断逻辑问题
 - POST 的 multipart/form-data类型参数解析异常
 - JSON 转对象时如果有注释会失败.如果JSON转对象失败不会再返回解析后的 Map,而是返回null.
 - 优化 Http 消息分段算发


####优化内容:####

 - 在使用监视过滤器的时候,统计的处理时间没有包含过滤器处理的时间,将监视过滤器(HttpMonitorFilter)从过滤器链的最后一个调整到第一个
 - 抽离出内部类 FilterConfig2.为 HttpRequest 增加 attribute 
 - 修改 HttpBizFilter 的方法,增加了返回值往复的传递机制,用于控制过滤去的状态
 - 为 HttpServer 提供了链式调用服务
 - 增加 Http 服务监控模块
 - TFile 中增加读取文件最后几行的行数public static byte[] loadFileLastLines(*,*)
 - 在 Web.json 中增加是否输出 access.log 日志文件的控制
 - Body 对象增加长度判断函数size()
 - 反射方法增加忽略属性名大小写的 findField 方法
 - 加 queryObject 和 queryObjectList 等方法,表的列名 和 对象的属性名的模糊匹配
 - 异步通信类使用非栈内存管理缓冲区提高性能
 - JdbcOperate 提供存储过程调用方法
 - 在未读取到数据时不进行消息截断的判断
 - 增加 SSL 连接 无客户端认证模式
 - 增加 TimeOutMessageSplitter,并且在 Aio 和 Nio中实现如果没有添加MessageSplitter,则使用TimeOutMessageSplitter作为默认
 - 增加单日志输出类用于补充输出特定的日志文件 SingleLogger
 - 异步通信包的 IoSession 类的 send 方法修改为 public,但要注意该方法不会出发 on Sent 事件
 - 为HttpServer 增加访问日志 access.log
 - 对默认的WEB 服务的根路径进行优化,可以自动识别相对和绝对路径
 - TFile 增加文件写入函数(采用流的形式)
 - TFile 增加取文件大小的函数
 - TFile 读取文件的时候没有用UTF-8转码,读不出带中文路径的文件
 - Logger.simple支持日志缩进(/s,/t)
 - Logger 增加日志输出控制方法setState()
 - 为 HttpClient 对象增加 putHeader 方法
 - HttpClient在连续不断的发送请求时Cookie自动留存
 - 将 HTTPClient 修改成可以不关闭连接持续不断的返送 HTTP 请求.
 - JdbcOperate 使用":"来标识参数会和时间格式冲突,修改成"::"标识参数
 - 日志工具异常处理,避免因日志异常导致进程结束.
 - 增加HTTPClient 自定 Cookie 的功能
 - 新增JSONDecode 中对 Long 的支持
 - Log 日志工具需要自动分割生成每天的日志文件
 - 为 HTTP 服务增加过滤器支持
 - 对请求参数,在构造时自动使用字符集参数进行编码.
 - Part 对象增加构造,可直接用构造函数构造简单Part,并提供字符集支持
 - Http 请求对象增加根据请求类型自动填写ContentType.
 - 增加 Connection: keep-alive 头,避免 HTTP 报文被服务端断开连接后接受报文不完成
 - 增加按换行返回消息分割器
 - 增加按定长返回消息分割器