package org.voovan.tools.buffer;

import org.voovan.tools.TUnsafe;

/**
 * 自动跟踪 GC 销毁的
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Deallocator implements Runnable {
    private long address;
    private int capacity;

    Deallocator(long address, int capacity) {
        this.address = address;
        this.capacity = capacity;
    }

    public void setAddress(long address){
        this.address = address;
    }

    public long getAddress() {
        return address;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void run() {

        if (this.address == 0) {
            return;
        }
        TUnsafe.getUnsafe().freeMemory(address);
        address = 0;
        TByteBuffer.free(capacity);
    }
}
