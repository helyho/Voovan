package org.voovan.tools;

import junit.framework.TestCase;
import java.util.HashMap;
import java.util.Map;


//import static jdk.jfr.internal.jfc.model.Constraint.any;
import static org.voovan.tools.TSQL.setPreparedParams;

public class TSQLTest extends TestCase {

    public void testRemoveEmptyCondiction() {
        String sqlText = "SELECT * FROM users WHERE name = ::name AND age > ::age";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", "John Doe");
        params.put("age", 25);
        String expectedSqlText = "SELECT * FROM users WHERE name = ::name AND age > ::age";
        String actualSqlText = TSQL.removeEmptyCondiction(sqlText, params);
        assertEquals(expectedSqlText, actualSqlText);

        sqlText = "SELECT * FROM users WHERE name = ::name AND age > ::age";
        params.clear();
        expectedSqlText = "SELECT * FROM users WHERE age > ::age";
        actualSqlText = TSQL.removeEmptyCondiction(sqlText, params);
        assertEquals(expectedSqlText, actualSqlText);

        sqlText = "SELECT * FROM users WHERE name = ::name AND age in ::ages";
        params.clear();
        params.put("name", "John Doe");
        params.put("ages", new int[]{25, 30, 35});
        expectedSqlText = "SELECT * FROM users WHERE name = ::name AND age in ::ages";
        actualSqlText = TSQL.removeEmptyCondiction(sqlText, params);
        assertEquals(expectedSqlText, actualSqlText);

        sqlText = "SELECT * FROM users WHERE name = ::name AND age in ::ages";
        params.clear();
        params.put("name", "John Doe");
        params.put("ages", new int[]{});
        expectedSqlText = "SELECT * FROM users WHERE name = ::name";
        actualSqlText = TSQL.removeEmptyCondiction(sqlText, params);
        assertEquals(expectedSqlText, actualSqlText);
    }

}