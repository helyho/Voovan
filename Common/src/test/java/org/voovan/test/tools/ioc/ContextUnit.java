package org.voovan.test.tools.ioc;

import org.voovan.tools.ioc.Container;
import org.voovan.tools.ioc.Context;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class ContextUnit {
    public static void main(String[] args) {
        Container container = Context.getDefaultContainer();
        IOC2 ioc2 = container.getByAnchor("IOC2", null);
        IOC3 ioc30 = container.get("IOC3Method", null);
        IOC3 ioc31 = container.getByAnchor("IOC3", null);
        IOC3 ioc32 = container.getByAnchor("IOC3", null);
        System.out.println("1111");
    }
}
