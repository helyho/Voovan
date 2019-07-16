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
    private Float valueF;
    private Double valueD;

    public SimpleObject() {
    }

    public SimpleObject(Integer stringx) {
        this.valueI = stringx;
    }

    public SimpleObject(String valueS) {
        this.valueS = valueS;
    }

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

    public Float getValueF() {
        return valueF;
    }

    public void setValueF(Float valueF) {
        this.valueF = valueF;
    }

    public Double getValueD() {
        return valueD;
    }

    public void setValueD(Double valueD) {
        this.valueD = valueD;
    }

    public String getData(String aa, Integer bb){
        return  System.currentTimeMillis() + " " + aa + " " + bb;
    }
}
