package org.voovan.tools;

import junit.framework.TestCase;
import org.junit.Test;

import java.math.BigDecimal;

public class ParameterTypeFactoryTest extends TestCase {

    @Test
    public void testCreate_NullValue() {
        ParameterType parameterType = ParameterTypeFactory.create(null);
        assertTrue(parameterType instanceof BasicParameterType);
    }

    @Test
    public void testCreate_BasicTypeValue() {
        ParameterType parameterType = ParameterTypeFactory.create(10);
        assertTrue(parameterType instanceof BasicParameterType);
    }

    @Test
    public void testCreate_BigDecimalValue() {
        ParameterType parameterType = ParameterTypeFactory.create(new BigDecimal("10.00"));
        assertTrue(parameterType instanceof BigDecimalParameterType);
    }

    @Test
    public void testCreate_ArrayValue() {
        ParameterType parameterType = ParameterTypeFactory.create(new int[] { 1, 2, 3 });
        assertTrue(parameterType instanceof ArrayParameterType);
    }

}