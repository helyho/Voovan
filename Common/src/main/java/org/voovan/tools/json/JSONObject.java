package org.voovan.tools.json;

/**
 * 标记并使用自定义的序列化方式
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public interface JSONObject<T> {
    default String toJSON() {
        return JSON.toJSON(this);
    }

    default T toObject(String json) {
        return JSON.toObject(json, this.getClass());
    }
}
