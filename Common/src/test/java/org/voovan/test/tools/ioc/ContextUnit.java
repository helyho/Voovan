package org.voovan.test.tools.ioc;

import org.voovan.tools.TEnv;
import org.voovan.tools.ioc.Container;
import org.voovan.tools.ioc.Context;
import org.voovan.tools.log.Logger;

import junit.framework.TestCase;

/**
 * Class name
 *
 * @author helyho
 * voovan Framework.
 * WebSite: https://github.com/helyho/voovan
 * Licence: Apache v2 License
 */
public class ContextUnit extends TestCase{
    public void testAll() {
        Container container = Context.getDefaultContainer();
        Context.init();
        IOC2 ioc2 = container.getByAnchor("IOC2", null);
        Logger.simple("=========^ioc2=========");
        IOC3 ioc30 = container.get("IOC3Method", null); // in ioc2
        Logger.simple("=========^ioc3 by ioc2 method bean=========");
        IOC3 ioc31 = container.getByAnchor("ovttiIOC3", null);
        ioc31.setStr("gxx");
        Logger.simple("=========^ioc3-1=========");
        IOC3 ioc32 = container.getByAnchor("ovttiIOC3", null);
        Logger.simple("=========^ioc3-2=========");
        container.addExtBean("IOCExt", new IOCExt());
        IOCExt IOCExt = container.getByAnchor("IOCExt", null);
        Logger.simple("=========^iocext=========");
    }
}
