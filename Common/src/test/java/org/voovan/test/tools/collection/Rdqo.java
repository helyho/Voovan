package org.voovan.test.tools.collection;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Class name
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Rdqo implements Delayed {
    long delayTime;
    Object value;

    public Rdqo() {
    }

    public Rdqo(long delayTime, Object value) {
        this.delayTime = delayTime;
        this.value = value;
    }


    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(delayTime, TimeUnit.SECONDS);
    }

    public int compareTo(Delayed o) {
        return 0;
    }
}
