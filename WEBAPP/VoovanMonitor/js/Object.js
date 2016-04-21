/**
 * Created by helyho on 16/4/18.
 */
/**
 * 获取对象信息
 */
function getObjects() {
    //获取页面元素的参数
    if(document.getElementById("Objects")==null){
        return;
    }
    regex = $("#ObjectSearchWord").val();

    if(regex==null || regex==undefined || regex==""){
        regex = "^((?!java|sun|\\[).)*$";
    }

    result = $.ajax({
        url: "Objects/"+encodeURIComponent(regex),
        async: false
    }).responseText;
    return $.parseJSON(result)
}

/**
 * 页面初始化
 */
$(function () {
    objects = getObjects();
    objCount = getObjProperityCount(objects);

    var objectsInfo = new Vue({
        el: '#Objects',
        data: {
            info: objects
        }
    });

    var objectCount = new Vue({
        el: '#ObjectCount',
        data: {
            count: objCount
        }
    });

    $("#ObjectSearch").on("click",function(){
        objects = getObjects();
        objectsInfo.info = objects;
        objCount = getObjProperityCount(objects);
        objectCount.count = objCount;
    })
})


