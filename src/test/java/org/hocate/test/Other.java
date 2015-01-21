package org.hocate.test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.hocate.log.Logger;

public class Other {
	public static void main(String[] args) throws Exception {
		Integer s= 1025;
		Logger.simple(s.byteValue());
		ByteBuffer byteBuffer = ByteBuffer.allocate(0);
		Logger.simple(byteBuffer.hasRemaining());
		
		
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
		byteOutputStream.write("bingo".getBytes());
		Logger.info(byteOutputStream.toByteArray().length);
		
		Logger.simple(ClassLoader.getSystemClassLoader().getClass().getName());
		
		Logger.simple(System.getProperty("user.dir"));
		String regex = ":[^/]+";
		Logger.simple("/test/:username_a/:id".replaceAll(regex, "[^/?]+"));
		
		Class.forName("com.mysql.jdbc.Driver");
		Connection conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1/buz", "buz", "99320866");
		conn.setAutoCommit(false);
		
		Statement statement1 = (Statement) conn.createStatement();
		int rows = statement1.executeUpdate("update sc_script set version=0 ");
		Logger.simple(rows);
		
		Statement statement = (Statement) conn.createStatement();
		ResultSet rs = statement.executeQuery("select version from sc_script");
		while(rs.next()){
			Logger.simple("Version: "+rs.getString("version"));
		}
		
		conn.rollback();
		conn.close();
	}
}
