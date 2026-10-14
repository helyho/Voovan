package org.voovan.test.tools.reflect;

import junit.framework.TestCase;

import org.junit.Test;
import org.voovan.tools.json.JSON;
import org.voovan.tools.reflect.ClassModel;
import org.voovan.tools.reflect.annotation.NotSerialization;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

/**
 * getClazzModel 简单测试
 * 验证类的模型描述及字段注解描述是否正确
 *
 * @author helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class GetClazzModelTest extends TestCase {

    /**
     * 测试用的注解
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface TestAnno {
        String value() default "";
    }

    /**
     * 测试数组/字符/转义字符串参数的注解
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ComplexAnno {
        String[] names() default {};
        char tag() default 'a';
        String esc() default "";
        int[] ints() default {};
    }

    /**
     * 测试用的实体类
     */
    public static class Entity {
        @TestAnno("name-anno")
        @Deprecated
        private String name;

        @TestAnno("1111")
        private int age;

        private String noAnno;

        @NotSerialization
        private String filtered;

        private transient String trans;

        private static String STATIC_FIELD;

        private HashMap<String, String> map;
    }

    /**
     * 数组/字符/转义字符串参数的实体类
     */
    public static class Entity2 {
        @ComplexAnno(names = {"a\"b", "c\\d"}, tag = 'x', esc = "he said \"hi\"\n", ints = {1, 2})
        private String data;
    }

    /**
     * 系统类型直接返回类名
     */
    public void testSystemType() {
        assertEquals("java.lang.String", ClassModel.getClazzModel(String.class));
    }

    /**
     * Map 类型直接返回类名
     */
    public void testMapType() {
        assertEquals("java.util.HashMap", ClassModel.getClazzModel(HashMap.class));
    }

    /**
     * 模型包含类名描述 "$" 键
     */
    public void testContainClassName() {
        String model = ClassModel.getClazzModel(Entity.class);
        assertTrue(model.contains("\"$\":\"org.voovan.test.tools.reflect.GetClazzModelTest.Entity\""));
    }

    /**
     * 模型包含普通字段
     */
    public void testContainField() {
        String model = ClassModel.getClazzModel(Entity.class);
        assertTrue(model.contains("\"name\":\"java.lang.String\""));
        assertTrue(model.contains("\"age\":\"int\""));
        assertTrue(model.contains("\"map\":\"java.util.HashMap\""));
    }

    /**
     * 字段的注解使用 "@字段名" 作为键, 值为注解描述数组, 包含注解全名及所有参数
     */
    @Test
    public void testFieldAnnotation() {
        String model = ClassModel.getClazzModel(Entity.class);
        //包含注解全名和参数值
        String prefix = "org.voovan.test.tools.reflect.GetClazzModelTest";
        assertTrue(model.contains("\"@name\":[{\"@\":\"" + prefix + ".TestAnno\",\"value\":\"name-anno\"},{\"@\":\"java.lang.Deprecated\""));
        assertTrue(model.contains("\"@age\":[{\"@\":\"" + prefix + ".TestAnno\",\"value\":\"1111\"}]"));

        //无注解字段不生成 "@字段名" 键
        assertFalse(model.contains("\"@noAnno\""));
    }

    /**
     * NotSerialization / transient / static 字段不出现在模型中
     */
    public void testFilteredField() {
        String model = ClassModel.getClazzModel(Entity.class);
        assertFalse(model.contains("\"filtered\""));
        assertFalse(model.contains("\"trans\""));
        assertFalse(model.contains("\"STATIC_FIELD\""));
    }

    /**
     * buildClass 将注解描述还原为字段上的注解声明代码
     */
    @Test 
    public void testBuildClassWithAnnotation() {
        String model = ClassModel.getClazzModel(Entity.class);
        String code = ClassModel.buildClass(model);
        assertNotNull(code);
        //注解代码位于字段声明之前, 注解参数同步还原(参数按字母序: forRemoval < since)
        String prefix = "org.voovan.test.tools.reflect.GetClazzModelTest";
        assertTrue(code.contains("@" + prefix + ".TestAnno(value=\"name-anno\") @java.lang.Deprecated(forRemoval=false,since=\"\") private String name;"));
        assertTrue(code.contains("@" + prefix + ".TestAnno(value=\"1111\") private int age;"));
    }

    /**
     * 数组/字符/转义字符串参数生成合法 JSON, 且 buildClass 还原为 Java 数组语法 {...}
     */
    @Test
    public void testArrayCharEscapeAnnotation() {
        String model = ClassModel.getClazzModel(Entity2.class);

        //数组参数输出为合法 JSON 数组, 字符参数加引号
        assertTrue(model.contains("\"names\":[\"a\\\"b\",\"c\\\\d\"]"));
        assertTrue(model.contains("\"tag\":\"x\""));
        assertTrue(model.contains("\"ints\":[1,2]"));
        assertTrue(model.contains("\"esc\":\"he said \\\"hi\\\"\\n\""));

        //模型是合法 JSON, 可被解析
        Object parsed = JSON.parse(model);
        assertTrue(parsed instanceof Map);

        //buildClass 还原: 数组参数使用 {...} 语法
        //(注: JSONDecode 保留字符串中的转义序列原样, buildClass 再转义一层产生双重转义, 为框架 JSON 既有行为)
        String code = ClassModel.buildClass(model);
        assertNotNull(code);
        assertTrue(code.contains("names={\"a\\\\\\\"b\",\"c\\\\\\\\d\"}"));
        assertTrue(code.contains("tag=\"x\""));
        assertTrue(code.contains("ints={1,2}"));
        assertTrue(code.contains("esc=\"he said \\\\\\\"hi\\\\\\\"\\\\n\""));
    }

    /**
     * 模型中注解参数为 null 时, buildClass 跳过该参数(注解参数不允许 null)
     */
    @Test
    public void testNullParamSkipped() {
        String model = "{\"$\":\"org.voovan.test.tools.reflect.GetClazzModelTest.NullEntity\","
                + "\"field\":\"java.lang.String\","
                + "\"@field\":[{\"@\":\"java.lang.Deprecated\",\"since\":null,\"forRemoval\":false}]}";
        String code = ClassModel.buildClass(model);
        assertNotNull(code);
        //since=null 被跳过, 只保留 forRemoval=false
        assertTrue(code.contains("@java.lang.Deprecated(forRemoval=false)"));
        assertFalse(code.contains("since"));
    }
}
