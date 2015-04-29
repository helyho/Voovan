package org.voovan.network;

import java.util.ArrayDeque;
import java.util.Iterator;
/**
 * 对象链
 * @author helyho
 *
 * @param <E>
 */
public class Chain<E> extends ArrayDeque<E>{
	
	private static final long serialVersionUID = -4820686406224770808L;	
	private Iterator<E> iterator;
	private boolean isStop;
	private E currentObj;
	
	/**
	 * 构造函数
	 */
	public Chain(){
		isStop = false;
		rewind();
	}
	
	/**
	 * 重置链的迭代器
	 */
	public void rewind(){
		isStop = false;
		iterator = this.iterator();
	}
	
	/**
	 * 迭代完成
	 */
	public void stop(){
		this.isStop = true;
	}
	
	/**
	 * 迭代器下一个元素
	 * @return
	 */
	public E next(){
		if(isStop){
			return null;
		}
		else{
			if(iterator.hasNext()){
				currentObj = iterator.next();
				return currentObj;
			}
			else {
				return null;
			}
		}
	}
	
	/**
	 * 迭代器当前元素
	 * @return
	 */
	public E current(){
		return currentObj;
	}
	
	/**
	 * 迭代器是否有下一个对象
	 * @return
	 */
	public boolean hasNext(){
		if(isStop){
			return false;
		}
		else{
			return iterator.hasNext();
		}
	}
	
	/**
	 *  从当前对象克隆一个 Chain
	 */
	public Chain<E> clone(){
		Chain<E> chain = new Chain<E>();
		chain.addAll(this);
		chain.rewind();
		return chain;
	}
}
