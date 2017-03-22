package org.voovan.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.voovan.test.http.HttpClientUnit;
import org.voovan.test.http.HttpParserUnit;
import org.voovan.test.onlineComplier.ComplierUnit;
import org.voovan.test.tools.ByteBufferChannelUnit;
import org.voovan.test.tools.TByteBufferUnit;
import org.voovan.test.tools.TDateTimeUnit;
import org.voovan.test.tools.TStringUnit;
import org.voovan.test.tools.json.JSONDecodeUnit;
import org.voovan.test.tools.json.JSONEncodeUnit;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//$JUnit-BEGIN$
		suite.addTestSuite(TDateTimeUnit.class);
		suite.addTestSuite(TStringUnit.class);
		suite.addTestSuite(TByteBufferUnit.class);
		suite.addTestSuite(JSONDecodeUnit.class);
		suite.addTestSuite(JSONEncodeUnit.class);
		suite.addTestSuite(ByteBufferChannelUnit.class);
		suite.addTestSuite(HttpClientUnit.class);
		suite.addTestSuite(HttpParserUnit.class);
		suite.addTestSuite(ComplierUnit.class);



		//suite.addTestSuite(JdbcOperatorUnit.class);
		//$JUnit-END$
		return suite;
	}

}
