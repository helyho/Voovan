package org.voovan.http.extend.engineio;

import org.voovan.tools.TString;
import org.voovan.tools.json.JSON;

import java.util.ArrayList;
import java.util.List;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Config {
    private String sid;
    private List<String> upgrades;
    private int pingInterval;
    private int pingTimeout;

    public Config(){
        this.sid = TString.generateShortUUID();
        this.upgrades = new ArrayList<String>();
        upgrades.add("websocket");
        this.pingInterval = 25000;
        this.pingTimeout = 60000;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public List<String> getUpgrades() {
        return upgrades;
    }

    public void setUpgrades(List<String> upgrades) {
        this.upgrades = upgrades;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public int getPingTimeout() {
        return pingTimeout;
    }

    public void setPingTimeout(int pingTimeout) {
        this.pingTimeout = pingTimeout;
    }

    public String toString(){
        return JSON.toJSON(this);
    }
}
