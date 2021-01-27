package org.voovan.tools.reflect.exclude;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Null implements Exclude {
    @Override
    public boolean check(String name, Object parameter) {
        return parameter == null;
    }
}
