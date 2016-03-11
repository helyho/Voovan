package org.voovan.tools;

import java.util.ArrayDeque;
import java.util.Iterator;
/**
 * 对象链
 * 
 * @author helyho
 *
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class Chain<E> extends ArrayDeque<E>{
	
	private static final long serialVersionUID = -4820686406224770808L;	
	private Iterator<E> iterator;
	private Iterator<E> invertedIterator;
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
		invertedIterator = this.descendingIterator();
	}
	
	/**
	 * 迭代完成
	 */
	public void stop(){
		this.isStop = true;
	}

	/**
	 * 迭代器当前元素
	 * @return
	 */
	public E current(){
		return currentObj;
	}

	/**
	 * 迭代器下一个元素
	 * @return
	 */
	public E next(){
		if(isStop){
			return null;
		} else {
			if(iterator.hasNext()){
				currentObj = iterator.next();
				return currentObj;
			} else {
				return null;
			}
		}
	}


	/**
	 * 迭代器是否有下一个对象
	 * @return
	 */
	public boolean hasNext(){
		if(isStop){
			return false;
		} else {
			return iterator.hasNext();
		}
	}



	/**
	 * 迭代器上一个元素
	 * @return
	 */
	public E previous(){
		if(isStop){
			return null;
		} else {
			if(invertedIterator.hasNext()){
				currentObj = invertedIterator.next();
				return currentObj;
			} else {
				return null;
			}
		}
	}

	/**
	 * 迭代器是否有上一个对象
	 * @return
	 */
	public boolean hasPrevious(){
		if(isStop){
			return false;
		} else {
			return invertedIterator.hasNext();
		}
	}
	
	/**
	 *  从当前对象克隆一个 Chain
	 */
	public Chain<E> clone(){
		super.clone();
		Chain<E> chain = new Chain<E>();
		chain.addAll(this);
		chain.rewind();
		return chain;
	}
}
