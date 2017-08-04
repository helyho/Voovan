package org.voovan.http.extend.socketio;

import org.voovan.http.extend.ParserException;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

/**
 * socket.io 报文解析
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SIOParser {

    private static final char SPLITER = ',';
    private static final char DATA_BEGIN = '[';

    /**
     * 判断是否是 socket.io 的消息
     * @param msg 消息
     * @return true: 是, false: 否
     */
    public static boolean isSocketIOMessage(String msg){
        return TString.regexMatch(msg, "(^[0-6].*?,(\\d*\\[.*\\])?)|(^[0-6]\\[.*\\])") >0;
    }

    /**
     * 解析字符串成 Packet 对象
     * @param msg 字符串
     * @return SIOPacket 对象
     * @throws ParserException 解析异常
     */
    public static SIOPacket decode(String msg) throws ParserException {
        SIOPacket packet = new SIOPacket();
        StringBuilder stringBuilder = new StringBuilder();

        char c = msg.charAt(0);

        int socketType = c-48;
        if(!TString.isNumber(String.valueOf(c),10)){
            throw new ParserException("The socket.io packet first char must be number");
        }

        if(socketType<0 || socketType>6) {
            throw new ParserException("The socket.io packet first char must be exists in [0...6]");
        }

        packet.setSocketType(socketType);

        int dotPosition = msg.indexOf(',');
        int dataPosition = msg.indexOf('[');
        if( (dotPosition>0 && dataPosition<0) || (dotPosition < dataPosition)) {
            packet.setNsp(msg.substring(1, dotPosition));
        }

        if(dotPosition>0 && dataPosition>0 && dotPosition < dataPosition ){
            String seqValue = msg.substring(dotPosition + 1, dotPosition + 2);
            if(TString.isNumber(seqValue, 10)) {
                packet.setSeq(Integer.valueOf(msg.substring(dotPosition + 1, dotPosition + 2)));
            }
        }

        if(dataPosition>0) {
            dataPosition = dataPosition < 0 ? 1 : dataPosition;
            packet.setData(msg.substring(dataPosition));
        }

        return packet;
    }

    /**
     * 编码 Packet 成 字符串
     * @param packet SIOPacket 对象
     * @return 字符串
     */
    public static String encode(SIOPacket packet){
        StringBuilder msg = new StringBuilder();
        msg.append(packet.getSocketType());

        if(packet.getNsp()!=null) {
            msg.append(packet.getNsp());
            msg.append(SPLITER);
        }

        if(packet.getSeq()!=-1) {
            msg.append(packet.getSeq());
        }

        if(packet.getData()!=null) {
            msg.append(packet.getData());
        }

        return msg.toString();

    }


    public static void main(String[] args) throws ParserException {
        String mm = "2/socketio,0[\"show\", \"kkkk\"]";
        Logger.simple(SIOParser.isSocketIOMessage(mm));
        SIOPacket packet = SIOParser.decode(mm);
        Logger.simple(SIOParser.encode(packet));

        mm = "2[\"show\", \"kkkk\"]";
        Logger.simple(SIOParser.isSocketIOMessage(mm));
        packet = SIOParser.decode(mm);
        Logger.simple(SIOParser.encode(packet));

        mm = "0/socketio,";
        packet = SIOParser.decode(mm);
        Logger.simple(SIOParser.isSocketIOMessage(mm));
        Logger.simple(SIOParser.encode(packet));

        mm = "2/socketio,[\"show\", \"kkkk\"]";
        Logger.simple(SIOParser.isSocketIOMessage(mm));
        packet = SIOParser.decode(mm);
        Logger.simple(SIOParser.encode(packet));
    }
}
