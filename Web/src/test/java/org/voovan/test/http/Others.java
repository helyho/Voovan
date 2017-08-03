package org.voovan.test.http;

import org.voovan.http.server.HttpRouter;
import org.voovan.http.websocket.WebSocketRouter;
import org.voovan.test.http.router.TestAnnotation;
import org.voovan.tools.TEnv;
import org.voovan.tools.log.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Others {

    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        TEnv.addClassPath(new File("/Users/helyho/Work/Java/Voovan/target/test-classes/Web"));
        TEnv.addClassPath(new File("/Users/helyho/Work/Java/Voovan/target/test-classes/Web"));
        List<Class> classes = TEnv.searchClassInEnv("org.voovan", new Class[]{HttpRouter.class, TestAnnotation.class} );

        Logger.info("Size: "+classes.size());
    }
}
