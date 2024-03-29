ServerName : VoovanWebServer,                                 // 当前服务节点名称
Host : 127.0.0.1,                                             // 服务 IP 地址,默认0.0.0.0
Port : 28080,                                                 // 服务端口,默认28080
ReadTimeout : 30,                                             // 连接超时时间(s),默认30秒
SendTimeout : 30,                                             // 连接超时时间(s),默认30秒
IndexFiles : index.htm, index.html, default.htm, default.htm, //定义首页索引文件的名称
ContextPath : WEBAPP,                                         // 上下文路径,绝对路径 /起始,相对路径 非/ 起始,默认是WEBAPP
MatchRouteIgnoreCase : false,                                 // 匹配路由不区分大小写,默认是 false
CharacterSet : GB2312,                                        // 默认字符集,默认 UTF-8

SessionContainer : org.voovan.tools.collection.CacheMap, // Session 容器类,默认com.dd.tools.collection.CachedHashMap
SessionTimeout : 1,                                      // Session 会话超时时间(m),默认30分钟, 如果设置小于等于0,则会被默认设置为30分钟

KeepAliveTimeout : 60, // KeepAlive 超时时间(s),默认60秒,如果值小于等于0则不启用 KeepAlive 设置 (该参数同样会被应用到 WebSocket 的连接保持上)
Gzip : true,           // 是否启用Gzip压缩,默认 true
GzipMinSize : 1024,    // 启用Gzip压缩的最小响应报文, 默认 2048 byte 以上启用 gzip 压缩
GzipMimeType : [
  "text/html",
  "text/plain",
  "text/css",
  "text/javascript",
  "application/javascript",
  "application/json"],                                     // 启用Gzip压缩的最小响应报文, 默认 2048 byte 以上启用 gzip 压缩
AccessLog : true,                                        // 是否记录access.log,默认 true
HotSwapInterval : 3,                                     // 热加载检测时间间隔. 默认:0秒. 0:关闭
LifeCycleClass : org.voovan.test.http.WebLifeCycleClass, // 配置在Web 服务启动时加载并运行初始化类, 该类需继承:org.voovan.http.server.WebServerInit
PauseURL : "/img/logo.jpg",                             // 服务器暂停状态下所有请求都会转向这个路由
Cache : false,
maxRequestSize : 20480, //请求大小的限制(单位:kb), 大于这个值的连接将会被放弃, -1 不限制上传文件的大小. 默认值: 10mb

WeaveConfig : {
  Scan : org.voovan,
  Inject : org.voovan
},

//  HTTPS证书配置
//  Https: {
//      CertificateFile        : "/src/test/java/org/voovan/test/http/ssl_ks",  // HTTPS 证书
//      CertificatePassword    : passStr,                // HTTPS 证书密码
//      KeyPassword            : 123123,                 // HTTPS 证书Key 密码
//  },

// 过滤器配置节点 请求 先执行filter1, 后执行filter2,响应则相反

Filters[
  {
    Name : filter1,
    ClassName : org.voovan.test.http.HttpFilterTest,
    Encoding : UTF-8,
    Action : pass },
  {
    Name : filter2,
    ClassName : org.voovan.test.http.HttpFilterTest,
    Encoding : UTF-8,
    Action : pass },
  {
    Name : filter3,
    ClassName : org.voovan.test.http.HttpFilterTest,
    Encoding : UTF-8,
    Action : pass },
  {
    Name : TokenBucketFilter, //限制请求的过滤器
    enable : true,
    ClassName : org.voovan.http.server.filter.RateLimiterFilter,

    limiter[
      {
        limitSize : 1,
        interval : 1,
        value : "/test",
        type : url,
        response : your request is limited
      },
      //        {
      //          limitSize:1,
      //          interval:5000,
      //          value: 127.0.0.1,
      //          type:IP,
      //          response: your request is limited
      //        },
      //        {
      //          limitSize:1,       //限流数量, 令牌桶为每次新增的令牌数, 漏桶为刷新后桶的容量
      //          interval:5000,     //限流器刷新时间, 令牌桶为新增令牌时间, 漏桶为刷新时间
      //          value: Connection, //限流的数据类型, 在 URL 限流时为数据, ip 这个数据无效, HEADER 和 SESSOIN 为具体数据的 key
      //          type:HEADER,       //限流的类型: HEADER, URL, SESSION, IP
      //          response: your request is limited,  // 被限流时的响应
      //          bucketType : TOKEN //过滤器限流类型: LEAK 漏桶, TOKEN 令牌桶
      //        },
      //        {
      //          limitSize:1,
      //          interval:5000,
      //          value: Connection,
      //          type:ession,
      //          response: your request is limited,
      //          bucketType : TOKEN
      //        },
    ]
  }
],

//路由管理器配置节点
Routers[
  {
    Name : 配置路由测试,                                         //路由名称
    Route : "/testRouter",                                //Http请求路径
    Method : GET,                                          //Http请求方法
    ClassName : org.voovan.test.http.router.HttpTestRouter //Http 路由处理器
  }
],

Modules[
  {
    Name : AnnotationModule, //模块名称
    // Path: /,                                                  //模块路径
    // ScanRouterPackage      : org.voovan.test.http.router,     //注解形式的路由扫描的包路径, 默认: null, 不设置这个属性则会被任务不开启
    ScanRouterInterval : 3,                                                       //注解形式的路由扫描的包路径的时间间隔. 默认:0秒. 0:关闭
    ClassName : org.voovan.http.server.module.annontationRouter.AnnotationModule, //模块处理器
    LifeCycleClass : org.voovan.test.http.HttpModuleLifeCycleClass,
    asyncRunnerSize : 16,
    isAsyncRunnerSteal : false,
    asyncRunnerSelector : org.voovan.http.server.module.annontationRouter.router.AsyncSocketBindSelector,


    Swagger {
      Enable : true,
      RoutePath : "/swagger",
      RefreshInterval : 30,
      Description : webserver swagger test,
      Version : v1.0.0
    }
  },
  //    {
  //      Name: Monitor,                                                    //模块名称
  //      Path: /monitor,                                                   //模块路径
  //      ClassName: org.voovan.http.server.module.monitor.MonitorModule,   //模块处理器
  //      AllowIPAddress: [127.0.0.1, 10.0.0.4]
  //    }
]