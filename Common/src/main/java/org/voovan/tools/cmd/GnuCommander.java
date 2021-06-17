package org.voovan.tools.cmd;

import org.voovan.tools.TString;
import org.voovan.tools.cmd.annotation.Command;
import org.voovan.tools.cmd.annotation.Option;
import org.voovan.tools.collection.MultiMap;
import org.voovan.tools.json.JSON;
import org.voovan.tools.log.Logger;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.*;

/**
 * GNU 控制台参数解析
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class GnuCommander<T> {
    private Class<T> optionClass;
    private Field[] fields;

    public GnuCommander(Class<T> optionClass) {
        this.optionClass = optionClass;
        this.fields = TReflect.getFields(optionClass);
        for(Field field : fields) {
            String fieldName = field.getName();
            Option option = field.getAnnotation(Option.class);
            if(option.name().length() > 1) {
                throw new RuntimeException("arg '" + getOptionReadableName(option) + "' is short name, it must be one character");
            }
        }
    }

    public T parser(String[] args) throws ParseException, ReflectiveOperationException {
        Map optionMap = argsParser(args);
        System.out.println(JSON.toJSON(optionMap));

        for(Field field : fields) {
            String fieldName = field.getName();
            Option option = field.getAnnotation(Option.class);
            if(optionMap.containsKey(option.name())) {
                optionMap.put(fieldName, optionMap.remove(option.name()));
            } else if(optionMap.containsKey(option.longName())) {
                optionMap.put(fieldName, optionMap.remove(option.longName()));
            } else  if(option.required()) {
                throw new RuntimeException("arg '" + fieldName + "' required, usage: " + option.usage());
            }
        }

        return (T) TReflect.getObjectFromMap(optionClass, optionMap, false);
    }

    public String getOptionReadableName(Option option) {
        boolean hasName = !TString.isNullOrEmpty(option.name());
        boolean hasLongName = !TString.isNullOrEmpty(option.longName());
        String optionName = "";
        optionName = optionName + (hasName ? "-" + option.name() : "");
        optionName = optionName + (hasName ? "" : "   ");
        optionName = optionName + (hasName && hasLongName ? "," : "");
        optionName = optionName + (hasLongName ? "--" + (option.longName()) : "");
        return optionName;
    }

    public List<String> usage() {
        List<String> usages = new ArrayList<>();

        for(Field field : fields) {
            Option option = field.getAnnotation(Option.class);

            String optionName = (option.required() ? "* " : "  ") + getOptionReadableName(option);
            usages.add(TString.rightPad(optionName, 35, ' ') + option.usage());
        }

        return usages;
    }

    public void printUsage(){
        printUsage(4);
    }

    public void printUsage(int identCount){
        Command command = optionClass.getAnnotation(Command.class);
        if(command!=null) {
            System.out.println(command.description());
            System.out.println("usage: " + command.usage());
        }

        for(Object str : usage()){
            str = TString.indent((String) str, identCount);
            System.out.println(str);
        }

        if(command!=null) {
            if(!TString.isNullOrEmpty(command.version())) {
                System.out.println(TString.rightPad("version: ", 11, ' ') + command.version());
            }
            if(!TString.isNullOrEmpty(command.author())) {
                System.out.println(TString.rightPad("author: ", 11, ' ') + command.author());
            }
            if(!TString.isNullOrEmpty(command.contact())) {
                System.out.println(TString.rightPad("contact: ", 11, ' ') + command.contact());
            }
            if(!TString.isNullOrEmpty(command.copyright())) {
                System.out.println(TString.rightPad("copyright: ", 11, ' ') + command.copyright());
            }

            if(!TString.isNullOrEmpty(command.licence())) {
                System.out.println(TString.rightPad("licence: ", 11, ' ') + command.licence());
            }
        }
    }

    public static void putOptionMap(Map<String, Object> optionMap, String key, String tmp) {
        Object value = optionMap.get(key);
        if(value == null) {
            optionMap.put(key, tmp);
        } else if (value instanceof List) {
            ((List)value).add(tmp);
        } else {
            List valuelist = new ArrayList();
            if(value!=null) {
                valuelist.add(value);
            }
            valuelist.add(tmp);
            optionMap.put(key, valuelist);
        }
    }

    private static Map<String, Object> argsParser(String[] args) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        for(String arg : args) {
            String t = arg;

            t = TString.fastReplaceAll(t, "^'", "\"");
            t = TString.fastReplaceAll(t, "'$", "\"");
            if(t.contains(" ")) {
                t = "\"" + t + "\"";
            }

            stringJoiner.add(t);
        }

        String argsLine = stringJoiner.toString();

        System.out.println(argsLine);

        char curr = 0;
        char pre  = 0;
        char next = 0;

        String tmp = "";
        Boolean isOpt = true;
        Boolean isOptArg = false;
        Boolean isString = false;

        Map<String, Object> optionMap = new HashMap<String, Object>();
        String key = "";
        boolean isShortCmd = false;

        for(int i=0;i<argsLine.length();i++) {
            curr = argsLine.charAt(i);
            pre = i>0                    ? argsLine.charAt(i-1) : pre;
            next = i<argsLine.length()-1 ? argsLine.charAt(i+1) : 0;
            //选项
            if(!isString && curr == '-' && (pre=='-' || pre==0 || pre==' ')) {
                isOpt = true;
                isOptArg = false;

                isShortCmd = isShortCmd == false ? true : false;
                continue;
            }

            if(!isString && curr == ' ') {
                //选项名称
                if(isOpt) {
                    isOpt = false;
                    isOptArg = true;
                    key = tmp;

                    if(isShortCmd && key.length()>1) {
                        for(char oneKey : key.toCharArray())
                        putOptionMap(optionMap, oneKey + "", "true");
                    } else {
                        putOptionMap(optionMap, key, null);
                    }

                    tmp = "";
                    isShortCmd = false;

                }
                //选项参数
                else if(isOptArg) {
                    putOptionMap(optionMap, key, tmp);
                    tmp = "";
                }

                continue;
            }


            //字符串
            if(isOptArg && curr=='\"' && pre != '\\') {
                isString = !isString;
                continue;
            }

            tmp = tmp + curr;
        }

        if(!TString.isNullOrEmpty(key)) {
            if(isOpt) {
                key = tmp;
                tmp = null;
            }

            putOptionMap(optionMap, key, tmp);
        }

        return optionMap;
    }

}
