package org.voovan.tools;

import org.voovan.Global;
import org.voovan.tools.json.JSON;

import java.util.*;
import java.util.concurrent.*;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Memory {

    private long address;
    private long capacity; //单位 byte

    //Key: 分配的内存大小, 只保存可以分配的
    private ConcurrentSkipListMap<Long, ConcurrentLinkedDeque<MemBlock>> freedMemBlocksMapBySize = new ConcurrentSkipListMap<Long, ConcurrentLinkedDeque<MemBlock>>();

    //按地址保存所有的内存块
    private ConcurrentHashMap<Long, MemBlock> memBlocksMapByStartAddress = new ConcurrentHashMap<Long, MemBlock>();
    private ConcurrentHashMap<Long, MemBlock> memBlocksMapByEndAddress = new ConcurrentHashMap<Long, MemBlock>();

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
            MemBlock rootBlock = new MemBlock(0, capacity, capacity);
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
        memBlocksMapByStartAddress.remove(memBlock.getStartAddress(), memBlock);
        memBlocksMapByEndAddress.remove(memBlock.getEndAddress(), memBlock);
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
            memBlocksMapByStartAddress.put(memBlocks[i].getStartAddress(), memBlocks[i]);
            memBlocksMapByEndAddress.put(memBlocks[i].getEndAddress(), memBlocks[i]);
        }
    }

    /**
     * 内存分割
     * @param parentMemBlock 需要拆分的内存块
     */
    private boolean split(MemBlock parentMemBlock){
        if(parentMemBlock==null || parentMemBlock.isUsed()){
            return false;
        }

        if(parentMemBlock.getSize()==1024){
            return false;
        }
        long spliteSize = parentMemBlock.getSize()/2;


        //拆分块
        MemBlock memBlock1 = new MemBlock(parentMemBlock.getStartAddress(), parentMemBlock.getStartAddress() + spliteSize, spliteSize);
        MemBlock memBlock2 = new MemBlock(parentMemBlock.getStartAddress() + spliteSize, parentMemBlock.getEndAddress(), spliteSize);

        //重新分配链表上的块
        addBlock(memBlock1, memBlock2);
        removeBlock(parentMemBlock);

        return true;
    }

    /**
     * 合并空闲内存
     * @param centerMemBlock 需要合并的内存块
     */
    private MemBlock merge(MemBlock centerMemBlock, boolean isPrev) {
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


        MemBlock mergedMemBlock = new MemBlock(startAddress, endAddress, size);
        addBlock(mergedMemBlock);
        removeBlock(centerMemBlock);
        removeBlock(tmpMemBlock);

        return mergedMemBlock;
    }

    /**
     * 分配内存
     * @param blockSize
     * @return
     */
    private MemBlock malloc(long blockSize){
        SortedMap<Long, ConcurrentLinkedDeque<MemBlock>> avaliableBlockSizeMap = freedMemBlocksMapBySize.tailMap(blockSize);
        long fixedSize = avaliableBlockSizeMap.firstKey();
        MemBlock result = null;
        if(avaliableBlockSizeMap.get(fixedSize).isEmpty()) {
            //探测可用来进行分配的最大尺寸内存快
            long maxFreedSize = fixedSize * 2;
            while (true) {
                ConcurrentLinkedDeque<MemBlock> memBlockLinkDeque = avaliableBlockSizeMap.get(maxFreedSize);
                if (!memBlockLinkDeque.isEmpty()) {
                    MemBlock memBlock = memBlockLinkDeque.poll();
                    if (memBlock != null) {
                        //拆分内块
                        while (split(memBlock)) {
                            maxFreedSize = maxFreedSize / 2;
                            memBlock = freedMemBlocksMapBySize.get(maxFreedSize).poll();
                            if (maxFreedSize == fixedSize) {
                                break;
                            }
                        }
                        result = avaliableBlockSizeMap.get(fixedSize).poll();
                        break;
                    }
                }
                maxFreedSize = maxFreedSize * 2;
                if(maxFreedSize > capacity){
                    break;
                }
            }
        } else {
            result = avaliableBlockSizeMap.get(fixedSize).poll();
        }

        if (result != null) {
            result.setUsed(true);
        }

        return result;
    }

    public Long allocate(long blockSize) {
        while(true){
            MemBlock result = malloc(blockSize);
            if(result==null){
                continue;
            }
            result.setUsed(true);
            return address + result.getStartAddress();
        }
    }



    /**
     *
     * @param realAddress
     */
    public void release(long realAddress){
        long address = realAddress - this.address;
        MemBlock backBlock = memBlocksMapByStartAddress.get(address);
        if(backBlock==null){
            return;
        }
        freedMemBlocksMapBySize.get(backBlock.getSize()).addLast(backBlock);
        MemBlock memBlock = null;
        do{
            memBlock =  merge(memBlock, true);
        }while(memBlock != null);

        do{
            memBlock =  merge(memBlock, false);
        }while(memBlock != null);

        backBlock.setUsed(false);
    }



    public class MemBlock {
        private long startAddress;
        private long endAddress;
        private long size;
        private boolean isUsed;

        public MemBlock(long startAddress, long endAddress, long size) {
            this.startAddress = startAddress;
            this.endAddress = endAddress;
            this.size = size;
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
            return isUsed;
        }

        public void setUsed(boolean used) {
            isUsed = used;
        }
    }
}
