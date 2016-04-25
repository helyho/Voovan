/**
 * Created by helyho on 16/4/18.
 */
/**
 * 获取JVM信息
 */
function getOverView() {
    if (document.getElementById("OverView") == null) {
        return;
    }
    result = $.ajax({
        url: "JVM",
        async: false
    }).responseText;
    return $.parseJSON(result)
}

function isWindowsPath(path){
    if(path.indexOf(":") < path.indexOf(";")){
        return true;
    }else{
        return false;
    }
}

/**
 * 页面初始化
 */
$(function () {
    sysInfo = getOverView();

    //概况模块渲染
    if (sysInfo != undefined)
        var overView = new Vue({
            el: '#OverView',
            data: {
                info: [
                    {
                        "name": "﻿操作系统",
                        "value": sysInfo["os.name"] + " " + sysInfo["os.version"]
                    }, {
                        "name": "系统架构",
                        "value": sysInfo["os.arch"]
                    }, {
                        "name": "当前用户",
                        "value": sysInfo["user.name"]
                    }, {
                        "name": "系统语言",
                        "value": sysInfo["user.language"]
                    }

                ]
            }
        });

    //JVM信息模块渲染
    if (sysInfo != undefined)
        var jvmInfo = new Vue({
            el: '#JvmInfo',
            data: {
                info: [
                    {
                        "name": "JVM信息",
                        "value": sysInfo["java.runtime.name"] + " " + sysInfo["java.runtime.version"] + " " + sysInfo["java.vm.name"]
                    }, {
                        "name": "JVM版本",
                        "value": sysInfo["java.version"]
                    }, {
                        "name": "JVM厂商",
                        "value": sysInfo["java.vendor"]
                    }, {
                        "name": "厂商网址",
                        "value": sysInfo["java.vendor.url"]
                    }, {
                        "name": "JAVA_HOME",
                        "value": sysInfo["java.home"]
                    }, {
                        "name": "X86/X64",
                        "value": sysInfo["sun.arch.data.model"]
                    }
                ]
            }
        });

    //环境模块渲染
    if (sysInfo != undefined)
        var envInfo = new Vue({
            el: '#EnvInfo',
            data: {
                info: [
                    {
                        "name": "临时目录",
                        "value": sysInfo["java.io.tmpdir"]
                    }, {
                        "name": "工作目录",
                        "value": sysInfo["user.dir"]
                    }, {
                        "name": "库目录",
                        "value": ReplaceAll(sysInfo["java.library.path"], isWindowsPath(sysInfo["java.library.path"])?";":":", "<br/>")
                    }, {
                        "name": "ClassPath",
                        "value": ReplaceAll(sysInfo["java.class.path"], isWindowsPath(sysInfo["java.class.path"])?";":":", "<br/>")
                    }
                ]
            }
        });

    //渲染图表模块
    initAllChart();
    autoRefreshChart();
})


