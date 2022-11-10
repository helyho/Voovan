package org.voovan.tools.serialize;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

/**
 * Class name
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ProtoStuffWrap {
    private Map mapObj;
    private Collection collectionObj;
    private BigDecimal bigDecimalObj;
    private BigInteger bigIntegerObj;

    public ProtoStuffWrap() {
    }

    public ProtoStuffWrap(Map mapObj) {
        this.mapObj = mapObj;
    }


    public ProtoStuffWrap(Collection collectionObj) {
        this.collectionObj = collectionObj;
    }


    public ProtoStuffWrap(BigDecimal bigDecimal) {
        this.bigDecimalObj = bigDecimal;
    }

    public ProtoStuffWrap(BigInteger bigInteger) {
        this.bigIntegerObj = bigInteger;
    }

    public Map getMapObj() {
        return mapObj;
    }

    public void setMapObj(Map mapObj) {
        this.mapObj = mapObj;
    }

    public Collection getCollectionObj() {
        return collectionObj;
    }

    public void setCollectionObj(Collection collectionObj) {
        this.collectionObj = collectionObj;
    }

    public BigDecimal getBigDecimalObj() {
        return bigDecimalObj;
    }

    public void setBigDecimalObj(BigDecimal bigDecimalObj) {
        this.bigDecimalObj = bigDecimalObj;
    }

    public BigInteger getBigInteger() {
        return bigIntegerObj;
    }

    public void setBigInteger(BigInteger bigInteger) {
        this.bigIntegerObj = bigInteger;
    }


    public void feed(Map target) {
        if(mapObj != null) {
            target.putAll(mapObj);
        }

    }

    public void feed(Collection target) {
        if(collectionObj!=null) {
            target.addAll(collectionObj);
        }
    }
}
