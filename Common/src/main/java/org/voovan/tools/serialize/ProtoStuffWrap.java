package org.voovan.tools.serialize;

import java.util.Collection;
import java.util.Map;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class ProtoStuffWrap {
    private Map mapObj;
    private Collection collectionObj;

    public ProtoStuffWrap() {
    }

    public ProtoStuffWrap(Map mapObj, Collection collectionObj) {
        this.mapObj = mapObj;
        this.collectionObj = collectionObj;
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

    public void feed(Map target) {
        target.putAll(mapObj);
    }

    public void feed(Collection target) {
        target.addAll(collectionObj);
    }
}
