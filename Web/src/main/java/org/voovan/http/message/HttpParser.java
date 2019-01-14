package org.voovan.http.message;

import org.voovan.Global;
import org.voovan.http.message.packet.Cookie;
import org.voovan.http.message.packet.Part;
import org.voovan.http.server.exception.ParserException;
import org.voovan.http.server.exception.RequestTooLarge;
import org.voovan.tools.*;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.Map.Entry;


/**
 * Http 报文解析类
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpParser {

    private static final String HTTP_PROTOCOL = "HTTP/";

    private static final String FL_METHOD 		= "FL_Method";
    private static final String FL_PATH 		= "FL_Path";
    private static final String FL_PROTOCOL		= "FL_Protocol";
    private static final String FL_VERSION		= "FL_Version";
    private static final String FL_STATUS		= "FL_Status";
    private static final String FL_STATUS_CODE = "FL_StatusCode";
    private static final String FL_QUERY_STRING = "FL_QueryString";

    public static final String MULTIPART_FORM_DATA = "multipart/form-data";

    private static final String BODY_PARTS = "Body_Parts";
    private static final String BODY_VALUE = "Body_Value";
    private static final String BODY_FILE = "Body_File";

    public static final String UPLOAD_PATH = TFile.assemblyPath(TFile.getTemporaryPath(),"voovan", "webserver", "upload");

    public static final String propertyLineRegex = ": ";
    public static final String equalMapRegex = "([^ ;,]+=[^;,]+)";

    public static ThreadLocal<Map<String, Object>> THREAD_PACKET_MAP = ThreadLocal.withInitial(()->new HashMap<String, Object>());
    public static ThreadLocal<Request> THREAD_REQUEST = ThreadLocal.withInitial(()->new Request());
    public static ThreadLocal<Response> THREAD_RESPONSE = ThreadLocal.withInitial(()->new Response());

    /**
     * 私有构造函数
     * 该类无法被实例化
     */
    private HttpParser(){

    }

    /**
     * 解析 HTTP Header属性行
     * @param propertyLine
     *              Http 报文头属性行字符串
     * @return
     */
    private static Map<String,String> parsePropertyLine(String propertyLine){
        Map<String,String> property = new HashMap<String, String>();

        int index = propertyLine.indexOf(propertyLineRegex);
        if(index > 0){
            String propertyName = propertyLine.substring(0, index);
            String properyValue = propertyLine.substring(index+2, propertyLine.length());

            property.put(fixHeaderName(propertyName), properyValue.trim());
        }

        return property;
    }

    /**
     * 校正全小写形式的 Http 头
     * @param headerName http 头的行数据
     * @return 校正后的http 头的行数据
     */
    public static String fixHeaderName(String headerName) {
        if(headerName==null){
            return null;
        }
        String[] headerNameSplits = headerName.split("-");
        StringBuilder stringBuilder = new StringBuilder();
        for(String headerNameSplit : headerNameSplits) {
            if(Character.isLowerCase(headerNameSplit.codePointAt(0))){
                stringBuilder.append((char)(headerNameSplit.codePointAt(0) - 32));
                stringBuilder.append(TString.removePrefix(headerNameSplit));
            } else {
                stringBuilder.append(headerNameSplit);
            }

            stringBuilder.append("-");
        }

        return TString.removeSuffix(stringBuilder.toString());
    }

    /**
     * 解析字符串中的所有等号表达式成 Map
     * @param str
     *              等式表达式
     * @return 等号表达式 Map
     */
    public static Map<String, String> getEqualMap(String str){
        Map<String, String> equalMap = new HashMap<String, String>();
        String[] searchedStrings = TString.searchByRegex(str, equalMapRegex);
        for(String groupString : searchedStrings){
            //这里不用 split 的原因是有可能等号后的值字符串中出现等号
            String[] equalStrings = new String[2];
            int equalCharIndex= groupString.indexOf(Global.CHAR_EQUAL);
            equalStrings[0] = groupString.substring(0,equalCharIndex);
            equalStrings[1] = groupString.substring(equalCharIndex+1,groupString.length());
            if(equalStrings.length==2){
                String key = equalStrings[0];
                String value = equalStrings[1];
                if(value.startsWith(Global.CHAR_QUOTATION) && value.endsWith(Global.CHAR_QUOTATION)){
                    value = value.substring(1,value.length()-1);
                }
                equalMap.put(key, value);
            }
        }
        return equalMap;
    }

    /**
     * 获取HTTP 头属性里等式的值
     * 		可以从字符串 Content-Type: multipart/form-data; boundary=ujjLiiJBznFt70fG1F4EUCkIupn7H4tzm
     * 		直接解析出boundary的值.
     * 		使用方法:getPerprotyEqualValue(packetMap,"Content-Type","boundary")获得ujjLiiJBznFt70fG1F4EUCkIupn7H4tzm
     * @param propertyName   属性名
     * @param valueName      属性值
     * @return
     */
    private static String getPerprotyEqualValue(Map<String,Object> packetMap,String propertyName,String valueName){
        Object propertyValueObj = packetMap.get(propertyName);
        if(propertyValueObj == null){
            return null;
        }
        String propertyValue = propertyValueObj.toString();
        Map<String, String> equalMap = getEqualMap(propertyValue);
        return equalMap.get(valueName);
    }

    /**
     * 处理消息的Cookie
     * @param packetMap         报文 MAp 对象
     * @param cookieName        Http 头中 Cookie 报文行
     * @param cookieValue        Http 头中 Cookie 报文行
     */
    @SuppressWarnings("unchecked")
    private static void parseCookie(Map<String, Object> packetMap,String cookieName, String cookieValue){
        if(!packetMap.containsKey(HttpStatic.COOKIE_STRING)){
            packetMap.put(HttpStatic.COOKIE_STRING, new ArrayList<Map<String, String>>());
        }
        List<Map<String, String>> cookies = (List<Map<String, String>>) packetMap.get(HttpStatic.COOKIE_STRING);

        //解析 Cookie 行
        Map<String, String>cookieMap = getEqualMap(cookieValue);

        //响应 response 的 cookie 形式 一个cookie 一行
        if(HttpStatic.SET_COOKIE_STRING.equalsIgnoreCase(cookieName)){
            //处理非键值的 cookie 属性
            if(cookieValue.toLowerCase().contains(HttpStatic.HTTPONLY_STRING)){
                cookieMap.put(HttpStatic.HTTPONLY_STRING, Global.EMPTY_STRING);
            }
            if(cookieValue.toLowerCase().contains(HttpStatic.SECURE_STRING)){
                cookieMap.put(HttpStatic.SECURE_STRING, Global.EMPTY_STRING);
            }
            cookies.add(cookieMap);
        }
        //请求 request 的 cookie 形式 多个cookie 一行
        else if(HttpStatic.COOKIE_STRING.equalsIgnoreCase(cookieName)){
            for(Entry<String,String> cookieMapEntry: cookieMap.entrySet()){
                HashMap<String, String> cookieOneMap = new HashMap<String, String>();
                cookieOneMap.put(cookieMapEntry.getKey(), cookieMapEntry.getValue());
                cookies.add(cookieOneMap);
            }
        }

    }

    /**
     * 处理 body 段
     * 		判断是否使用 GZIP 压缩,如果使用则解压缩后返回,如果没有压缩则直接返回
     * @param packetMap
     * @param contentBytes
     * @return
     * @throws IOException
     */
    private static byte[] dealBodyContent(Map<String, Object> packetMap,byte[] contentBytes) throws IOException{
        byte[] bytesValue;
        if(contentBytes.length == 0 ){
            return contentBytes;
        }

        //是否支持 GZip
        boolean isGZip = packetMap.get(HttpStatic.CONTENT_ENCODING_STRING)==null ? false : packetMap.get(HttpStatic.CONTENT_ENCODING_STRING).toString().contains(HttpStatic.GZIP_STRING);

        //如果是 GZip 则解压缩
        if(isGZip && contentBytes.length>0){
            bytesValue = TZip.decodeGZip(contentBytes);
        } else {
            bytesValue = contentBytes;
        }
        return TObject.nullDefault(bytesValue,new byte[0]);
    }

    /**
     * 解析 HTTP 请求写一行
     * @param packetMap 解析后数据的容器
     * @param byteBufferChannel ByteBufferChannel对象
     * @return 解析后的便宜量
     * @throws ParserException 解析异常
     */
    public static int parserProtocol(Map<String, Object> packetMap, ByteBufferChannel byteBufferChannel) throws ParserException {
        StringBuilder stringBuilder = new StringBuilder();

        int position = 0;

        //遍历 Protocol
        int segment = 0;
        int protocolType = 0; //0: request, 1:response
        char prevChar = '\0';
        char currentChar = '\0';

        ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();

        while (!byteBufferChannel.isReleased() && byteBufferChannel.size() > 0) {
            if(position >= byteBufferChannel.size()){
                throw new ParserException("Parse header reach the stream end");
            }

            currentChar = (char)(byteBuffer.get(position) & 0xFF);
            position++;


            if (currentChar == '/') {
                if (segment == 0 || segment == 2) {
                    if (stringBuilder.length()== 4 && stringBuilder.indexOf(HttpStatic.HTTP_STRING) == 0) {
                        if (segment == 0) {
                            protocolType = 1;
                        }
                        packetMap.put(FL_PROTOCOL, HttpStatic.HTTP_STRING);
                        stringBuilder = new StringBuilder();
                    }

                    continue;
                }
            }

            if (currentChar == ' ') {
                if (segment == 0) {
                    if (protocolType == 0) {
                        packetMap.put(FL_METHOD, stringBuilder.toString());
                    } else {
                        packetMap.put(FL_VERSION, stringBuilder.toString());
                    }

                    stringBuilder = new StringBuilder();
                    segment = 1;
                    continue;
                }

                if (segment == 1) {
                    if (protocolType == 0) {
                        if (packetMap.containsKey(FL_PATH)) {
                            packetMap.put(FL_QUERY_STRING, stringBuilder.toString());
                        } else {
                            packetMap.put(FL_PATH, stringBuilder.toString());
                        }
                    } else {
                        packetMap.put(FL_STATUS,stringBuilder.toString());
                    }

                    stringBuilder = new StringBuilder();
                    segment = 2;
                }

                continue;
            }

            if (currentChar == '?') {
                if (segment == 1) {
                    packetMap.put(FL_PATH, stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    continue;
                }
            }

            if (prevChar == '\r' && currentChar == '\n' && segment == 2) {
                if (protocolType == 0) {
                    packetMap.put(FL_VERSION, stringBuilder.toString());
                } else {
                    packetMap.put(FL_STATUS_CODE, stringBuilder.toString());
                }
                stringBuilder = new StringBuilder();
                break;
            }


            prevChar = currentChar;
            if (currentChar == '\r') {
                continue;
            }

            stringBuilder.append(currentChar);
        }

        byteBufferChannel.compact();

        if(!packetMap.containsKey(FL_PROTOCOL)){
            packetMap.clear();
            position = 0;
        }

        return position;
    }

    /**
     * 解析 HTTP 请求头
     * @param packetMap 解析后数据的容器
     * @param offset 解析开始的偏移量位置
     * @param byteBufferChannel ByteBufferChannel对象
     * @return 解析后的便宜量
     * @throws ParserException 解析异常
     */
    public static int parseHeader(Map<String, Object> packetMap, int offset, ByteBufferChannel byteBufferChannel) throws ParserException {
        StringBuilder stringBuilder = new StringBuilder();

        int position = offset;

        //遍历 Protocol
        boolean isHeaderName = true;
        char prevChar = '\0';
        char currentChar = '\0';
        String headerName = null;
        String headerValue = null;

        ByteBuffer byteBuffer = byteBufferChannel.getByteBuffer();

        while (!byteBufferChannel.isReleased() && byteBufferChannel.size() > 0) {
            if(position >= byteBufferChannel.size()){
                throw new ParserException("Parse header reach the stream end");
            }

            currentChar = (char)(byteBuffer.get(position) & 0xFF);
            position++;

            if(isHeaderName && prevChar==':' && currentChar==' ') {
                headerName = stringBuilder.toString();
                isHeaderName = false;
                stringBuilder = new StringBuilder();
                continue;
            }

            if(!isHeaderName && prevChar=='\r' && currentChar=='\n') {
                headerValue = stringBuilder.toString();
                break;
            }

            //http 头结束了
            if(isHeaderName && prevChar=='\r' && currentChar=='\n' && position == offset + 2){
                packetMap.put(null, null);
                return position;
            }

            prevChar = currentChar;

            if(isHeaderName && currentChar==':') {
                continue;
            }

            if(!isHeaderName && currentChar=='\r') {
                continue;
            }

            stringBuilder.append(currentChar);

        }

        byteBufferChannel.compact();

        packetMap.put(fixHeaderName(headerName), headerValue);
        return position;

    }

    /**
     * 解析 HTTP 报文
     * 		解析称 Map 形式,其中:
     * 			1.protocol 解析成 key/value 形式
     * 			2.header   解析成 key/value 形式
     * 			3.cookie   解析成 List[Map[String,String]] 形式
     * 			3.part     解析成 List[Map[Stirng,Object]](因为是递归,参考 HTTP 解析形式) 形式
     * 			5.body     解析成 key=BODY_VALUE 的Map 元素
     * @param packetMap 用于填充的解析 map
     * @param byteBufferChannel 输入流
     * @param timeOut 读取超时时间参数
     * @param requestMaxSize 上传文件的最大尺寸, 单位: kb
     * @return 解析后的 Map
     * @throws IOException IO 异常
     */

    public static Map<String, Object> parser(Map<String, Object> packetMap, ByteBufferChannel byteBufferChannel, int timeOut, long requestMaxSize) throws IOException{
        long totalLength = 0;
        boolean isBodyConent = false;

        requestMaxSize = requestMaxSize < 0 ? Integer.MAX_VALUE : requestMaxSize;

        int position = 0;

        //按行遍历HTTP报文
        while(position < byteBufferChannel.size()){

            position = parserProtocol(packetMap, byteBufferChannel);

            String cookieName = null;
            String cookieValue = null;

            while(!packetMap.containsKey(null)) {
                position = parseHeader(packetMap, position, byteBufferChannel);
            }

            if(packetMap.containsKey(HttpStatic.SET_COOKIE_STRING)){
                cookieName = HttpStatic.SET_COOKIE_STRING;
                cookieValue = packetMap.get(HttpStatic.SET_COOKIE_STRING).toString();
                packetMap.remove(HttpStatic.SET_COOKIE_STRING);
            }

            if(packetMap.containsKey(HttpStatic.COOKIE_STRING)){
                cookieName = HttpStatic.COOKIE_STRING;
                cookieValue = packetMap.get(HttpStatic.COOKIE_STRING).toString();
                packetMap.remove(HttpStatic.COOKIE_STRING);
            }

            if(cookieName!=null){
                parseCookie(packetMap, cookieName, cookieValue);
            }

            packetMap.remove(null);

            byteBufferChannel.shrink(position);

            //如果 消息缓冲通道没有数据或已关闭
            if(byteBufferChannel.size() <= 0) {
                break;
            }

            isBodyConent = true;

            totalLength = position;

            //解析 HTTP 请求 body
            if(isBodyConent){
                String contentType =packetMap.get(HttpStatic.CONTENT_TYPE_STRING)==null ? Global.EMPTY_STRING : packetMap.get(HttpStatic.CONTENT_TYPE_STRING).toString();
                String transferEncoding = packetMap.get(HttpStatic.TRANSFER_ENCODING_STRING)==null ? "" : packetMap.get(HttpStatic.TRANSFER_ENCODING_STRING).toString();

                //1. 解析 HTTP 的 POST 请求 body part
                if(contentType.contains(MULTIPART_FORM_DATA)){
                    //用来保存 Part 的 list
                    List<Map<String, Object>> bodyPartList = new ArrayList<Map<String, Object>>();

                    //取boundary 用于 part 内容分段
                    String boundary = TString.assembly("--", getPerprotyEqualValue(packetMap, HttpStatic.CONTENT_TYPE_STRING, HttpStatic.BOUNDARY_STRING));

                    ByteBuffer boundaryEnd = ByteBuffer.allocate(2);
                    while(true) {
                        //等待数据
                        if (!byteBufferChannel.waitData(boundary.getBytes(), timeOut)) {
                            throw new ParserException("Http Parser read data error");
                        }

                        int index = byteBufferChannel.indexOf(boundary.getBytes(Global.CS_UTF_8));

                        //跳过 boundary
                        byteBufferChannel.shrink((index + boundary.length()));

                        //取 boundary 结尾字符
                        boundaryEnd.clear();
                        int readSize = byteBufferChannel.readHead(boundaryEnd);

                        //累计请求大小
                        totalLength = totalLength + readSize;
                        //请求过大的处理
                        if(totalLength > requestMaxSize * 1024){
                            throw new RequestTooLarge("Request is too large: {max size: " + requestMaxSize*1024 + ", expect size: " + totalLength + "}");
                        }

                        //确认 boundary 结尾字符, 如果是"--" 则标识报文结束
                        if (Arrays.equals(boundaryEnd.array(), "--".getBytes())) {
                            //收缩掉尾部的换行
                            byteBufferChannel.shrink(2);
                            break;
                        }

                        byte[] mark = HttpStatic.BODY_MARK.getBytes();
                        //等待数据
                        if (!byteBufferChannel.waitData(mark, timeOut)) {
                            throw new ParserException("Http Parser read data error");
                        }

                        int partHeadEndIndex = byteBufferChannel.indexOf(mark);


                        //Part 头读取
                        ByteBuffer partHeadBuffer = TByteBuffer.allocateDirect(partHeadEndIndex + 4);
                        byteBufferChannel.readHead(partHeadBuffer);

                        //构造新的 Bytebufer 递归解析
                        ByteBufferChannel partByteBufferChannel = new ByteBufferChannel(partHeadEndIndex + 4); //包含换行符
                        partByteBufferChannel.writeEnd(partHeadBuffer);
                        Map<String, Object> partMap = new HashMap<String, Object>();
                        partMap = parser(partMap, partByteBufferChannel, timeOut, requestMaxSize);
                        TByteBuffer.release(partHeadBuffer);
                        partByteBufferChannel.release();
                        TByteBuffer.release(partHeadBuffer);

                        String fileName = getPerprotyEqualValue(partMap, HttpStatic.CONTENT_DISPOSITION_STRING, "filename");
                        if(fileName!=null && fileName.isEmpty()){
                            break;
                        }

                        //解析 Part 报文体
                        //重置 index
                        index = -1;
                        //普通参数处理
                        if (fileName == null) {
                            //等待数据
                            if (!byteBufferChannel.waitData(boundary.getBytes(), timeOut)) {
                                throw new ParserException("Http Parser read data error");
                            }

                            index = byteBufferChannel.indexOf(boundary.getBytes(Global.CS_UTF_8));


                            ByteBuffer bodyByteBuffer = ByteBuffer.allocate(index - 2);
                            byteBufferChannel.readHead(bodyByteBuffer);
                            partMap.put(BODY_VALUE, bodyByteBuffer.array());
                        }
                        //文件处理
                        else {

                            String fileExtName = TFile.getFileExtension(fileName);
                            fileExtName = fileExtName==null || fileExtName.equals(Global.EMPTY_STRING) ? ".tmp" : fileExtName;

                            //拼文件名
                            String localFileName = UPLOAD_PATH + TString.assembly(Global.NAME, System.currentTimeMillis(), ".", fileExtName);

                            //文件是否接收完成
                            boolean isFileRecvDone = false;

                            while (true){
                                byteBufferChannel.getByteBuffer();
                                int dataLength = 0;

                                try {
                                    dataLength = byteBufferChannel.size();
                                    //等待数据, 1毫秒超时
                                    if (byteBufferChannel.waitData(boundary.getBytes(), 1)) {
                                        isFileRecvDone = true;
                                    }
                                } finally {
                                    byteBufferChannel.compact();
                                }

                                if(!isFileRecvDone) {
                                    if(byteBufferChannel.size() > 1024*10) {
                                        byteBufferChannel.saveToFile(localFileName, dataLength);
                                        //累计请求大小
                                        totalLength = totalLength + dataLength;
                                    } else {
                                        continue;
                                    }
                                } else {
                                    index = byteBufferChannel.indexOf(boundary.getBytes(Global.CS_UTF_8));
                                    int length = index == -1 ? byteBufferChannel.size() : (index - 2);
                                    if (index > 0) {
                                        byteBufferChannel.saveToFile(localFileName, length);
                                        totalLength = totalLength + dataLength;
                                    }
                                }

                                //请求过大的处理
                                if(totalLength > requestMaxSize * 1024){
                                    TFile.deleteFile(new File(localFileName));
                                    throw new RequestTooLarge("Request is too large: {max size: " + requestMaxSize*1024 + ", expect size: " + totalLength + "}");
                                }


                                if(!isFileRecvDone){
                                    TEnv.sleep(100);
                                } else {
                                    break;
                                }

                            }

                            if(index == -1){
                                new File(localFileName).delete();
                                throw new ParserException("Http Parser read data error");
                            }else{
                                partMap.remove(BODY_VALUE);
                                partMap.put(BODY_FILE, localFileName.getBytes());
                            }
                        }

                        //加入bodyPartList中
                        bodyPartList.add(partMap);
                    }
                    //将存有多个 part 的 list 放入packetMap
                    packetMap.put(BODY_PARTS, bodyPartList);
                }

                //2. 解析 HTTP 响应 body 内容段的 chunked
                else if(HttpStatic.CHUNKED_STRING.equals(transferEncoding)){

                    ByteBufferChannel chunkedByteBufferChannel = new ByteBufferChannel(3);
                    String chunkedLengthLine = "";

                    while(chunkedLengthLine!=null){

                        // 等待数据
                        if(!byteBufferChannel.waitData("\r\n".getBytes(), timeOut)){
                            throw new ParserException("Http Parser read data error");
                        }

                        chunkedLengthLine = byteBufferChannel.readLine().trim();

                        if("0".equals(chunkedLengthLine)){
                            break;
                        }

                        if(chunkedLengthLine.isEmpty()){
                            continue;
                        }

                        int chunkedLength = 0;
                        //读取chunked长度
                        try {
                            chunkedLength = Integer.parseInt(chunkedLengthLine, 16);
                        }catch(Exception e){
                            e.printStackTrace();
                            break;
                        }

                        // 等待数据
                        if(!byteBufferChannel.waitData(chunkedLength, timeOut)){
                            throw new ParserException("Http Parser read data error");
                        }

                        int readSize = 0;
                        if(chunkedLength > 0) {
                            //按长度读取chunked内容
                            ByteBuffer byteBuffer = TByteBuffer.allocateDirect(chunkedLength);
                            readSize = byteBufferChannel.readHead(byteBuffer);

                            //累计请求大小
                            totalLength = totalLength + readSize;
                            //请求过大的处理
                            if(readSize != chunkedLength){
                                throw new ParserException("Http Parser read chunked data error");
                            }

                            //如果多次读取则拼接
                            chunkedByteBufferChannel.writeEnd(byteBuffer);
                            TByteBuffer.release(byteBuffer);
                        }

                        //请求过大的处理
                        if(totalLength > requestMaxSize * 1024){
                            throw new RequestTooLarge("Request is too large: {max size: " + requestMaxSize*1024 + ", expect size: " + totalLength + "}");
                        }

                        //跳过换行符号
                        byteBufferChannel.shrink(2);
                    }

                    byte[] value = dealBodyContent(packetMap, chunkedByteBufferChannel.array());
                    chunkedByteBufferChannel.release();
                    packetMap.put(BODY_VALUE, value);
                    byteBufferChannel.shrink(2);
                }

                //3. HTTP(请求和响应) 报文的内容段中Content-Length 提供长度,按长度读取 body 内容段
                else if(packetMap.containsKey(HttpStatic.CONTENT_LENGTH_STRING)){
                    int contentLength = Integer.parseInt(packetMap.get(HttpStatic.CONTENT_LENGTH_STRING).toString());

                    //累计请求大小
                    totalLength = totalLength + contentLength;

                    //请求过大的处理
                    if(totalLength > requestMaxSize * 1024){
                        throw new ParserException("Request is too large: {max size: " + requestMaxSize*1024 + ", expect size: " + totalLength + "}");
                    }


                    // 等待数据
                    if(!byteBufferChannel.waitData(contentLength, timeOut)){
                        throw new ParserException("Http Parser read data error");
                    }

                    ByteBuffer byteBuffer = ByteBuffer.allocate(contentLength);

                    byteBufferChannel.readHead(byteBuffer);
                    byte[] contentBytes = byteBuffer.array();

                    byte[] value = dealBodyContent(packetMap, contentBytes);
                    packetMap.put(BODY_VALUE, value);
                }

                break;
            }
        }

        return packetMap;
    }

    /**
     * 解析报文成 HttpRequest 对象
     * @param byteBufferChannel  输入字节流
     * @param timeOut 读取超时时间参数
     * @param requestMaxSize 上传文件的最大尺寸, 单位: kb
     * @return   返回请求报文
     * @throws IOException IO 异常
     */
    @SuppressWarnings("unchecked")
    public static Request parseRequest(ByteBufferChannel byteBufferChannel, int timeOut, long requestMaxSize) throws IOException{

        Map<String, Object> packetMap = THREAD_PACKET_MAP.get();
        try {
            packetMap = parser(packetMap, byteBufferChannel, timeOut, requestMaxSize);
        } catch (ParserException e) {
            Logger.warn("HttpParser.parser: " + e.getMessage());
            return null;
        }

        //如果解析的Map为空,则直接返回空
        if(packetMap==null || packetMap.isEmpty() || byteBufferChannel.isReleased()){
            return null;
        }

        Request request = THREAD_REQUEST.get();
        request.clear();
        //填充报文到请求对象
        Set<Entry<String, Object>> parsedItems= packetMap.entrySet();
        for(Entry<String, Object> parsedPacketEntry: parsedItems) {
            String key = parsedPacketEntry.getKey();
            switch (key) {
                case FL_METHOD:
                    request.protocol().setMethod(parsedPacketEntry.getValue().toString());
                    break;
                case FL_PROTOCOL:
                    request.protocol().setProtocol(parsedPacketEntry.getValue().toString());
                    break;
                case FL_QUERY_STRING:
                    request.protocol().setQueryString(parsedPacketEntry.getValue().toString());
                    break;
                case FL_VERSION:
                    request.protocol().setVersion(parsedPacketEntry.getValue().toString());
                    break;
                case FL_PATH:
                    request.protocol().setPath(parsedPacketEntry.getValue().toString());
                    break;
                case HttpStatic.COOKIE_STRING:
                    List<Map<String, String>> cookieMap = (List<Map<String, String>>)packetMap.get(HttpStatic.COOKIE_STRING);
                    //遍历 Cookie,并构建 Cookie 对象
                    for(Map<String,String> cookieMapItem : cookieMap){
                        Cookie cookie = Cookie.buildCookie(cookieMapItem);
                        request.cookies().add(cookie);
                    }
                    cookieMap.clear();
                    break;
                case BODY_VALUE:
                    byte[] value = (byte[])(parsedPacketEntry.getValue());
                    request.body().write(value);
                    break;
                case BODY_PARTS:
                    List<Map<String, Object>> parsedParts = (List<Map<String, Object>>)(parsedPacketEntry.getValue());
                    //遍历 part List,并构建 Part 对象
                    for(Map<String, Object> parsedPartMap : parsedParts){
                        Part part = new Part();
                        //将 part Map中的值,并填充到新构建的 Part 对象中
                        for(Entry<String, Object> parsedPartMapItem : parsedPartMap.entrySet()){
                            //填充 Value 中的值到 body 中
                            if(parsedPartMapItem.getKey().equals(BODY_VALUE)){
                                part.body().changeToBytes((byte[])parsedPartMapItem.getValue());
                            } if(parsedPartMapItem.getKey().equals(BODY_FILE)){
                                String filePath = new String((byte[])parsedPartMapItem.getValue());
                                part.body().changeToFile(new File(filePath));
                            } else {
                                //填充 header
                                String partedHeaderKey = parsedPartMapItem.getKey();
                                String partedHeaderValue = parsedPartMapItem.getValue().toString();
                                part.header().put(partedHeaderKey, partedHeaderValue);
                                if(HttpStatic.CONTENT_DISPOSITION_STRING.equals(partedHeaderKey)){
                                    //对Content-Disposition中的"name=xxx"进行处理,方便直接使用
                                    Map<String, String> contentDispositionValue = HttpParser.getEqualMap(partedHeaderValue);
                                    part.header().putAll(contentDispositionValue);
                                }
                            }
                        }
                        request.parts().add(part);
                        parsedPartMap.clear();
                    }
                    break;
                default:
                    request.header().put(parsedPacketEntry.getKey(), parsedPacketEntry.getValue().toString());
                    break;
            }
        }

        packetMap.clear();

        return request;
    }

    /**
     * 解析报文成 HttpResponse 对象
     * @param byteBufferChannel  输入字节流
     * @param timeOut 读取超时时间参数
     * @return   返回响应报文
     * @throws IOException IO 异常
     */
    @SuppressWarnings("unchecked")
    public static Response parseResponse(ByteBufferChannel byteBufferChannel, int timeOut) throws IOException {
        Map<String, Object> packetMap = THREAD_PACKET_MAP.get();
        try {
            packetMap = parser(packetMap, byteBufferChannel, timeOut, -1);
        } catch (ParserException e) {
            Logger.warn("HttpParser.parser: " + e.getMessage());
            return null;
        }

        //如果解析的Map为空,则直接返回空
        if(packetMap==null || packetMap.isEmpty() || byteBufferChannel.isReleased()){
            return null;
        }

        Response response = THREAD_RESPONSE.get();
        response.clear();
        //填充报文到响应对象
        Set<Entry<String, Object>> parsedItems= packetMap.entrySet();
        for(Entry<String, Object> parsedPacketEntry: parsedItems){
            String key = parsedPacketEntry.getKey();
            switch (key) {
                case FL_PROTOCOL:
                    response.protocol().setProtocol(parsedPacketEntry.getValue().toString());
                    break;
                case FL_VERSION:
                    response.protocol().setVersion(parsedPacketEntry.getValue().toString());
                    break;
                case FL_STATUS:
                    response.protocol().setStatus(Integer.parseInt(parsedPacketEntry.getValue().toString()));
                    break;
                case FL_STATUS_CODE:
                    response.protocol().setStatusCode(parsedPacketEntry.getValue().toString());
                    break;
                case HttpStatic.COOKIE_STRING:
                    List<Map<String, String>> cookieMap = (List<Map<String, String>>)parsedPacketEntry.getValue();
                    //遍历 Cookie,并构建 Cookie 对象
                    for(Map<String,String> cookieMapItem : cookieMap){
                        Cookie cookie = Cookie.buildCookie(cookieMapItem);
                        response.cookies().add(cookie);
                    }
                    break;
                case BODY_VALUE:
                    response.body().write((byte[])parsedPacketEntry.getValue());
                    break;
                default:
                    response.header().put(parsedPacketEntry.getKey(), parsedPacketEntry.getValue().toString());
                    break;
            }
        }
        packetMap.clear();
        return response;
    }

    public static void resetThreadLocal(){
        THREAD_REQUEST.set(new Request());
        THREAD_RESPONSE.set(new Response());
    }
}
