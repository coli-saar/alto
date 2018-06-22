package de.up.ling.irtg.util;

import java.util.Collection;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntStack;

public abstract class AbstractIntDeque implements IntDeque, IntStack{

	@Override
	public void addFirst(Integer e) {
		this.addFirst(e.intValue());
	}

	@Override
	public void addLast(Integer e) {
		this.addLast(e.intValue());
	}

	@Override
	public boolean offerFirst(Integer e) {
		return this.offerFirst(e.intValue());
	}

	@Override
	public boolean offerLast(Integer e) {
		return this.offerLast(e.intValue());
	}

	@Override
	public Integer removeFirst() {
		return this.removeFirstInt();
	}

	@Override
	public Integer removeLast() {
		return this.removeLastInt();
	}

	@Override
	public Integer pollFirst() {
		return (!this.isEmpty()) ? this.removeFirstInt() : null;
	}

	@Override
	public Integer pollLast() {
		return (!this.isEmpty()) ? this.removeFirstInt() : null;
	}

	@Override
	public Integer getFirst() {
		return this.getFirstInt();
	}

	@Override
	public Integer getLast() {
		return this.getLastInt();
	}

	@Override
	public Integer peekFirst() {
		return (!this.isEmpty()) ? this.getFirstInt() : null;
	}

	@Override
	public Integer peekLast() {
		return (!this.isEmpty()) ? this.getLastInt() : null;
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(Integer e) {
		this.addLast(e.intValue());
		return true;
	}

	@Override
	public boolean offer(Integer e) {
		return this.offerLast(e.intValue());
	}

	@Override
	public Integer remove() {
		return this.removeFirstInt();
	}

	@Override
	public Integer poll() {
		return (!this.isEmpty()) ? this.removeFirstInt() : null;
	}

	@Override
	public Integer element() {
		return this.getFirstInt();
	}

	@Override
	public Integer peek() {
		return (!this.isEmpty()) ? this.getFirstInt() : null;
	}
	
	@Override 
	public Integer peek(int arg0){
		return this.peekInt(arg0);
	}
	
	@Override
	public int peekInt(int arg0) {
		IntIterator it = this.iterator();
		for(int i=0;i<arg0;i++){
			it.nextInt();
		}
		return it.nextInt();
	}

	@Override
	public void push(Integer e) {
		this.addFirst(e.intValue());
	}
	
	@Override
	public void push(int x){
		this.addFirst(x);
	}
	
	@Override
	public Integer top() {
		return this.topInt();
	}
	
	@Override
	public int topInt(){
		return this.getFirstInt();
	}

	@Override
	public Integer pop() {
		return this.removeFirstInt();
	}
	
	@Override
	public int popInt(){
		return this.removeFirstInt();
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	abstract public IntIterator iterator();	

	@Override
	public abstract IntIterator descendingIterator();

	public IntArrayList toIntArrayList(){
		IntArrayList copy = new IntArrayList(this.size());
		for(int x: this){
			copy.add(x);
		}
		return copy;
	}
		

	@Override
	@Deprecated
	public <T> T[] toArray(T[] a) {
		return this.toIntArrayList().toArray(a);
	}
	
	@Override
	@Deprecated
	public Object[] toArray() {
		return this.toIntArrayList().toArray();
	}

	@Override
	@Deprecated
	public boolean containsAll(Collection<?> c) {
		return this.toIntArrayList().containsAll(c);
	}

	@Override
	@Deprecated
	public boolean addAll(Collection<? extends Integer> c) {
		for(Integer x: c){
			this.add(x);
		}
		return true;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}
	


}
