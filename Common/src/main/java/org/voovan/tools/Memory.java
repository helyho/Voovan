package org.voovan.tools;

import org.voovan.tools.json.JSON;
import org.voovan.tools.json.annotation.NotJSON;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Memory {

    private volatile long address;
    private volatile long capacity; //单位 byte

    //Key: 分配的内存大小, 只保存可以分配的
    private ConcurrentSkipListMap<Long, ConcurrentLinkedDeque<MemBlock>> freedMemBlocksMapBySize = new ConcurrentSkipListMap<Long, ConcurrentLinkedDeque<MemBlock>>();

    //按地址保存所有的内存块
    protected ConcurrentHashMap<Long, MemBlock> memBlocksMapByStartAddress = new ConcurrentHashMap<Long, MemBlock>();
    protected ConcurrentHashMap<Long, MemBlock> memBlocksMapByEndAddress = new ConcurrentHashMap<Long, MemBlock>();

    /**
     * 构造函数
     * @param capacity 容量大小, 单位: byte
     */
    public Memory(long capacity) {
        long tempCapacity = capacity;

        //检查参数是否合法
        {
            boolean capacityCorrect = false;

            if(capacity < 1024){
                throw new UnsupportedOperationException("The capacity is error, it's must be larger than 1024 byte");
            }


            if(tempCapacity%2!=0){
                throw new UnsupportedOperationException("The capacity is error, it's not base on 2");
            }

            //用 33 可以分配到 8TB 的内存
            for (int i = 0; i < 33; i++) {
                if (tempCapacity == Math.pow(2, i)) {
                    capacityCorrect = true;
                    break;
                }
            }

            if (!capacityCorrect) {
                throw new UnsupportedOperationException("The capacity is error, it's not base on 2");
            }
        }

        this.capacity = capacity;
        this.address = TUnsafe.getUnsafe().allocateMemory(capacity);

        //初始化freedMemBlocksMapBySize
        {
            tempCapacity = capacity;
            freedMemBlocksMapBySize.put(tempCapacity, new ConcurrentLinkedDeque<MemBlock>());
            MemBlock rootBlock = new MemBlock(this, 0, capacity - 1, capacity);
            freedMemBlocksMapBySize.get(tempCapacity).addLast(rootBlock);
            tempCapacity = tempCapacity / 2;
            while (true) {
                freedMemBlocksMapBySize.put(tempCapacity, new ConcurrentLinkedDeque<MemBlock>());
                if (tempCapacity == 1024) {
                    break;
                }

                tempCapacity = tempCapacity / 2;
            }
        }
    }

    /**
     * 释放内存
     */
    public void free(){
        if(address!=0) {
            TUnsafe.getUnsafe().freeMemory(address);
            address = 0;
        }
    }

    /**
     * 移除一个内存块
     * @param memBlock 需移除的内存块
     */
    private void removeBlock(MemBlock memBlock){
        if(memBlock == null){
            return;
        }

        freedMemBlocksMapBySize.get(memBlock.getSize()).remove(memBlock);
        memBlock.remove();
    }

    /**
     * 新增一个内存块
     * @param memBlocks 新增的多个内存块
     */
    private void addBlock(MemBlock ... memBlocks){
        for (int i = 0; i < memBlocks.length; i++) {
            if(memBlocks[i]==null){
                continue;
            }

            freedMemBlocksMapBySize.get(memBlocks[i].getSize()).offer(memBlocks[i]);
        }
    }

    //获取标准的内存块大小
    private long getStanderBlockSize(long size){
        SortedMap<Long, ConcurrentLinkedDeque<MemBlock>> avaliableBlockSizeMap = freedMemBlocksMapBySize.tailMap(size);
        return avaliableBlockSizeMap.firstKey();
    }

    /**
     * 探测可以拆分的内存块
     * @param fixedSize
     * @return
     */
    private MemBlock findTopSplitBlock(long fixedSize){
        //探测可用来进行分配的最大尺寸内存快
        long maxFreedSize = fixedSize * 2;
        while (true) {
            ConcurrentLinkedDeque<MemBlock> memBlockLinkDeque = freedMemBlocksMapBySize.get(maxFreedSize);
            if (!memBlockLinkDeque.isEmpty()) {
                return memBlockLinkDeque.poll();
            }
            maxFreedSize = maxFreedSize * 2;
            if(maxFreedSize > capacity){
               return null;
            }
        }
    }

    /**
     * 内存分割
     * @param parentMemBlock 需要拆分的内存块
     */
    private synchronized MemBlock split(MemBlock parentMemBlock, long targetSize){
        if(parentMemBlock==null || parentMemBlock.isUsed()){
            return null;
        }

        if(parentMemBlock.getSize()==1024){
            return null;
        }
        long spliteSize = parentMemBlock.getSize()/2;


        //拆分块
        MemBlock memBlockForStore = new MemBlock(this, parentMemBlock.getStartAddress(), parentMemBlock.getStartAddress() + spliteSize - 1, spliteSize);
        MemBlock memBlockForUse = new MemBlock(this, parentMemBlock.getStartAddress() + spliteSize, parentMemBlock.getEndAddress(), spliteSize);

        //重新分配链表上的块
        removeBlock(parentMemBlock);
        addBlock(memBlockForStore);

        if(spliteSize!=targetSize){
            memBlockForUse = split(memBlockForUse, targetSize);
        }

        return memBlockForUse;
    }

    /**
     * 分配内存
     * @param blockSize
     * @return
     */
    private MemBlock malloc(long blockSize){

        long fixedSize = getStanderBlockSize(blockSize);

        MemBlock result = null;

        if(freedMemBlocksMapBySize.get(fixedSize).isEmpty()) {

            //探测可用户拆分的内存块
            MemBlock memBlock = findTopSplitBlock(fixedSize);

            //拆分内存块
            memBlock = split(memBlock, fixedSize);
            if(memBlock==null){
               return null;
            } else {
                return memBlock;
            }

        } else {
            result = freedMemBlocksMapBySize.get(fixedSize).poll();
        }

        return result;
    }

    public Long allocate(long blockSize) {
        while(true) {
            MemBlock result = malloc(blockSize);
            if(result==null){
                continue;
            }
            synchronized (result) {
                result.setUsed(true);
                TUnsafe.getUnsafe().setMemory(this.address + result.getStartAddress(), result.getSize(), (byte)0);

                return address + result.getStartAddress();
            }
        }
    }

    /**
     * 合并空闲内存
     * @param centerMemBlock 需要合并的内存块
     */
    private synchronized MemBlock merge(MemBlock centerMemBlock, boolean isPrev) {
        if(centerMemBlock==null){
            return null;
        }

        MemBlock tmpMemBlock = null;
        long startAddress = centerMemBlock.getStartAddress();
        long endAddress = centerMemBlock.getEndAddress();
        long size = centerMemBlock.getSize();

        if(isPrev) {
            tmpMemBlock = memBlocksMapByEndAddress.get(startAddress - 1);

            if(tmpMemBlock!=null && !tmpMemBlock.isUsed()) {
                if(tmpMemBlock.getSize() != centerMemBlock.getSize()){
                    return null;
                }

                removeBlock(tmpMemBlock);
            } else {
                return null;
            }


        } else {
            tmpMemBlock = memBlocksMapByStartAddress.get(endAddress + 1);

            if(tmpMemBlock!=null && !tmpMemBlock.isUsed()) {
                if(tmpMemBlock.getSize() != centerMemBlock.getSize()){
                    return null;
                }
                removeBlock(tmpMemBlock);
            } else {
                return null;
            }
        }

        if (isPrev) {
            startAddress = tmpMemBlock.getStartAddress();
            size = size + tmpMemBlock.getSize();
        } else {
            endAddress = tmpMemBlock.endAddress;
            size = size + tmpMemBlock.getSize();
        }


        MemBlock mergedMemBlock = new MemBlock(this, startAddress, endAddress, size);
        removeBlock(centerMemBlock);

        return mergedMemBlock;
    }

    /**
     *
     * @param realAddress
     */
    public void release(long realAddress){
        long address = realAddress - this.address;
        MemBlock backBlock = memBlocksMapByStartAddress.get(address);
        MemBlock memBlock = backBlock;
        MemBlock lastMemBlock = null;

//        do {
//            lastMemBlock = memBlock;
//            memBlock = merge(memBlock, true);
//        } while (memBlock != null);
//        addBlock(lastMemBlock);
//
//        do {
//            lastMemBlock = memBlock;
//            memBlock = merge(memBlock, false);
//        } while (memBlock != null);
//        addBlock(lastMemBlock);

        synchronized (backBlock) {
            backBlock.setUsed(false);
        }
    }

    public class MemBlock {
        private long startAddress;
        private long endAddress;
        private long size;
        private AtomicBoolean isUsed = new AtomicBoolean(false);
        @NotJSON
        private Memory memory;
        String mm="";

        public MemBlock(Memory memory, long startAddress, long endAddress, long size) {
            this.startAddress = startAddress;
            this.endAddress = endAddress;
            this.size = size;
            this.memory = memory;
            memory.memBlocksMapByStartAddress.put(startAddress, this);
            memory.memBlocksMapByEndAddress.put(endAddress, this);
        }

        public long getStartAddress() {
            return startAddress;
        }

        public void setStartAddress(long startAddress) {
            this.startAddress = startAddress;
        }

        public long getEndAddress() {
            return endAddress;
        }

        public void setEndAddress(long endAddress) {
            this.endAddress = endAddress;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public boolean isUsed() {
            return isUsed.get();
        }

        public void setUsed(boolean isUsed) {
            this.isUsed.getAndSet(isUsed);
            if(isUsed){
                memory.freedMemBlocksMapBySize.get(size).remove(this);
            } else {
                memory.freedMemBlocksMapBySize.get(size).add(this);
            }
        }

        public void remove(){
            mm = mm + TEnv.getStackMessage() +"\r\n  =================\r\n" ;
            memory.memBlocksMapByStartAddress.remove(this);
            memory.memBlocksMapByEndAddress.remove(this);
        }

        public String toString(){
            return JSON.toJSON(this);
        }
    }
}
