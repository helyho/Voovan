package org.voovan.tools.tuple;

/**
 * Class name
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TupleItem {
    private Object name;
    private Class clazz;
    private Object value;

    public TupleItem(Object name, Class clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public TupleItem(Object name, Class clazz, Object value) {
        this.name = name;
        this.clazz = clazz;
        this.value = value;
    }

    public Object getName() {
        return name;
    }

    public void setName(Object name) {
        this.name = name;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "TupleItem{" +
                "name='" + name + '\'' +
                ", clazz=" + clazz +
                ", value=" + value +
                '}';
    }
}
