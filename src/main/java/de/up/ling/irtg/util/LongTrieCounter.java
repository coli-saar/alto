/**
 * 
 */
package de.up.ling.irtg.util;

import java.io.Serializable;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * @author christoph
 *
 */
public class LongTrieCounter implements Serializable
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
	private final Long2ObjectMap<LongTrieCounter> followers = new Long2ObjectOpenHashMap<>();

	/**
	 * 
	 * @param position
	 * @param length
	 * @param key
	 * @return
	 */
	private double getNorm(int position, int length, long[] key){
		if(length == 0){
			return nodeNormalizer;
		}
		
		LongTrieCounter child = this.followers.get(key[position]);

		return child == null ? 0 : child.getNorm(position+1, length-1, key);
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public double getNorm(LongArrayList key){
		return this.getNorm(0, key.size(), key.elements());
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public double getNorm(long[] key){
		return this.getNorm(0, key.length, key);
	}


	/**
	 * 
	 * @param key
	 * @return
	 */
	public double get(LongArrayList key){
		return this.get(0, key.size(), key.elements());
	}

	/**
	 * 
	 * @param key
	 * @return
	 */
	public double get(long[] key){
		return this.get(0, key.length, key);
	}

	/**
	 * 
	 * @param position
	 * @param length
	 * @param key
	 * @return
	 */
	private double get(int position, int length, long[] key){
		if(length == 0){
			return getNorm();
		}

		LongTrieCounter child = this.followers.get(key[position]);

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
	public double add(LongArrayList key, double amount){
		return this.add(0, key.size(), key.elements(), amount);
	}

	/**
	 * 
	 * @param key
	 * @param amount
	 * @return
	 */
	public double add(long[] key, double amount){
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
	private double add(int position, int length, long[] key, double amount){
		if(length == 0){
			nodeNormalizer += amount;
			return nodeCount += amount;
		}

		nodeNormalizer += amount;

		LongTrieCounter child = this.followers.get(key[position]);
		if(child == null){
			child = new LongTrieCounter();
			this.followers.put(key[position], child);
		}

		return child.add(position+1, length-1, key, amount);
	}

	/**
	 * 
	 * @return
	 */
	public LongIterator keyIterator()
	{
		return this.followers.keySet().iterator();
	}
	
	/**
	 * 
         * @param depth
	 * @return
	 */
	public ObjectIterator<long[]> getSequenceIterator(int depth)
	{
		return new SequenceIterator(depth);
	}

	/**
	 * 
         * @param key
	 * @return
	 */
	public LongTrieCounter getSubtrie(long[] key)
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
	private LongTrieCounter getSubtrie(int position, int length, long[] key)
	{
		if(length == 0){
			return this;
		}
		
		LongTrieCounter ltc = this.followers.get(key[position]);
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
	public LongTrieCounter getSubtrie(long word)
	{
		LongTrieCounter ltc = this.followers.get(word);
		if(ltc == null){
			return null;
		}
		
		return ltc;
	}
	
	/**
	 * 
	 * @author christoph
	 *
	 */
	public class SequenceIterator implements ObjectIterator<long[]>{
		/**
		 * 
		 */
		private final ObjectArrayList<LongTrieCounter> active;
		
		/**
		 * 
		 */
		private final ObjectArrayList<LongIterator> iterators;
		
		/**
		 * 
		 */
		private final long[] sequence;
		
		/**
		 * 
		 */
		private final long[] result;
		
		/**
		 * 
		 */
		private boolean isEmpty;
		
		/**
		 * 
		 * @param depth
		 */
		public SequenceIterator(int depth)
		{
			this.active = new ObjectArrayList<>(depth);
			this.iterators = new ObjectArrayList<>(depth);
			this.sequence = new long[depth];
			this.result = new long[depth];
			
			active.add(LongTrieCounter.this);
			iterators.add(LongTrieCounter.this.keyIterator());
			if(!iterators.get(0).hasNext()){
				this.isEmpty = true;
				return;
			}
			sequence[0] = iterators.get(0).nextLong();
			
			for(int i=1;i<depth;++i){
				LongTrieCounter ltc = active.get(i-1).followers.get(sequence[i-1]);
				active.add(ltc);
				LongIterator li;
				iterators.add(li = ltc.keyIterator());
				if(!li.hasNext()){
					this.isEmpty = true;
					return;
				}
				
				sequence[i] = li.nextLong();
			}
			
			this.isEmpty = false;
		}
		
		
		@Override
		public boolean hasNext()
		{
                    return !isEmpty;
		}

		@Override
		public long[] next()
		{
			System.arraycopy(this.sequence, 0, result, 0, this.sequence.length);
			
			update();
			
			return result;
		}

		/**
		 * 
		 */
		private void update()
		{
			while(true){
				int pos = this.sequence.length-1;
				while(!this.iterators.get(pos).hasNext()){
					if(--pos < 0){
						this.isEmpty = true;
						return;
					}
				}

				this.sequence[pos] = this.iterators.get(pos).nextLong();
				
				boolean done = true;
				for(int i=pos+1;i<sequence.length;++i){
					LongTrieCounter ltc = active.get(i-1).followers.get(sequence[i-1]);
					active.set(i, ltc);
					LongIterator li;
					iterators.set(i, li = ltc.keyIterator());
					if(!li.hasNext()){
						done = false;
						break;
					}
					
					sequence[i] = li.nextLong();
				}
				
				if(done){
					return;
				}
			}
		}


		@Override
		public int skip(int arg0)
		{
			int count = 0;
			while(count < arg0 && this.hasNext()){
				++count;
				this.next();
			}
			
			return count;
		}
	}
}