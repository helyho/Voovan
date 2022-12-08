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
        Context.init("org.voovan.test.tools.ioc");

        Container container = Context.getDefaultContainer();
        IOC2 ioc2 = container.getByAnchor("IOC2", null);
        System.out.println("1111");
    }
}
