/**
 * Created by helyho on 16/4/20.
 */

var cpuChartElement;
var cpuOption;
var memChartElement;
var memOption;
var heapMemChartElement;
var heapMemOption;
var noHeapMemChartElement;
var noHeapMemOption;
var threadChartElement;
var threadOption;
var objectChartElement;
var objectOption;

//初始化所有的 Chart
function initAllChart() {
    initCPUChart();
    initMemChart();
    initHeapMemChart();
    initNoHeapMemChart();
    initThreadChart();
    initObjectChart();
}

//定时渲染所有的 Chart
function autoRefreshChart() {
// 使用刚指定的配置项和数据显示图表。
    setInterval(function () {
        refreshCPUChart();
        refreshMemChart();
        refreshThreadChart();
        refreshObjectChart();
    }, 1000);
}

/**
 *  CPU负载图标函数
 */
function initCPUChart() {
    if (document.getElementById('CPUChart') == null) {
        return;
    }
    // 基于准备好的dom，初始化echarts实例
    cpuChartElement = echarts.init(document.getElementById('CPUChart'));

    // 指定图表的配置项和数据
    cpuOption = {
        title: {
            text: 'CPU负载'
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: ['负载(秒)']
        },
        toolbox: {},
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: ["1秒", "2秒", "3秒", "4秒", "5秒", "6秒", "7秒", "8秒", "9秒", "10秒",
                "11秒", "12秒", "13秒", "14秒", "15秒", "16秒", "17秒", "18秒", "19秒", "20秒",
                "21秒", "22秒", "23秒", "24秒", "25秒", "26秒", "27秒", "28秒", "29秒", "30秒"]

        },
        yAxis: {
            type: 'value'
        },
        series: [{
            name: '负载(秒)',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }]
    };

    var dataSeries = cpuOption.series;
    var dataLength = cpuOption.xAxis.data.length;

    //初始化图表
    for (var i = 0; i < dataSeries.length; i++) {
        dataSeries[i].data = initArray(dataSeries[i].data, dataLength);
    }

    cpuChartElement.setOption(cpuOption);
}

function refreshCPUChart() {
    var dataSeries = cpuOption.series;
    var dataLength = cpuOption.xAxis.data.length;

    $.ajax({
        url: "CPU",
        success: function (response) {
            //动态属性图表数据
            result = $.parseJSON(response);
            dataSeries[0].data.push(result.SystemLoadAverage.toFixed(2))
            if (dataSeries[0].data.length > cpuOption.xAxis.data.length) {
                dataSeries[0].data.shift()
            }

        }
    })
    cpuChartElement.setOption(cpuOption);
}

/**
 * 内存负载图表函数
 */
function initMemChart() {
    if (document.getElementById('MemChart') == null) {
        return;
    }

    // 基于准备好的dom，初始化echarts实例
    memChartElement = echarts.init(document.getElementById('MemChart'));

    // 指定图表的配置项和数据
    memOption = {
        title: {
            text: 'JVM内存负载'
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: ['Total', 'Max', 'Usage']
        },
        toolbox: {},
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: ["1秒", "2秒", "3秒", "4秒", "5秒", "6秒", "7秒", "8秒", "9秒", "10秒",
                "11秒", "12秒", "13秒", "14秒", "15秒", "16秒", "17秒", "18秒", "19秒", "20秒",
                "21秒", "22秒", "23秒", "24秒", "25秒", "26秒", "27秒", "28秒", "29秒", "30秒"]
        },
        yAxis: {
            type: 'value'
        },
        series: [{
            name: 'Max',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }, {
            name: 'Total',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }, {
            name: 'Usage',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }]
    };

    var dataSeries = memOption.series;
    var dataLength = memOption.xAxis.data.length;
    //初始化图表
    for (var i = 0; i < dataSeries.length; i++) {
        dataSeries[i].data = initArray(dataSeries[i].data, dataLength);
    }

    memChartElement.setOption(memOption);
}


/**
 * 堆内存负载图表函数
 */
function initHeapMemChart() {
    if (document.getElementById('HeapMemChart') == null) {
        return;
    }

    // 基于准备好的dom，初始化echarts实例
    heapMemChartElement = echarts.init(document.getElementById('HeapMemChart'));

    // 指定图表的配置项和数据
    heapMemOption = {
        title: {
            text: '堆内存负载'
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: ['Init', 'Max', 'Usage', 'Commit']
        },
        toolbox: {},
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: ["1秒", "2秒", "3秒", "4秒", "5秒", "6秒", "7秒", "8秒", "9秒", "10秒",
                "11秒", "12秒", "13秒", "14秒", "15秒", "16秒", "17秒", "18秒", "19秒", "20秒",
                "21秒", "22秒", "23秒", "24秒", "25秒", "26秒", "27秒", "28秒", "29秒", "30秒"]
        },
        yAxis: {
            type: 'value'
        },
        series: [{
            name: 'Max',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }, {
            name: 'Commit',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }, {
            name: 'Init',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }, {
            name: 'Usage',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }]
    };

    var dataSeries = heapMemOption.series;
    var dataLength = heapMemOption.xAxis.data.length;
    //初始化图表
    for (var i = 0; i < dataSeries.length; i++) {
        dataSeries[i].data = initArray(dataSeries[i].data, dataLength);
    }

    heapMemChartElement.setOption(heapMemOption);
}

/**
 * 堆内存负载图表函数
 */
function initNoHeapMemChart() {
    if (document.getElementById('HeapMemChart') == null) {
        return;
    }

    // 基于准备好的dom，初始化echarts实例
    noHeapMemChartElement = echarts.init(document.getElementById('NoHeapMemChart'));

    // 指定图表的配置项和数据
    noHeapMemOption = {
        title: {
            text: '非堆内存负载'
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: ['Init', 'Usage', 'Commit']
        },
        toolbox: {},
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: ["1秒", "2秒", "3秒", "4秒", "5秒", "6秒", "7秒", "8秒", "9秒", "10秒",
                "11秒", "12秒", "13秒", "14秒", "15秒", "16秒", "17秒", "18秒", "19秒", "20秒",
                "21秒", "22秒", "23秒", "24秒", "25秒", "26秒", "27秒", "28秒", "29秒", "30秒"]
        },
        yAxis: {
            type: 'value'
        },
        series: [{
            name: 'Commit',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }, {
            name: 'Usage',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }, {
            name: 'Init',
            type: 'line',
            smooth: true,
            symbol: 'none',
            areaStyle: {normal: {}},
            data: []
        }]
    };

    var dataSeries = noHeapMemOption.series;
    var dataLength = noHeapMemOption.xAxis.data.length;
    //初始化图表
    for (var i = 0; i < dataSeries.length; i++) {
        dataSeries[i].data = initArray(dataSeries[i].data, dataLength);
    }

    noHeapMemChartElement.setOption(noHeapMemOption);
}

function refreshMemChart() {
    if (memOption != undefined) {
        var memDataSeries = memOption.series;
        var memDataLength = memOption.xAxis.data.length;
    }
    if (heapMemOption != undefined) {
        var heapMemDataSeries = heapMemOption.series;
        var heapMemDataLength = heapMemOption.xAxis.data.length;
    }
    if (noHeapMemOption != undefined) {
        var noHeapMemDataSeries = noHeapMemOption.series;
        var noHeapMemDataLength = noHeapMemOption.xAxis.data.length;
    }
    $.ajax({
        url: "Memory",
        success: function (response) {
            //动态属性图表数据
            result = $.parseJSON(response);
            //渲染 JVM 内存信息
            if (memOption != undefined) {
                memDataSeries[0].data.push((result.max / 1024 / 1024).toFixed(2));
                memDataSeries[1].data.push((result.total / 1024 / 1024).toFixed(2));
                memDataSeries[2].data.push(((result.total - result.free) / 1024 / 1024).toFixed(2));
                if (memDataSeries[0].data.length > memDataLength) {
                    memDataSeries[0].data.shift()
                    memDataSeries[1].data.shift()
                    memDataSeries[2].data.shift()
                }
                memChartElement.setOption(memOption);
            }

            //渲染heap内存信息
            if (heapMemOption != undefined) {
                heapMemDataSeries[0].data.push((result.heapMax / 1024 / 1024).toFixed(2));
                heapMemDataSeries[1].data.push((result.heapCommit / 1024 / 1024).toFixed(2));
                heapMemDataSeries[2].data.push((result.heapInit / 1024 / 1024).toFixed(2));
                heapMemDataSeries[3].data.push((result.heapUsage / 1024 / 1024).toFixed(2));
                if (heapMemDataSeries[0].data.length > heapMemDataLength) {
                    heapMemDataSeries[0].data.shift()
                    heapMemDataSeries[1].data.shift()
                    heapMemDataSeries[2].data.shift()
                    heapMemDataSeries[3].data.shift()
                }
                heapMemChartElement.setOption(heapMemOption);
            }

            //渲染noheap内存信息
            if (noHeapMemOption != undefined) {
                noHeapMemDataSeries[0].data.push((result.noHeapCommit / 1024 / 1024).toFixed(2));
                noHeapMemDataSeries[1].data.push((result.noHeapUsage / 1024 / 1024).toFixed(2));
                noHeapMemDataSeries[2].data.push((result.noHeapInit / 1024 / 1024).toFixed(2));
                if (noHeapMemDataSeries[0].data.length > noHeapMemDataLength) {
                    noHeapMemDataSeries[0].data.shift()
                    noHeapMemDataSeries[1].data.shift()
                    noHeapMemDataSeries[2].data.shift()
                }
                noHeapMemChartElement.setOption(noHeapMemOption);
            }
        }
    })
}


/**
 *  线程负载图表函数
 */
function initThreadChart() {
    if (document.getElementById('ThreadChart') == null) {
        return;
    }
    // 基于准备好的dom，初始化echarts实例
    threadChartElement = echarts.init(document.getElementById('ThreadChart'));

    // 指定图表的配置项和数据
    threadOption = {
        title: {
            text: '线程数量'
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: ['数量(秒)']
        },
        toolbox: {},
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: ["1秒", "2秒", "3秒", "4秒", "5秒", "6秒", "7秒", "8秒", "9秒", "10秒",
                "11秒", "12秒", "13秒", "14秒", "15秒", "16秒", "17秒", "18秒", "19秒", "20秒",
                "21秒", "22秒", "23秒", "24秒", "25秒", "26秒", "27秒", "28秒", "29秒", "30秒"]

        },
        yAxis: {
            type: 'value'
        },
        series: [{
            name: '数量(秒)',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineStyle: {
                normal: {
                    color: '#3366cc'
                }
            },
            areaStyle: {
                normal: {
                    color: 'rgba(0, 0, 255, 0.7)'
                }
            },
            data: []
        }]
    };

    var dataSeries = threadOption.series;
    var dataLength = threadOption.xAxis.data.length;

    //初始化图表
    for (var i = 0; i < dataSeries.length; i++) {
        dataSeries[i].data = initArray(dataSeries[i].data, dataLength);
    }

    threadChartElement.setOption(threadOption);
}


function refreshThreadChart() {
    var dataSeries = threadOption.series;
    var dataLength = threadOption.xAxis.data.length;

    $.ajax({
        url: "ThreadCount",
        success: function (response) {
            //动态属性图表数据
            result = response;
            dataSeries[0].data.push(result)
            if (dataSeries[0].data.length > threadOption.xAxis.data.length) {
                dataSeries[0].data.shift()
            }

        }
    })
    threadChartElement.setOption(threadOption);
}


/**
 *  线程负载图表函数
 */
function initObjectChart() {
    if (document.getElementById('ObjectChart') == null) {
        return;
    }
    // 基于准备好的dom，初始化echarts实例
    objectChartElement = echarts.init(document.getElementById('ObjectChart'));

    // 指定图表的配置项和数据
    objectOption = {
        title: {
            text: '对象数量'
        },
        tooltip: {
            trigger: 'axis'
        },
        legend: {
            data: ['数量(秒)']
        },
        toolbox: {},
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: ["1秒", "2秒", "3秒", "4秒", "5秒", "6秒", "7秒", "8秒", "9秒", "10秒",
                "11秒", "12秒", "13秒", "14秒", "15秒", "16秒", "17秒", "18秒", "19秒", "20秒",
                "21秒", "22秒", "23秒", "24秒", "25秒", "26秒", "27秒", "28秒", "29秒", "30秒"]

        },
        yAxis: {
            type: 'value'
        },
        series: [{
            name: '数量(秒)',
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineStyle: {
                normal: {
                    color: '#669900'
                }
            },
            areaStyle: {
                normal: {
                    color: 'rgba(0, 255, 0, 0.7)'
                }
            },
            data: []
        }]
    };

    var dataSeries = objectOption.series;
    var dataLength = objectOption.xAxis.data.length;

    //初始化图表
    for (var i = 0; i < dataSeries.length; i++) {
        dataSeries[i].data = initArray(dataSeries[i].data, dataLength);
    }

    objectChartElement.setOption(objectOption);
}


function refreshObjectChart() {
    var dataSeries = objectOption.series;
    var dataLength = objectOption.xAxis.data.length;

    $.ajax({
        url: "ObjectCount",
        success: function (response) {
            //动态属性图表数据
            result = response;
            dataSeries[0].data.push(result)
            if (dataSeries[0].data.length > objectOption.xAxis.data.length) {
                dataSeries[0].data.shift()
            }

        }
    })
    objectChartElement.setOption(objectOption);
}


