package org.voovan.test.tools.json;

import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ModbusMsg_04 {
    private short opcode = 0x04; //操作码
    private short dataLen = 0x00; //数据长度
    private int[] data = null; //数据体

    public ModbusMsg_04(){

    }

    public ModbusMsg_04(short dataLen){
        this.dataLen=dataLen;
    }

    /**
     * @return the opcode
     */
    public short getOpcode() {
        return opcode;
    }

    /**
     * @param opcode the opcode to set
     */
    public void setOpcode(short opcode) {
        this.opcode = opcode;
    }

    /**
     * @return the dataLen
     */
    public short getDataLen() {
        return dataLen;
    }

    /**
     * @param dataLen the dataLen to set
     */
    public void setDataLen(short dataLen) {
        this.dataLen = dataLen;
    }

    /**
     * @return the data
     */
    public int[] getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(int[] data) {
        this.data = data;
    }

    public void ReadFromBytes(byte[] messageBodyBytes) {

//        dataLen = messageBodyBytes[0];
//        int[] data = new int[dataLen / 2];
        data = new int[dataLen / 2];
        for (int i = 0; i < dataLen / 2; i++) {

            data[i] = 1 ; //无符号short
//            System.out.println(data[i]);
        }
    }

    public static void main(String[] args) {
        ModbusMsg_04 modbusMsg_04 = new ModbusMsg_04((short)10);
        modbusMsg_04.ReadFromBytes(new byte[0]);
        Logger.simple(JSON.toJSON(modbusMsg_04));
    }
}
