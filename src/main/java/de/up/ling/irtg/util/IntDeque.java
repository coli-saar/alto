package de.up.ling.irtg.util;

import java.util.Deque;

import it.unimi.dsi.fastutil.ints.IntIterable;

public interface IntDeque extends Deque<Integer>, IntIterable {
	
	public void addFirst(int x);
	public boolean offerFirst(int x);
	
	public int removeFirstInt();
	public int getFirstInt();
	
	
	public void addLast(int x);
	public boolean offerLast(int x);

	public int removeLastInt();
	public int getLastInt();
}
