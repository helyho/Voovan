/**
 * Created by helyho on 16/4/18.
 */
/**
 * 全部替换
 * @param str
 * @param sptr
 * @param sptr1
 * @returns {*}
 * @constructor
 */
function ReplaceAll(str, sptr, sptr1) {
    while (str.indexOf(sptr) >= 0) {
        str = str.replace(sptr, sptr1);
    }
    return str;
}

/**
 * 初始化数据
 * @param arrayObj
 * @param size
 * @returns {*}
 */
function initArray(arrayObj,size){
    for(i= 0; i < size; i++){
        arrayObj.push(0);
    }
    return arrayObj;
}

/**
 * 获取对象的属性数
 * @param obj
 * @returns {number}
 */
function getObjProperityCount(obj){
    var count = 0;
    for(var x in obj){
        count++;
    }
    return count;
}

