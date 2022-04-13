package org.voovan.tools.compiler;

import org.voovan.Global;
import org.voovan.tools.compiler.sandbox.SandboxSecurity;
import org.voovan.tools.compiler.sandbox.SandboxControler;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * 动态类加载器
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class DynamicClassLoader extends URLClassLoader {
    private SandboxControler sandboxControler;
    private SandboxSecurity sandboxSecurity;

    public DynamicClassLoader(ClassLoader parent) {
        super(new URL[]{}, parent);
        if(Global.ENABLE_SANDBOX) {
            SandboxControler sandboxControler = new SandboxControler();
            sandboxControler.loadConfig();
            sandboxSecurity = new SandboxSecurity(sandboxControler);
            System.setSecurityManager(sandboxSecurity);
        }
    }

    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
        try {
            if(Global.ENABLE_SANDBOX) {
                sandboxSecurity.checkLoadClass(name);
            }
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e){
            e.printStackTrace();
            throw e;
        }
    }

    public SandboxControler getSandboxControler() {
        return sandboxControler;
    }

    public void setSandboxControler(SandboxControler sandboxControler) {
        this.sandboxControler = sandboxControler;
    }

    public SandboxSecurity getSandboxSecurity() {
        return sandboxSecurity;
    }

    public void setSandboxSecurity(SandboxSecurity sandboxSecurity) {
        this.sandboxSecurity = sandboxSecurity;
    }
}
