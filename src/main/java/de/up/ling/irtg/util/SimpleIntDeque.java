package de.up.ling.irtg.util;

import it.unimi.dsi.fastutil.ints.IntListIterator;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntListIterator;

public class SimpleIntDeque extends AbstractIntDeque {
	
	private static class Cell{
		int data;
		Cell next;
		Cell prev;
		
		private Cell(){
			this.next = null;
			this.prev = null;
		}

	}
	
	private static Cell emptyCell(){
		Cell c = new Cell();
		c.next = c;
		c.prev = c;
		return c;
	}
	
	private static void insertBefore(int data, Cell after){
		Cell c = new Cell();
		Cell before = after.prev;
		
		c.data = data;
		c.prev = before;
		c.next = after;
		
		before.next = c;
		after.prev = c;
		
	}
	
	/*
	 * Never to be called on the sentinel.
	 */
	private static void delete(Cell c){
		c.prev.next = c.next;
		c.next.prev = c.prev;
		
		c.prev = null;
		c.next = null;
		
	}
	
	
	private Cell sentinel;
	private int size;

	public SimpleIntDeque(){
		this.sentinel = emptyCell();
		this.size = 0;
	}
	

	@Override
	public boolean isEmpty(){
		return this.size == 0;
	}



	@Override
	public void addFirst(int x) {
		insertBefore(x, this.sentinel.next);
		this.size++;
	}

	@Override
	public boolean offerFirst(int x) {
		this.addFirst(x);
		return true;
	}

	@Override
	public int removeFirstInt() {
		Cell rem = this.sentinel.next;
		int data = rem.data;
		delete(rem);
		this.size--;
		return data;
	}


	@Override
	public int getFirstInt() {
		return this.sentinel.next.data;
	}

	@Override
	public void addLast(int x) {
		insertBefore(x, this.sentinel);
		this.size++;
	}

	@Override
	public boolean offerLast(int x) {
		this.addLast(x);
		return true;
	}

	@Override
	public int removeLastInt() {
		Cell rem = this.sentinel.prev;
		int data = rem.data;
		delete(rem);
		this.size--;
		return data;
	}


	@Override
	public int getLastInt() {
		return this.sentinel.prev.data;
	}

	@Override
	public int size() {
		return this.size;
	}


	@Override
	public void clear() {
		while(!this.isEmpty()){
			this.removeFirstInt();
		}
	}


	
	private class CellIterator implements IntListIterator{

		private Cell current;
		private boolean forward;
		private int index;
		private boolean moved;
		
		private CellIterator(boolean forward){
			this.forward = forward;
			this.current = SimpleIntDeque.this.sentinel;
			this.index = 0;
			this.moved = false;
		}
		
		private void move(boolean towardsNext){
			if((this.forward && towardsNext) || (!this.forward && !towardsNext)){
				this.current = current.next;
			}else{
				this.current = current.prev;
			}
			this.index = (towardsNext) ? this.index + 1 : this.index - 1;
			this.moved = true;
		}
		
		@Override
		public boolean hasNext() {
			return this.index != SimpleIntDeque.this.size();
		}

		@Override
		public boolean hasPrevious() {
			return this.index != 0;
		}

		@Override
		public int nextInt() {
			this.move(true);
			return this.current.data;			
		}

		@Override
		public int previousInt() {
			int data = this.current.data;
			this.move(false);
			return data;
		}

		@Override
		public int nextIndex() {
			return this.forward ? this.index : SimpleIntDeque.this.size()-this.index-1;
		}

		@Override
		public int previousIndex() {
			return this.forward ? this.index - 1 : SimpleIntDeque.this.size()-this.index;
		}
		
		@Override
		public void remove(){
			if(!moved){
				throw new IllegalStateException();
			}else{
				Cell deleted = this.current;
				this.move(false);
				delete(deleted);
				this.moved = false;
				
				SimpleIntDeque.this.size--;
			}
		}
		
		@Override
		public void add(int x){
			if(this.forward){
				insertBefore(x, this.current.next);
				this.move(true);
			}else{
				insertBefore(x, this.current);
				this.move(true);
			}
			
			this.moved = false;
			SimpleIntDeque.this.size++;
		}
		
	}
	
	@Override
	public IntListIterator iterator() {
		return new CellIterator(true);
	}

	@Override
	public IntListIterator descendingIterator() {
		return new CellIterator(false);
	}
	
	/**
	 * In place sorted merge other's element into this collection. The result might contains the same element multiple times.
	 * @param other
	 */
	public void sortedMerge(IntIterable other){
		IntListIterator it = this.iterator();
		for(int e : other){
			while(it.hasNext()){
				int next = it.nextInt();
				if(e < next){
					it.previous();
					break;
				}
			}
			it.add(e);
			
		}
	}


	public static void main(String[] args){
		SimpleIntDeque q = new SimpleIntDeque();
		
		for(int i=0; i<10; i++) {
			q.addFirst(i); q.addLast(i);
			System.err.println(q.size());
		}
		
		IntListIterator it = q.iterator();
		while(it.hasNext()){

			System.err.print(it.nextInt());
			System.err.print(", ");
		}			
		System.err.println("-> done");
		
		while(it.hasPrevious()){
			System.err.print(it.previousInt());
			System.err.print(", ");
		}
		
		System.err.println("<- done");
		
		
		it = q.descendingIterator();
		while(it.hasNext()){

			System.err.print(it.nextInt());
			System.err.print(", ");
		}			
		System.err.println("-> done");
		
		while(it.hasPrevious()){
			System.err.print(it.previousInt());
			System.err.print(", ");
		}
		System.err.println("<- done");

		
		for(int i=0; i<10; i++){
			System.err.println("retrieving: "+q.removeFirstInt()+", "+q.removeLastInt());
			System.err.println(q.size());
		}
		
		System.err.println(q.iterator().hasNext());
	
		
		SimpleIntDeque qq = new SimpleIntDeque();
		SimpleIntDeque other = new SimpleIntDeque();
		for(int i=0; i<10;i++){
			qq.add(2*i);
			other.add(2*i+1);
		}
		
		qq.sortedMerge(other);
		System.err.println("merge: ");
		for(int i : qq){
			System.err.print(i+", ");
		}
	
		
	}
	
}
