package de.up.ling.irtg.util;

import java.util.Deque;

import it.unimi.dsi.fastutil.ints.IntIterable;

public interface IntDeque extends Deque<Integer>, IntIterable {
	
	void addFirst(int x);
	boolean offerFirst(int x);
	
	int removeFirstInt();
	int getFirstInt();
	
	
	void addLast(int x);
	boolean offerLast(int x);

	int removeLastInt();
	int getLastInt();
}
