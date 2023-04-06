package org.voovan.test.tools.ioc;

import org.voovan.tools.TEnv;
import org.voovan.tools.ioc.Container;
import org.voovan.tools.ioc.Context;
import org.voovan.tools.log.Logger;

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
        Logger.simple("=========^ioc2=========");
        IOC3 ioc30 = container.get("IOC3Method", null);
        Logger.simple("=========^ioc3 by ioc2 method bean=========");
        IOC3 ioc31 = container.getByAnchor("IOC3", null);
        Logger.simple("=========^ioc3-1=========");
        IOC3 ioc32 = container.getByAnchor("IOC3", null);
        Logger.simple("=========^ioc3-2=========");
        container.addExtBean("IOCExt", new IOCExt());
        IOCExt IOCExt = container.getByAnchor("IOCExt", null);
        Logger.simple("=========^iocext=========");
    }
}
