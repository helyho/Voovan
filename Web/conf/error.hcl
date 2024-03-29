{
  /*
	 * All exception on router will be try match error config here
	 * properties not must be give value, it some property not define, it whill be use default value.
	 * default value of property:
	 * StatusCode  : 500
	 * Page        : Error.html
	 * Description : the exception's stack information
	 */

  //available static file not found
  "org.voovan.http.server.exception.ResourceNotFound" : {
    "Mime" : "text/html",
    "StatusCode" : 404,
    "Page" : "Error.html",
    "Description" : "The request file is not found."
  },
  //available router not found
  "org.voovan.http.server.exception.RouterNotFound" : {
    "Mime" : "application/json",
    "StatusCode" : 404,
    "Page" : "Error.json",
    "Description" : "None avaliable router to use."
  },
  //undefined error
  "Other" : {
    "Mime" : "text/html",
    "StatusCode" : 500,
    "Page" : "Error.json"
  }
}