package org.voovan.test.tools.hashwheeltimer;

import junit.framework.TestCase;
import org.voovan.tools.TDateTime;
import org.voovan.tools.TEnv;
import org.voovan.tools.hashwheeltimer.HashWheelTask;
import org.voovan.tools.hashwheeltimer.HashWheelTimer;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.util.Date;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HashWheelTimerUnit extends TestCase {

    private HashWheelTimer hashWheelTimer;

    public void setUp() throws IOException {
        hashWheelTimer = new HashWheelTimer(5, 1);

        //1个步长
        hashWheelTimer.addTask(new HashWheelTask() {
            @Override
            public void run() {
                Logger.simple("\t 1: Sync: "+Thread.currentThread().getName()+" "+ TDateTime.format(new Date(), TDateTime.STANDER_DATETIME_TEMPLATE));
            }
        }, 1);

        //两个步长
        hashWheelTimer.addTask(new HashWheelTask() {
            @Override
            public void run() {
                Logger.simple("\t 2: Sync: "+Thread.currentThread().getName()+" "+TDateTime.format(new Date(), TDateTime.STANDER_DATETIME_TEMPLATE));
            }
        }, 2);

        //5个步长
        hashWheelTimer.addTask(new HashWheelTask() {
            @Override
            public void run() {
                Logger.simple("\t 5: Sync: "+Thread.currentThread().getName()+" "+TDateTime.format(new Date(), TDateTime.STANDER_DATETIME_TEMPLATE));
            }
        }, 5);

        //5个步长
        hashWheelTimer.addTask(new HashWheelTask() {
            @Override
            public void run() {
                Logger.simple("\t10: Sync: "+Thread.currentThread().getName()+" "+TDateTime.format(new Date(), TDateTime.STANDER_DATETIME_TEMPLATE));
            }
        }, 10);

        //6个步长
        hashWheelTimer.addTask(new HashWheelTask() {
            @Override
            public void run() {
                Logger.simple("\t 6: Async: "+Thread.currentThread().getName()+" "+TDateTime.format(new Date(), TDateTime.STANDER_DATETIME_TEMPLATE));
            }
        }, 6, true);

        //12个步长
        hashWheelTimer.addTask(new HashWheelTask() {
            @Override
            public void run() {
                Logger.simple("\t12: Sync: "+Thread.currentThread().getName()+" "+TDateTime.format(new Date(), TDateTime.STANDER_DATETIME_TEMPLATE));
            }
        }, 12);
    }

    public void testRotateWheel(){
        hashWheelTimer.rotate();
        TEnv.sleep(60 * 1000 * 10);
    }

}
