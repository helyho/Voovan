package org.voovan.test.tools.json;

import junit.framework.TestCase;
import org.voovan.tools.json.JSON;
import org.voovan.tools.json.YAML2JSON;
import org.voovan.tools.log.Logger;

public class YAMLJSONDecodeUnit extends TestCase {

	public YAMLJSONDecodeUnit(String name) {
		super(name);
	}

	@SuppressWarnings("rawtypes")
	public void testObject() throws Exception{
		String jsonString =
				"jedis :\n" +
						"  pool :\n" +
						"    host : 127.0.0.1\n" +
						"    port : 6379\n" +
						"    config :\n" +
						"      maxTotal: 100\n" +
						"      maxIdle: 10\n" +
						"      maxWaitMillis : 100000\n" +
						"server :\n" +
						"  port : 8080\n" +
						"  alias:\n" +
						"     - aaaa\n" +
						"     - bbbb\n" +
						"     - cccc \n" +
						"aaaa: > \n" +
						"  aaaaaa \n" +
						"  bbbbbb \n" +
						"  cccccc \n" +
						"  \n" +
						"  \n" +
						"  \n" +
						"  \n" +
						"jedis2 :\n" +
						"  pool :\n" +
						"    host : 127.0.0.1\n" +
						"    port : 6379\n" +
						"    config :\n" +
						"      maxTotal: 100\n" +
						"      maxIdle: 10\n" +
						"      maxWaitMillis : 100000\n" +
						"server :\n" +
						"  port :  8080\n" +
						"  alias :\n" +
						"     - aaaa\n" +
						"     - bbbb\n" +
						"     - cccc \n" +
						"  oooo: | \n" +
						"    aaaaaa \n" +
						"    bbbbbb \n" +
						"    cccccc \n" +
						"    \n" +
						"    \n" +
						"    \n" +
						"    \n"+
						"dddd: |+\n" +
						"  aaaaaa \n" +
						"  bbbbbb \n" +
						"  #adfasdfasdf\n"+
						"  kkkkkk \n" +
						"  \n" +
						"  \n" +
						"  \n" +
						"  \n"+
						"eeeee : |-\n" +
						"  aaaaaa \n" +
						"  bbbbbb \n" +
						"  cccccc \n" +
						"  \n" +
						"  \n" +
						"  \n" +
						"  \n";

		Logger.simple(jsonString);
		Logger.simple(JSON.formatJson(YAML2JSON.convert(jsonString)));
	}

	public void testObjectArrayFirst() throws Exception{
		String jsonString =
				"aaaa: \n" +
						"  - aaaa\n" +
						"  #adfasdfasdf\n"+
						"  - bbbb\n" +
						"  - cccc\n" +
						"  - \n" +
						"    attr1: 1111 \n" +
						"    attr2: 2222 \n" +
						"jedis :\n" +
						"  pool :\n" +
						"    host : 127.0.0.1\n" +
						"    port : 6379\n" +
						"    #adfasdfasdf\n"+
						"    config :\n" +
						"      maxTotal: 100\n" +
						"      maxIdle: 10\n" +
						"      maxWaitMillis : 100000\n" +
						"server :\n" +
						"  port :  8080\n" +
						"  alias: 1234\n" +
						"aaaa: |+ \n" +
						"  aaaaaa\n" +
						"  bbbbbb\n" +
						"  cccccc\n" +
						"  \n" +
						"  \n" +
						"  \n" +
						"  \n";;
		Logger.simple(jsonString);
		Logger.simple(JSON.formatJson(YAML2JSON.convert(jsonString)));
	}

	public void testArray() throws Exception{
		String jsonString =
				"- aaaa\n" +
						"- bbbb\n" +
						"- cccc \n" +
						"-  \n" +
						"  attr1: 1111 \n" +
						"  attr2: 2222 \n";
		Logger.simple(jsonString);
		Logger.simple(JSON.formatJson(YAML2JSON.convert(jsonString)));

	}


	public void testMulitLine() throws Exception{
		String jsonString =
				"aaaa: |- \n" +
						"  aaaaaa\n" +
						"  bbbbbb\n" +
						"  cccccc\n" +
						"  \n" +
						"  \n" +
						"  \n" +
						"  \n";
		Logger.simple(jsonString);
		Logger.simple(YAML2JSON.convert(jsonString));

	}
}