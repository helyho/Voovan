package org.voovan.http.extend.socketio;

/**
 * socket.io 报文
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class SIOPacket {

    public static final int CONNECT = 0;
    public static final int DISCONNECT = 1;
    public static final int EVENT = 2;
    public static final int ACK = 3;
    public static final int ERROR = 4;
    public static final int BINARY_EVENT = 5;
    public static final int BINARY_ACK = 6;


    private static int seqValue = 0;

    private int seq = -1;
    private String nsp = null;
    private String data = null;
    private int socketType = -1;

    public static String[] SOCKET_TYPES = new String[] {
            "CONNECT",
            "DISCONNECT",
            "EVENT",
            "ACK",
            "ERROR",
            "BINARY_EVENT",
            "BINARY_ACK"
    };

    public SIOPacket() {
    }

    public SIOPacket(int socketType, String nsp, String data) {
        this.socketType = socketType;
        this.data = data;
        this.nsp = nsp;
        this.seqValue = seqValue;
        this.seqValue++;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public String getNsp() {
        return nsp;
    }

    public void setNsp(String nsp) {
        this.nsp = nsp;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getSocketType() {
        return socketType;
    }

    public void setSocketType(int socketType) {
        this.socketType = socketType;
    }
}
