package org.voovan.tools.collection;

import org.voovan.tools.reflect.annotation.NotSerialization;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 属性存储器
 *
 * @author: helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class Attributes {

    private Map<Object, Object> attr;

    @NotSerialization
    private boolean modifyed = false;

    public Attributes() {
        attr = new ConcurrentHashMap<Object, Object>();
    }

    public boolean isModifyed() {
        return modifyed;
    }

    public void setModifyed(boolean modifyed) {
        this.modifyed = modifyed;
    }

    /**
     * 获取全部属性参数
     * @return 属性参数Map
     */
    public Map<Object,Object> attributes(){
        return this.attr;
    }

    /**
     * 获取属性参数
     * @param key 参数名
     * @return    参数对象
     */
    public Object getAttribute(Object key) {
        return attr.get(key);
    }

    /**
     * 设置属性参数
     * @param key     参数名
     * @param value   参数对象
     */
    public void setAttribute(Object key, Object value) {
        this.attr.put(key, value);
        modifyed = true;
    }

    /**
     * 移除属性参数
     * @param key     参数名
     */
    public void removeAttribute(Object key) {
        this.attr.remove(key);
        modifyed = true;
    }

    /**
     * 检查属性参数是否存在
     * @param key     参数名
     * @return 是否包含
     */
    public boolean containAttribute(Object key) {
        return this.attr.containsKey(key);
    }

    /**
     * 清空 session 的缓存配置
     */
    public void clearAttribute() {
        attr.clear();
        modifyed = true;
    }

}
