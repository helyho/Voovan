package org.voovan.test.tools;

/**
 * 类文字命名
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class SimpleObject {
    private String valueS;
    private Integer valueI;

    public String getValueS() {
        return valueS;
    }

    public void setValueS(String valueS) {
        this.valueS = valueS;
    }

    public Integer getValueI() {
        return valueI;
    }

    public void setValueI(Integer valueI) {
        this.valueI = valueI;
    }

    public String getData(String aa, Integer bb){
        return  System.currentTimeMillis() + " " + aa + " " + bb;
    }
}
