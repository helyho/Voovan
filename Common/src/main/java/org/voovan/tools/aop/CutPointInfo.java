package org.voovan.tools.aop;

import javassist.CtMethod;
import org.voovan.tools.TString;

/**
 * 切面描述信息
 *
 * @author: helyho
 * DBase Framework.
 * WebSite: https://github.com/helyho/DBase
 * Licence: Apache v2 License
 */
public class CutPointInfo {
    private String resultType;
    private String clazzName;
    private String methodName;
    private String[] parameterTypes = new String[0];
    private Integer type;
    private CtMethod cutPointMethod; //带有切点注解的切面方法类
    private boolean interceptLambda; //是否拦截 lambda 表达式

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public String getClazzName() {
        return clazzName;
    }

    public void setClazzName(String clazzName) {
        this.clazzName = clazzName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(String[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public CtMethod getCutPointMethod() {
        return cutPointMethod;
    }

    public void setCutPointMethod(CtMethod cutPointMethod) {
        this.cutPointMethod = cutPointMethod;
    }

    public boolean isInterceptLambda() {
        return interceptLambda;
    }

    public void setInterceptLambda(boolean interceptLambda) {
        this.interceptLambda = interceptLambda;
    }

    public static CutPointInfo parse(String cutPointDesc){
        CutPointInfo cutPointInfo = new CutPointInfo();

        String[] descInfo = cutPointDesc.split("\\@");

        String[] resultAndClass = descInfo[0].split(" +");
        cutPointInfo.setResultType(resultAndClass[0]);
        cutPointInfo.setClazzName(resultAndClass[1]);


        if(descInfo.length > 1) {

            descInfo[1] = TString.removeSuffix(descInfo[1]);
            String[] classAndMethodInfo = descInfo[1].split("\\(");
            cutPointInfo.setMethodName(classAndMethodInfo[0]);

            if (classAndMethodInfo.length == 2) {
                classAndMethodInfo[1] = classAndMethodInfo[1].replaceAll(" ", "");
                cutPointInfo.setParameterTypes(classAndMethodInfo[1].split(","));
            }
        }

        return cutPointInfo;
    }
}
