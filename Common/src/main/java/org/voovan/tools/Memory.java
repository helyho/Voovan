package org.voovan.tools;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

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

    //内存分配的恩连续区间,用于分割和合并
    private Vector<MemBlock> MemBlockLinkList = new Vector<MemBlock>();

    //Key: 分配的内存大小, 只保存可以分配的
    private ConcurrentSkipListMap<Long, LinkedList<MemBlock>> freedMemBlocksMapBySize = new ConcurrentSkipListMap<Long, LinkedList<MemBlock>>();

    //按地址保存所有的内存块
    private ConcurrentSkipListMap<Long, MemBlock> memBlocksMapByAddress = new ConcurrentSkipListMap<Long, MemBlock>();


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

        tempCapacity = capacity;
        freedMemBlocksMapBySize.put(tempCapacity, new LinkedList<MemBlock>());
        MemBlock rootBlock = new MemBlock(0, capacity, capacity);
        MemBlockLinkList.add(rootBlock);
        freedMemBlocksMapBySize.get(tempCapacity).addFirst(rootBlock);
        tempCapacity = tempCapacity/2;
        while(true) {
            freedMemBlocksMapBySize.put(tempCapacity, new LinkedList<MemBlock>());
            if(tempCapacity == 1024){
                break;
            }

            tempCapacity = tempCapacity / 2;
        }
    }

    /**
     * 释放内存
     */
    public synchronized void free(){
        if(address!=0) {
            TUnsafe.getUnsafe().freeMemory(address);
            address = 0;
        }
    }

    /**
     * 移除一个内存块
     * @param memBlock 需移除的内存块
     */
    private synchronized void removeBlock(MemBlock memBlock){
        if(memBlock == null){
            return;
        }

        MemBlockLinkList.remove(memBlock);
        freedMemBlocksMapBySize.get(memBlock.getSize()).remove(memBlock);
        memBlocksMapByAddress.remove(memBlock.getStartAddress(), memBlock);
    }

    /**
     * 新增一个内存块
     * @param index 索引位置
     * @param memBlocks 新增的多个内存块
     */
    private synchronized void addBlock(int index, MemBlock ... memBlocks){
            for (int i = 0; i < memBlocks.length; i++) {
                if(memBlocks[i]==null){
                    continue;
                }

                try {
                    MemBlockLinkList.add(index + i, memBlocks[i]);
                    freedMemBlocksMapBySize.get(memBlocks[i].getSize()).addLast(memBlocks[i]);
                    memBlocksMapByAddress.put(memBlocks[i].getStartAddress(), memBlocks[i]);
                }catch (Exception e){
                    e.printStackTrace();
                    i=i;
                }
            }

    }

    /**
     * 内存分割
     * @param parentMemBlock 需要拆分的内存块
     */
    private synchronized boolean split(MemBlock parentMemBlock){
        if(parentMemBlock==null || parentMemBlock.isUsed()){
            return false;
        }

        if(parentMemBlock.getSize()==1024){
            return false;
        }
        long spliteSize = parentMemBlock.getSize()/2;
        int parentMemBlockIndex = MemBlockLinkList.indexOf(parentMemBlock);

        //拆分块
        MemBlock memBlock1 = new MemBlock(parentMemBlock.getStartAddress(), parentMemBlock.getStartAddress() + spliteSize, spliteSize);
        MemBlock memBlock2 = new MemBlock(parentMemBlock.getStartAddress() + spliteSize, parentMemBlock.getEndAddress(), spliteSize);

        //重新分配链表上的块
        addBlock(parentMemBlockIndex, memBlock1, memBlock2);
        removeBlock(parentMemBlock);

        return true;
    }

    /**
     * 合并空闲内存
     * @param centerMemBlock 需要合并的内存块
     */
    private synchronized MemBlock merge(MemBlock centerMemBlock) {
        if(centerMemBlock==null){
            return null;
        }

        int centerMemBlockIndex = MemBlockLinkList.indexOf(centerMemBlock);
        int addMemBlockIndex = centerMemBlockIndex;

        MemBlock prevMemBlock = null;
        MemBlock nextMemBlock = null;
        if(centerMemBlockIndex!=0) {
            prevMemBlock = MemBlockLinkList.get(centerMemBlockIndex - 1);
            if(prevMemBlock.getSize() == centerMemBlock.getSize()) {
                prevMemBlock = prevMemBlock.isUsed() ? null : prevMemBlock;
            } else {
                return null;
            }
        }

        if(prevMemBlock == null) {
            if (centerMemBlockIndex != MemBlockLinkList.size() - 1) {
                nextMemBlock = MemBlockLinkList.get(centerMemBlockIndex + 1);
                if (nextMemBlock.getSize() == centerMemBlock.getSize()) {
                    nextMemBlock = nextMemBlock.isUsed() ? null : nextMemBlock;
                } else {
                    return null;
                }
            }
        }

        if(prevMemBlock==null && nextMemBlock==null){
            return null;
        }

        long startAddress = centerMemBlock.getStartAddress();
        long endAddress = centerMemBlock.getEndAddress();
        long size = centerMemBlock.getSize();

        if(prevMemBlock != null){
            startAddress = prevMemBlock.getStartAddress();
            size = size + prevMemBlock.getSize();
            addMemBlockIndex = MemBlockLinkList.indexOf(prevMemBlock);
        }

        if(nextMemBlock != null){
            endAddress = nextMemBlock.endAddress;
            size = size + nextMemBlock.getSize();
        }


        MemBlock mergedMemBlock = new MemBlock(startAddress, endAddress, size);
        addBlock(addMemBlockIndex, mergedMemBlock);
        removeBlock(centerMemBlock);
        removeBlock(prevMemBlock);
        removeBlock(nextMemBlock);

        return mergedMemBlock;
    }

    /**
     * 分配内存
     * @param blockSize
     * @return
     */
    public synchronized long allocate(long blockSize){
        SortedMap<Long, LinkedList<MemBlock>> avaliableBlockSizeMap = freedMemBlocksMapBySize.tailMap(blockSize);
        long fixedSize = avaliableBlockSizeMap.firstKey();

        if(avaliableBlockSizeMap.get(fixedSize).isEmpty()) {

            //探测可用来进行分配的最大尺寸内存快
            long maxFreedSize = fixedSize * 2;
            while (true) {
                LinkedList<MemBlock> memBlockLinkList = avaliableBlockSizeMap.get(maxFreedSize);
                if (memBlockLinkList == null) {
                    break;
                }

                if (!memBlockLinkList.isEmpty()) {
                    break;
                }

                maxFreedSize = maxFreedSize * 2;
            }

            //拆分内块
            while(split(avaliableBlockSizeMap.get(maxFreedSize).getFirst())) {
                maxFreedSize = maxFreedSize / 2;
                if (maxFreedSize == fixedSize) {
                    break;
                }
            }
        }

        MemBlock result = avaliableBlockSizeMap.get(fixedSize).getFirst();
        result.setUsed(true);
        freedMemBlocksMapBySize.get(result.getSize()).remove(result);
        return address + result.getStartAddress();
    }


    public synchronized void release(long realAddress){
        long address = realAddress - this.address;
        MemBlock memBlock = memBlocksMapByAddress.get(address);
        memBlock.setUsed(false);
        freedMemBlocksMapBySize.get(memBlock.getSize()).addLast(memBlock);
        while(memBlock != null){
            memBlock =  merge(memBlock);
        }
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
