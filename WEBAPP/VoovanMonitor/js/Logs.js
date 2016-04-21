/**
 * Created by helyho on 16/4/21.
 */

function getLogs(type, lineNumber)
{
    result = $.ajax({
        url: "Log/" + type + "/" + lineNumber,
        async: false
    }).responseText;
    return ReplaceAll(ReplaceAll(result,"\r\n","<br/>")," ","&nbsp;");
}

function refreshLog(type){
    if(type=="SYSOUT") {
        sysoutLogElement.$data.content = getLogs("SYSOUT", $("#LineNumber").val())
    }
    if(type=="ACCESS") {
        accessLogElement.$data.content = getLogs("ACCESS", $("#LineNumber").val())
    }
}

$(function () {
    sysoutLogElement = new Vue({
        el: "#SysoutLog",
        data: {
            content: getLogs("SYSOUT", 100)
            }
        })

    accessLogElement = new Vue({
        el: "#AccessLog",
        data: {
            content: getLogs("ACCESS", 100)
            }
        })

})