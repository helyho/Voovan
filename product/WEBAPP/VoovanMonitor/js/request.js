/**
 * Created by helyho on 16/4/21.
 */
/**
 * 获取对象信息
 */
function getRequests() {
    //获取页面元素的参数
    if(document.getElementById("RequestInfo")==null){
        return;
    }

    result = $.ajax({
        url: "RequestInfo",
        async: false
    }).responseText;
    return $.parseJSON(result)
}

function refreshRequestInfo(){
    requestInfos = getRequests();
    requestInfoElement.requests = requestInfos;
    requestCountElement.count = requestInfos.length;
}

$(function(){
    requestInfos = getRequests();
    requestInfoElement = new Vue({
        el: "#RequestInfo",
        data: {
            requests: requestInfos
        }
    });

    requestCountElement = new Vue({
        el: "#RequestCount",
        data: {
            count: requestInfos.length
        }
    });
})