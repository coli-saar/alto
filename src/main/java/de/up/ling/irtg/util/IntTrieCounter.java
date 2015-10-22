/**
 * 
 */
package de.up.ling.irtg.util;

import java.io.Serializable;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * @author christoph
 *
 */
public class IntTrieCounter implements Serializable
{
	/**
	 * 
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * 
	 */
	private double nodeNormalizer = 0;

	/**
	 * 
	 */
	private double nodeCount = 0;

	/**
	 * 
	 */
	private final Int2ObjectMap<IntTrieCounter> followers = new Int2ObjectOpenHashMap<>();

	/**
	 * 
	 * @param position
	 * @param length
	 * @param key
	 * @return
	 */
	private double getNorm(int position, int length, int[] key){
		if(length == 0){
			return nodeNormalizer;
		}
		
		IntTrieCounter child = this.followers.get(key[position]);

		return child == null ? 0 : child.getNorm(position+1, length-1, key);
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public double getNorm(IntArrayList key){
		return this.getNorm(0, key.size(), key.elements());
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public double getNorm(int[] key){
		return this.getNorm(0, key.length, key);
	}


	/**
	 * 
	 * @param key
	 * @return
	 */
	public double get(IntArrayList key){
		return this.get(0, key.size(), key.elements());
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public double get(int[] key){
		return this.get(0, key.length, key);
	}

	/**
	 * 
	 * @param position
	 * @param length
	 * @param key
	 * @return
	 */
	private double get(int position, int length, int[] key){
		if(length == 0){
			return getNorm();
		}

		IntTrieCounter child = this.followers.get(key[position]);

		return child == null ? 0 : child.get(position+1, length-1, key);
	}

        /**
        * 
        * @return 
        */
        public double getNorm() {
            return nodeCount;
        }

	/**
	 * 
	 * @param key
	 * @param amount
	 * @return
	 */
	public double add(IntArrayList key, double amount){
		return this.add(0, key.size(), key.elements(), amount);
	}

	/**
	 * 
	 * @param key
	 * @param amount
	 * @return
	 */
	public double add(int[] key, double amount){
		return this.add(0, key.length, key, amount);
	}

	/**
	 * 
	 * @param position
	 * @param length
	 * @param key
	 * @param amount
	 * @return
	 */
	private double add(int position, int length, int[] key, double amount){
		if(length == 0){
			nodeNormalizer += amount;
			return nodeCount += amount;
		}

		nodeNormalizer += amount;

		IntTrieCounter child = this.followers.get(key[position]);
		if(child == null){
			child = new IntTrieCounter();
			this.followers.put(key[position], child);
		}

		return child.add(position+1, length-1, key, amount);
	}

	/**
	 * 
	 * @return
	 */
	public IntIterator keyIterator()
	{
		return this.followers.keySet().iterator();
	}

	/**
	 * 
         * @param key
	 * @return
	 */
	public IntTrieCounter getSubtrie(int[] key)
	{
		return this.getSubtrie(0, key.length, key);
	}

	/**
	 * 
	 * @param i
	 * @param length
	 * @param key
	 * @return
	 */
	private IntTrieCounter getSubtrie(int position, int length, int[] key)
	{
		if(length == 0){
			return this;
		}
		
		IntTrieCounter ltc = this.followers.get(key[position]);
		if(ltc == null){
			return null;
		}
		
		return ltc.getSubtrie(position+1, length-1, key);
	}

	/**
	 * 
	 * @param word
	 * @return
	 */
	public IntTrieCounter getSubtrie(int word)
	{
		IntTrieCounter ltc = this.followers.get(word);
		if(ltc == null){
			return null;
		}
		
		return ltc;
	}
}