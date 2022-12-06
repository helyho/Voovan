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
        Context context = new Context("org.voovan.test.tools.ioc");
        context.init();

        Container container = context.getContainer("default");
        IOC2 ioc2 = container.get("IOC2", null);
        System.out.println("1111");
    }
}
