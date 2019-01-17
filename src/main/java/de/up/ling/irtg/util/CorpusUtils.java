package de.up.ling.irtg.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.saar.basic.Pair;
import de.up.ling.irtg.Interpretation;
import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.algebra.Algebra;
import de.up.ling.irtg.algebra.BinarizingTreeWithAritiesAlgebra;
import de.up.ling.irtg.algebra.StringAlgebra;
import de.up.ling.irtg.algebra.graph.GraphAlgebra;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.automata.TreeAutomaton;
import de.up.ling.irtg.codec.IsiAmrInputCodec;
import de.up.ling.irtg.corpus.Corpus;
import de.up.ling.irtg.corpus.CorpusReadingException;
import de.up.ling.irtg.corpus.Instance;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.signature.Signature;

import de.up.ling.tree.Tree;
import edu.stanford.nlp.process.Tokenizer;
import it.unimi.dsi.fastutil.ints.IntIterable;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import nu.xom.ParsingException;


public class CorpusUtils {
	public static final Pattern notAMR = Pattern.compile("\\s*#.*");
	public static final Pattern empty = Pattern.compile("\\s*");
	public static final Pattern startsWithSemevalSNT = Pattern.compile("\\s*#\\s*(::snt ).*");
	
	@SuppressWarnings({ "rawtypes" })
	public static Corpus retrieveAltoCorpus(String corpusFile, List<Pair<String, Algebra>> interpretations) throws FileNotFoundException, IOException, CorpusReadingException {
		InterpretedTreeAutomaton dull = new InterpretedTreeAutomaton(null);
		
		addInterpretations(interpretations, dull);
		
		Corpus corpus = Corpus.readCorpus(new FileReader(corpusFile), dull);
		return corpus;
	}
	
	
	public static enum IntTypes{
		STRING,
		GRAPH,
		TREE
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void addInterpretations(List<Pair<String, Algebra>> interpretations, InterpretedTreeAutomaton dull){
		for(Pair<String, Algebra> e : interpretations){
			dull.addInterpretation(new Interpretation(e.getRight(), null, e.getLeft()));

		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void addInt(List<Pair<String, Algebra>> list, String name, IntTypes t){
		switch(t){
			case STRING : list.add(new Pair(name, new StringAlgebra()));break;
			case GRAPH : list.add(new Pair(name, new GraphAlgebra()));break;
			case TREE : list.add(new Pair(name, new BinarizingTreeWithAritiesAlgebra()));break;
		}
	}
	
	
	
	/*
	public static void main(String[] args){
		@SuppressWarnings("rawtypes")
		List<Pair<String, Algebra>> l  = new ArrayList<Pair<String, Algebra>>();
		addInt(l, "string", IntTypes.STRING);
		addInt(l, "tree", IntTypes.TREE);
		addInt(l, "graph", IntTypes.GRAPH);
		
		for(@SuppressWarnings("rawtypes") Pair p : l){
			System.err.println(p.getLeft());
			System.err.println(p.getRight());
			System.err.println();
		}
		try {
			Corpus test = retrieveAltoCorpus(args[0], l);
			for(Instance i : test){
				System.err.println(i);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CorpusReadingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
		
		}
		*/
	
	/**
	 * Assumes the character stream returns one index per line, in increasing order of indices in the Stream.
	 * @param in
	 * @return
	 * @throws IOException 
	 */
	public static IntIterable retrieveIndices(Reader in) throws IOException{
		
		BufferedReader br = new BufferedReader(in);
		
		SimpleIntDeque list = new SimpleIntDeque();
		
		br.lines().forEach( (String line) -> list.add(Integer.parseInt(line.trim())) );
		
		br.close();;
		
		return list;
		
		
	}
	
	/**
	 * 
	 * @param in
	 * @param indices, an Iterable of integer denoting indices of bad instances to remove from a Corpus, in increasing order.
	 * @return
	 */
	public static Corpus removeIndices(Corpus in, IntIterable indices){
		Corpus out = new Corpus();
		
		int k = 0;
		int index = -1;
		IntIterator indicesIt = indices.iterator();
		
		for(Instance i: in){
			
			if(k > index && indicesIt.hasNext()){
				index = indicesIt.nextInt();
			}
			
			if(k == index){
					System.err.println("Removing instance #"+k);
				}else{
					out.addInstance(i);
				}
				k++;
			}
			return out;
		}
	
	
	
	/**
	 * 
	 * Convert a corpus (with derivation trees) into a finite IRTG suitable for exploring the different derivations with the Alto GUI.
	 * @param derivations
	 * @param g the IRTG underlying the input corpus (of which every instance is a derivation). g must be such that it's core automaton can accept a given derivation in only one way (note: this is not a restriction of expresseviness for IRTGs). 
	 * @param makeTerminal
	 * @return
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static InterpretedTreeAutomaton asFiniteGrammar(Corpus derivations, InterpretedTreeAutomaton g, Predicate<String> makeTerminal){

		TreeAutomaton<String> originalA = g.getAutomaton();
		ConcreteTreeAutomaton<String> finiteA = new ConcreteTreeAutomaton<>(new Signature());
		
		Map<String, Interpretation<?>> interpretations = new Object2ObjectArrayMap<String, Interpretation<?>>(g.getInterpretations().size());
		for(Interpretation<?> interpretation : g.getInterpretations().values())
			interpretations.put(interpretation.name, new Interpretation(interpretation.getAlgebra(), new Homomorphism(finiteA.getSignature(), interpretation.getHomomorphism().getTargetSignature()), interpretation.name));
		
		int derivationIndex = 0;
		
		for(Instance i : derivations){
			Tree<Integer> derivation = i.getDerivationTree();
			Tree<Rule> ruleTree;
			try {
				ruleTree = originalA.getRuleTree(derivation);
			} catch (Exception e) {
				System.err.println("Input grammar has more than one Rule Tree for the input tree: "+derivation);
				throw new IllegalArgumentException();
			}
			
			
			//traversal of the rule tree (breadth first here, but doesn't matter). Not using the generic dfs method because there is a need to inform children node on their position within their siblings. 
			LinkedList<Tree<Rule>> nodes = new LinkedList<>();
			nodes.add(ruleTree);
			SimpleIntDeque nodeIndices = new SimpleIntDeque();
			nodeIndices.add(0);
			
			while(!nodes.isEmpty()){
				
				Tree<Rule> node = nodes.remove();
				int parentIndex = nodeIndices.removeFirstInt();
			
			
				Rule top = node.getLabel();
				List<Tree<Rule>> childrenRules = node.getChildren();
			
				ArrayList<String> children = new ArrayList<>(childrenRules.size());
				Iterator<Tree<Rule>> childrenIt = childrenRules.iterator();
				int[] rawChildren = top.getChildren();
			
				for(int k=0;k<rawChildren.length;k++){
					nodes.addLast(childrenIt.next());
				
					int childIndex = parentIndex*10+k+1;
					nodeIndices.addLast(childIndex);
				
					children.add(originalA.getStateForId(rawChildren[k])+"_"+derivationIndex+"_"+childIndex);
				}
			
				//make specific version of the current rule
				Rule specific = finiteA.createRule(originalA.getStateForId(top.getParent())+"_"+derivationIndex+"_"+parentIndex, originalA.getSignature().resolveSymbolId(top.getLabel())+"_"+derivationIndex, children);
				finiteA.addRule(specific);	
				
				//update interpretations
				for(Entry<String, Interpretation<?>> entry :interpretations.entrySet()) entry.getValue().getHomomorphism().add(specific.getLabel(), g.getInterpretation(entry.getKey()).getHomomorphism().get(top.getLabel()));
				
				//mark parent nonterminal as sarting nomterminal if needed
				if(makeTerminal.test(originalA.getStateForId(top.getParent()))){
					finiteA.addFinalState(specific.getParent());
				}
				
				
			}
			derivationIndex++;		
		}
		
		
		InterpretedTreeAutomaton out = new InterpretedTreeAutomaton(finiteA);
		out.addAllInterpretations(interpretations);
		
		return out;
	}
	
	public static Predicate<String> keepSameStartingNonTerminals(TreeAutomaton<String> a){
		return (String s) -> a.getFinalStates().contains(a.getIdForState(s));
	}
	
	

}
