package org.voovan.tools.aop;

import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;
import org.voovan.tools.reflect.annotation.NotSerialization;

import java.lang.reflect.Method;

/**
 * 切面调用信息
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class InterceptInfo {
    private Class clazz;
    private String methodName;
    @NotSerialization
    private Object originObject;
    private Class[] argTypes;
    private Object[] args;
    private Class returnType;
    private Object result;
    private Exception exception;

    public InterceptInfo(Class clazz, String methodName, Object originObject, Class[] argTypes, Object[] args, Class returnType, Object result, Exception exception) {
        this.clazz = clazz;
        this.methodName = methodName;
        this.originObject = originObject;
        this.argTypes = argTypes;
        this.args = args;
        this.returnType = returnType;
        this.result = result;
        this.exception = exception;
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

    public Object process() throws Throwable{
        try {
            Method originMethod = TReflect.findMethod(originObject.getClass(), methodName+"$origin", argTypes);
            if(originMethod==null){
                throw new NoSuchMethodException("[AOP] Method \"methodName\" not found or the cut point isn't around");
            }

            return TReflect.invokeMethod(originObject, originMethod, args);
        } catch (ReflectiveOperationException e) {

            Throwable exception = e;

            do {
                exception = exception.getCause();

            } while(exception.getCause()!=null && exception instanceof ReflectiveOperationException);

            if(exception == null){
                throw e;
            } else {
                throw exception;
            }
        }
    }

}
