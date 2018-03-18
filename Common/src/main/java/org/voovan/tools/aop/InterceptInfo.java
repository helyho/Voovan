package org.voovan.tools.aop;

/**
 * 切面调用信息
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class InterceptInfo {
    private String className;
    private String methodName;
    private Object[] args;
    private Object result;

    public InterceptInfo(String className, String methodName, Object[] args, Object result) {
        this.className = className;
        this.methodName = methodName;
        this.args = args;
        this.result = result;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

}
