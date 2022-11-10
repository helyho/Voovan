package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.json.JSON;
import org.voovan.tools.serialize.ProtoStuffSerialize;
import org.voovan.tools.tuple.Tuple;

/**
 * Class name
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class TupleUnit extends TestCase {

    public void testTuple() {
//        Tuple tuple = new Tuple();
//        tuple.addField("string", String.class);
//        tuple.addField("number", Integer.class);
//
//        tuple.set("123123");
//        tuple.set("456456");

        Tuple tuple = Tuple.withName("1", "1111", "2", 2222, "I3", 3333);
        tuple.set(789).set(567);
        System.out.println("-->" + tuple.get(2));
//        System.out.println("-->" + tuple.get(1));

        System.out.println(JSON.toJSONWithFormat(tuple));

        System.out.println("-->" + tuple.toList());
        System.out.println("-->" + tuple.toMap());

        ProtoStuffSerialize protoStuffSerialize = new ProtoStuffSerialize();
        byte[] xx = protoStuffSerialize.serialize(tuple);
        Object obj = protoStuffSerialize.unserialize(xx);
        System.out.println(JSON.toJSONWithFormat(obj));
    }
}
