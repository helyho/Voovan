/**
 * Created by helyho on 16/4/20.
 */
/**
 * 获取线程信息
 */
function getTheads() {
    if (document.getElementById("Threads") == null) {
        return;
    }
    result = $.ajax({
        url: "Threads",
        async: false
    }).responseText;
    return $.parseJSON(result)
}

function getTheadPool() {
    if (document.getElementById("ThreadPool") == null) {
        return;
    }
    result = $.ajax({
        url: "ThreadPool",
        async: false
    }).responseText;
    return $.parseJSON(result)
}

function refreshThread(){
    threadsInfo = getTheads();
    threads.info = threadsInfo
    threadCount.count = threadsInfo.length
}

function refreshThreadPool(){
    threadsPoolInfo = getTheadPool();
    threadPool.info = threadsPoolInfo;
}

/**
 * 页面初始化
 */
$(function () {
    threadsInfo = getTheads();
    threadsPoolInfo = getTheadPool();
    threads = new Vue({
        el: '#Threads',
        data: {
            info: threadsInfo
        },
        methods: {
            stateClass: function (state) {
                if(state=="WAITING"){
                    return "uk-alert-success";
                }else if(state=="BLOCKED"){
                    return "uk-alert-danger";
                }else if(state=="RUNNABLE"){
                    return "uk-alert";
                }else if(state=="TERMINATED"){
                    return "uk-alert";
                }else if(state=="TIMED_WAITING"){
                    return "uk-alert-warning";
                }else if(state=="NEW"){
                    return "uk-alert-alert";
                }
            },
            showStack: function(thread){
                var stackInfo = ReplaceAll(thread.StackTrace,"\r\n","<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                    "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                var content = "<b>线程信息:&nbsp;</b>\""+thread.Name+"\"" +
                    " #"+thread.Id+
                    " Priority:"+thread.Priority+
                    "<br> <hr><b>堆栈信息:&nbsp;</b>"+stackInfo;
                $("#dialogContent").html(content);
            }
        }
    });

    threadCount = new Vue({
        el:"#ThreadCount",
        data:{
            count:threadsInfo.length
        }
    })

    Vue.filter('toName', function (value) {
        if(value=="QueueSize"){
            return "线程任务队列";
        } else if(value=="TaskCount"){
            return "线程任务数";
        } else if(value=="FinishedTaskCount"){
            return "完成线程任务数";
        } else if(value=="CorePoolSize"){
            return "线程池大小";
        } else if(value=="ActiveCount"){
            return "活动线程数";
        }

    })

    threadPool = new Vue({
        el:"#ThreadPool",
        data:{
            info:threadsPoolInfo
        }
    })
})