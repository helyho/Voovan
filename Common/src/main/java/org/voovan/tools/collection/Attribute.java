package org.voovan.tools.collection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类文字命名
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Attribute {

    private Map<Object, Object> attributes;

    public Attribute() {
        attributes = new ConcurrentHashMap<Object, Object>();
    }


    /**
     * 获取全部会话参数
     * @return 会话参数Map
     */
    public Map<Object,Object> attributes(){
        return this.attributes;
    }

    /**
     * 获取会话参数
     * @param key 参数名
     * @return    参数对象
     */
    public Object getAttribute(Object key) {
        return attributes.get(key);
    }

    /**
     * 设置会话参数
     * @param key     参数名
     * @param value   参数对象
     */
    public void setAttribute(Object key, Object value) {
        this.attributes.put(key, value);
    }

    /**
     * 移除会话参数
     * @param key     参数名
     */
    public void removeAttribute(Object key) {
        this.attributes.remove(key);
    }

    /**
     * 检查会话参数是否存在
     * @param key     参数名
     * @return 是否包含
     */
    public boolean containAttribute(Object key) {
        return this.attributes.containsKey(key);
    }

    /**
     * 清空 session 的缓存配置
     */
    public void clearAttribute() {
        attributes.clear();
    }

}
