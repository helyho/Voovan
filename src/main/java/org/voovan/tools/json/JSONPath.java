package org.voovan.tools.json;

import org.voovan.tools.TObject;
import org.voovan.tools.TReflect;
import org.voovan.tools.TString;
import org.voovan.tools.log.Logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 类文字命名
 *
 * @author helyho
 *         <p>
 *         Voovan Framework.
 *         WebSite: https://github.com/helyho/Voovan
 *         Licence: Apache v2 License
 */
public class JSONPath {

    private Object parsedObj;

    public JSONPath(String jsonStr) {
        Object result = JSON.parse(jsonStr);
        if (result instanceof List) {
            parsedObj = TObject.cast(result);
        } else if (result instanceof Map) {
            parsedObj = TObject.cast(result);
        }
    }

    public Object pathValue(String pathQry) {
        Object currentPathObject = parsedObj;
        String[] pathElems = pathQry.split("/");
        ArrayList result = new ArrayList();

        try{
            for(String pathElem : pathElems){
                pathElem = pathElem.trim();

                if(pathElem.isEmpty()){
                    continue;
                }

                String[] listMarks = TString.searchByRegex(pathElem,"\\[\\d+\\]$");
                int listIndex = -1;
                if(listMarks.length>0){
                    listIndex = Integer.parseInt( TString.removeSuffix( TString.removePrefix(listMarks[0]) ) );
                }

                if(!pathElem.startsWith("root")){
                    Method mapGetMethod = TReflect.findMethod(HashMap.class,"get",new Class[]{Object.class});
                    currentPathObject = TReflect.invokeMethod(currentPathObject, mapGetMethod, (Object)(listIndex==-1?pathElem:pathElem.replace("["+listIndex+"]","")) );
                }

                if(listIndex!=-1) {
                    Method listGetMethod = TReflect.findMethod(ArrayList.class,"get",new Class[]{int.class});
                    currentPathObject = TReflect.invokeMethod(currentPathObject, listGetMethod, listIndex);
                }
            }

            return currentPathObject;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
