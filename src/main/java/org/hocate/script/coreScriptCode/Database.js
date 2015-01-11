/**
 * JavaScript 进行数据库访问的公共类
 */
importClass(org.hocate.biz.config.ConfigLoader)
importClass(org.hocate.db.JdbcOperate)

/**
 * 数据库操作类
 */
function Database(){
	var config = new ConfigLoader();
	var dbOper = new JdbcOperate(config.getDataSource());
	
	/**
	 * 准备 SQL 参数
	 */
	function praperArgs(args){
		var argsStr = "";
		for(i=0;i<args.length;i++){
			argsStr = argsStr+"arguments["+i+"],"
		}
		argsStr = argsStr.substr(0,argsStr.length-1);
		print(argsStr)
		return argsStr;
	}
	
	/**
	 * 调用查询,返回结果集 List<Map>的形式
	 * 参数 sql,param_1...param_n
	 */
	this.query = function(){
		var argsStr = praperArgs(arguments);
		return eval("dbOper.queryMapList("+argsStr+")")
	}
	
	/**
	 * 调用更新,返回被影响的行数目
	 * 参数 sql,param_1...param_n
	 */
	this.update = function(){
		var argsStr = praperArgs(arguments);
		return eval("dbOper.update("+argsStr+")")
	}
	
	
}