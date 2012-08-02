/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.binarization;

import de.saar.basic.Pair;
import de.saar.basic.StringOrVariable;
import de.saar.penguin.irtg.automata.ConcreteTreeAutomaton;
import de.saar.penguin.irtg.automata.Rule;
import de.saar.penguin.irtg.automata.TreeAutomaton;
import de.saar.penguin.irtg.hom.Homomorphism;
import de.up.ling.tree.Tree;
import java.util.*;

/**
 *
 * @author koller
 */
public class SynchronousBinarization<E, F> {
    public static final String VARIABLE_MARKER = "?";
    public static final StringOrVariable CONSTANTL = new StringOrVariable("y",false); // das kann so nicht bleiben
    public static final StringOrVariable CONSTANTR = new StringOrVariable("z",false);
    //private E leftDummy;
    //private F rightDummy;
    private int nextGensym;
    private Map<Item,Set<Item>> itemCombinations;
    private Set<CItem> cChart;
    private Set<LItem> leftChart;
    private Set<RItem> rightChart;
    private Queue<Item> agenda;
    private TreeAutomaton<E> leftAuto;
    private TreeAutomaton<F> rightAuto;
    private Rule grammarRule;
    private ConcreteTreeAutomaton<String> outputAutomaton;
    private Homomorphism leftHomOut;
    private Homomorphism rightHomOut;

    // eigentlich: ConcreteTreeAutomaton<Pair<E, F>> outputAutomaton
    public void binarize(Rule grammarRule, TreeAutomaton<E> leftAuto, TreeAutomaton<F> rightAuto, ConcreteTreeAutomaton<String> outputAutomaton, Homomorphism leftHomOut, Homomorphism rightHomOut) {
        cChart = new HashSet<CItem>();
        leftChart = new HashSet<LItem>();
        rightChart = new HashSet<RItem>();
        agenda = new LinkedList<Item>();
        nextGensym = 1;
        itemCombinations = new HashMap<Item,Set<Item>>();        

        this.leftAuto = leftAuto;
        this.rightAuto = rightAuto;
        this.grammarRule = grammarRule;
        this.outputAutomaton = outputAutomaton;
        this.leftHomOut = leftHomOut;
        this.rightHomOut = rightHomOut;
        
        // select a constant as dummy symbol from both algebras
        // ...
        

        // initialize agenda and charts with (Var), (InitC), (InitL) and (InitR)
        for (Rule<E> leftRule : leftAuto.getRuleSet()){
            if (leftRule.getArity() == 0){
                String label = leftRule.getLabel();
                if (label.startsWith(VARIABLE_MARKER)) {
                    // combine variable with corresponding variable(s)
                    for (Rule<F> rightRule : rightAuto.getRulesBottomUp(label, new ArrayList())) {
                        var(leftRule,rightRule);
                    }
                } else { 
                    // combine left constant with every right constant
                    for (Rule<F> rightRule: rightAuto.getRuleSet()) {
                        if (rightRule.getArity() == 0 && !rightRule.getLabel().startsWith(VARIABLE_MARKER)) {
                            initC(leftRule,rightRule);
                        }
                    }
                    initL(leftRule);                  
                }               
            }
        }
        
        for (Rule<F> rightRule : rightAuto.getRuleSet()){
            if (rightRule.getArity() == 0 && !rightRule.getLabel().startsWith(VARIABLE_MARKER)){
                initR(rightRule);
            }
        }
        
        while (!agenda.isEmpty()) {
            Item item = agenda.remove();
            
            if (item instanceof SynchronousBinarization.CItem) {
                CItem itemC = (CItem) item;
                leftC(itemC);
                rightC(itemC);
                for (LItem other : new ArrayList<LItem>(leftChart)) {
                    if (isNewCombination(itemC,other)) {
                        cl(itemC, other);
                        lc(other, itemC);
                    }
                }
                for (RItem other : new ArrayList<RItem>(rightChart)) {
                    if (isNewCombination(itemC,other)) {
                        cr(itemC, other);
                        rc(other, itemC); 
                    }
                }
                for (CItem other : new ArrayList<CItem>(cChart)) {
                    if (isNewCombination(itemC,other)) {                    
                        cc(itemC, other); 
                        ccRev(itemC, other);
                        cc(other, itemC); 
                        ccRev(other, itemC);
                    }
                }       
            } else if (item instanceof SynchronousBinarization.LItem) {
                LItem itemL = (LItem) item;
                ruleL(itemL);
                for (LItem other : new ArrayList<LItem>(leftChart)) {
                    if (isNewCombination(itemL,other)) {
                        ll(itemL, other);
                        ll(other, itemL);
                    }
                }
                for (CItem other : new ArrayList<CItem>(cChart)) { 
                    if (isNewCombination(itemL,other)) {                    
                        lc(itemL, other);
                        cl(other, itemL);
                    }
                }              
            } else if (item instanceof SynchronousBinarization.RItem){   
                RItem itemR = (RItem) item;
                ruleR(itemR);
                for (RItem other : new ArrayList<RItem>(rightChart)) {
                    if (isNewCombination(itemR,other)) {
                        rr(itemR, other);
                        rr(other, itemR);
                    }
                }
                for (CItem other : new ArrayList<CItem>(cChart)) {
                    if (isNewCombination(itemR,other)) {                    
                        rc(itemR, other);
                        cr(other, itemR);
                    }
                }
            }
        }
        
        // add rules for parent state of original rule if both states in CItem are final states
        for (CItem itemC : cChart) {
            if (leftAuto.getFinalStates().contains(itemC.leftState) && rightAuto.getFinalStates().contains(itemC.rightState)) {
                List ruleChildren = makeChildrenList(itemC.getSymbolInRule());
                Tree<StringOrVariable> leftHomTree = Tree.create(new StringOrVariable("?1", true));
                Tree<StringOrVariable> rightHomTree = Tree.create(new StringOrVariable("?1", true)); 
                outputRule((String)grammarRule.getParent(), ruleChildren, leftHomTree, rightHomTree);
            }
        }
    }

        
    private void var(Rule<E> leftRule, Rule<F> rightRule) {
        CItem item = new CItem(leftRule.getParent(),rightRule.getParent());
        addItem(item,cChart);

        int index = Homomorphism.getIndexForVariable(new StringOrVariable(leftRule.getLabel(),true));
        List ruleChildren = makeChildrenList(grammarRule.getChildren()[index]);
        
        Tree<StringOrVariable> leftHomTree = Tree.create(new StringOrVariable("?1", true));
        Tree<StringOrVariable> rightHomTree = Tree.create(new StringOrVariable("?1", true));    
        outputRule(item.getSymbolInRule(), ruleChildren, leftHomTree, rightHomTree);
    }
    
    private void initC(Rule<E> leftRule, Rule<F> rightRule) {
        CItem item = new CItem(leftRule.getParent(),rightRule.getParent());
        addItem(item,cChart);

        Tree<StringOrVariable> leftHomTree = Tree.create(new StringOrVariable(leftRule.getLabel(),false));
        Tree<StringOrVariable> rightHomTree = Tree.create(new StringOrVariable(rightRule.getLabel(),false));
        outputRule(item.getSymbolInRule(), new ArrayList(), leftHomTree, rightHomTree);
    }
    
    private void initL(Rule<E> leftRule){
        LItem item = new LItem(leftRule.getParent());
        addItem(item,leftChart);

        Tree<StringOrVariable> leftHomTree = Tree.create(new StringOrVariable(leftRule.getLabel(),false));
        Tree<StringOrVariable> rightHomTree = Tree.create(CONSTANTR);
        outputRule(item.getSymbolInRule(), new ArrayList(), leftHomTree, rightHomTree);
    }
    
    private void initR(Rule<F> rightRule){
        RItem item = new RItem(rightRule.getParent());
        addItem(item,rightChart);

        Tree<StringOrVariable> leftHomTree = Tree.create(CONSTANTL);            
        Tree<StringOrVariable> rightHomTree = Tree.create(new StringOrVariable(rightRule.getLabel(),false));
        outputRule(item.getSymbolInRule(), new ArrayList(), leftHomTree, rightHomTree);
    }  
    
    
    private void ruleL(LItem oldItem) {
        for (String label : leftAuto.getAllLabels()) { 
            for (Rule<E> rule : leftAuto.getRulesBottomUp(label, makeChildrenList(oldItem.state))) {
                LItem item = new LItem(rule.getParent());
                addItem(item,leftChart);
                
                List ruleChildren = makeChildrenList(oldItem.getSymbolInRule());
                Tree<StringOrVariable> leftHomTree = makeHomTree(label);
                Tree<StringOrVariable> rightHomTree = Tree.create(CONSTANTR);
                outputRule(item.getSymbolInRule(), ruleChildren, leftHomTree, rightHomTree);
            }
        }
    }
    
    private void ruleR(RItem oldItem) {
        for (String label : rightAuto.getAllLabels()) { 
            for (Rule<F> rule : rightAuto.getRulesBottomUp(label, makeChildrenList(oldItem.state))) {        
                RItem item = new RItem(rule.getParent());
                addItem(item,rightChart);

                List ruleChildren = makeChildrenList(oldItem.getSymbolInRule());
                Tree<StringOrVariable> leftHomTree = Tree.create(CONSTANTL);            
                Tree<StringOrVariable> rightHomTree = makeHomTree(label); 
                outputRule(item.getSymbolInRule(), ruleChildren, leftHomTree, rightHomTree);
            }
        }
    }
    
    
    private void leftC(CItem oldItem) {
        for (String label : leftAuto.getAllLabels()) { 
            for (Rule<E> rule : leftAuto.getRulesBottomUp(label, makeChildrenList(oldItem.leftState))) {
                CItem item = new CItem(rule.getParent(),oldItem.rightState);
                addItem(item,cChart);

                List ruleChildren = makeChildrenList(oldItem.getSymbolInRule());
                Tree<StringOrVariable> leftHomTree = makeHomTree(label);
                Tree<StringOrVariable> rightHomTree = Tree.create(new StringOrVariable("?1", true));
                outputRule(item.getSymbolInRule(), ruleChildren, leftHomTree, rightHomTree);
            }
        }
    } 
      
    private void rightC(CItem oldItem) {
        for (String label : rightAuto.getAllLabels()) { 
            for (Rule<F> rule : rightAuto.getRulesBottomUp(label, makeChildrenList(oldItem.rightState))) {
                CItem item = new CItem(oldItem.leftState,rule.getParent());
                addItem(item,cChart);

                List ruleChildren = makeChildrenList(oldItem.getSymbolInRule());
                Tree<StringOrVariable> leftHomTree = Tree.create(new StringOrVariable("?1", true));
                Tree<StringOrVariable> rightHomTree = makeHomTree(label);
                outputRule(item.getSymbolInRule(),ruleChildren, leftHomTree, rightHomTree);
            }
        }
    }    

    private void ll(LItem item1, LItem item2){
        List children = makeChildrenList(item1.state,item2.state);
        for (String label: leftAuto.getAllLabels()) {
            for (Rule<E> rule : leftAuto.getRulesBottomUp(label,children)) {
                LItem item = new LItem(rule.getParent());
                addItem(item,leftChart);

                List ruleChildren = makeChildrenList(item1.getSymbolInRule(),item2.getSymbolInRule());
                Tree<StringOrVariable> leftHomTree = makeHomTree(label,false);
                Tree<StringOrVariable> rightHomTree = Tree.create(CONSTANTR);  
                outputRule(item.getSymbolInRule(), ruleChildren, leftHomTree, rightHomTree);
            }
        }
    }
    
    private void rr(RItem item1, RItem item2){
        List children = makeChildrenList(item1.state,item2.state);
        for (String label: rightAuto.getAllLabels()) {
            for (Rule<F> rule : rightAuto.getRulesBottomUp(label,children)) {
                RItem item = new RItem(rule.getParent());
                addItem(item,rightChart);

                List ruleChildren = makeChildrenList(item1.getSymbolInRule(),item2.getSymbolInRule());
                Tree<StringOrVariable> leftHomTree = Tree.create(CONSTANTL);                     
                Tree<StringOrVariable> rightHomTree = makeHomTree(label,false);
                outputRule(item.getSymbolInRule(), ruleChildren, leftHomTree, rightHomTree);
            }
        }
    }   
    
    private void cl(CItem itemC, LItem itemL) {
        cl(itemC,itemL,false);
    }
    
    private void lc(LItem itemL, CItem itemC) {
        cl(itemC,itemL,true);
    }
    
    private void cl(CItem itemC, LItem itemL, boolean reverse) {
        List children = makeChildrenList(itemC.leftState,itemL.state,reverse);
        for (String label: leftAuto.getAllLabels()) {
            for (Rule<E> rule : leftAuto.getRulesBottomUp(label,children)) {
                CItem item = new CItem(rule.getParent(),itemC.rightState);
                addItem(item,cChart);

                List ruleChildren = makeChildrenList(itemC.getSymbolInRule(),itemL.getSymbolInRule());
                Tree<StringOrVariable> leftHomTree = makeHomTree(label,reverse);                     
                Tree<StringOrVariable> rightHomTree = Tree.create(new StringOrVariable("?1", true));
                outputRule(item.getSymbolInRule(), ruleChildren, leftHomTree, rightHomTree);
            }
        }
    }
    

    private void cr(CItem itemC, RItem itemR) {
        cr(itemC,itemR,false);
    }
    
    private void rc(RItem itemR, CItem itemC) {
        cr(itemC,itemR,true);

    }
    
    private void cr(CItem itemC, RItem itemR, boolean reverse) {
        List children = makeChildrenList(itemC.rightState,itemR.state,reverse);
        for (String label: rightAuto.getAllLabels()) {
            for (Rule<F> rule : rightAuto.getRulesBottomUp(label,children)) {
                CItem item = new CItem(itemC.leftState,rule.getParent());
                addItem(item,cChart);

                List ruleChildren = makeChildrenList(itemC.getSymbolInRule(),itemR.getSymbolInRule());
                Tree<StringOrVariable> leftHomTree = Tree.create(new StringOrVariable("?1", true));                    
                Tree<StringOrVariable> rightHomTree = makeHomTree(label,reverse);                     
                outputRule(item.getSymbolInRule(), ruleChildren, leftHomTree, rightHomTree);
            }
        }
    }   
    
    private void cc(CItem item1, CItem item2) {
        cc(item1, item2, false);
    }
    
    private void ccRev(CItem item1, CItem item2) {
        cc(item1, item2, true);
    }
     
    private void cc(CItem item1, CItem item2, boolean reverse) {
        List leftChildren = makeChildrenList(item1.leftState,item2.leftState);
        List rightChildren = makeChildrenList(item1.rightState,item2.rightState,reverse);
        for (String leftLabel : leftAuto.getAllLabels()) {
            for (Rule<E> leftRule : leftAuto.getRulesBottomUp(leftLabel,leftChildren)){
                for (String rightLabel : rightAuto.getAllLabels()) {
                    for (Rule<F> rightRule : rightAuto.getRulesBottomUp(rightLabel, rightChildren)) {
                        CItem item = new CItem(leftRule.getParent(),rightRule.getParent());
                        addItem(item, cChart);
                        
                        List ruleChildren = makeChildrenList(item1.getSymbolInRule(), item2.getSymbolInRule());
                        Tree<StringOrVariable> leftHomTree = makeHomTree(leftLabel,false);                    
                        Tree<StringOrVariable> rightHomTree = makeHomTree(rightLabel,reverse);                     
                        outputRule(item.getSymbolInRule(), ruleChildren, leftHomTree, rightHomTree);
                    }
                }
            }
        }
    }
    
    
    private boolean addItem(Item item, Set chart) {
        if (chart.add(item)) {
            agenda.add(item);
            return true;
        }
        
        return false;
    }

    // true if items have not been combined before
    // (regardless of order)
    private boolean isNewCombination(Item item1, Item item2) {
        boolean i1i2New;
        boolean i2i1New;
        
        if (itemCombinations.containsKey(item1)){
            i1i2New = itemCombinations.get(item1).add(item2);
        } else {
            Set<Item> newItem1Set = new HashSet<Item>();
            newItem1Set.add(item2);
            itemCombinations.put(item1, newItem1Set);
            i1i2New = true;
        }
        
        if (itemCombinations.containsKey(item2)){
            i2i1New = itemCombinations.get(item2).add(item1);
        } else { 
            i2i1New = true; 
        }
        
        return (i1i2New && i2i1New);
    }
    
    private void outputRule(String parent, List children, Tree<StringOrVariable> leftHomTree, Tree<StringOrVariable> rightHomTree) {
        Rule<String> newRule = new Rule<String>(parent, gensym(), children);
        
        outputAutomaton.addRule(newRule);
        leftHomOut.add(newRule.getLabel(), leftHomTree);                            
        rightHomOut.add(newRule.getLabel(), rightHomTree);        
    }
    
    private List makeChildrenList(Object o1, Object o2, boolean reverse) {
        if (reverse) {
            return makeChildrenList(o2, o1);
        } else {
            return makeChildrenList(o1, o2);
        }
    }
    
    private List makeChildrenList(Object o1, Object o2) {
        List children = new ArrayList(2);
        
        children.add(o1);
        children.add(o2);
        
        return children;
    } 
    
    private List makeChildrenList(Object o) {
        List childAsList = new ArrayList(1);
        
        childAsList.add(o);
        
        return childAsList;
    }
    
    private Tree makeHomTree(String label){     // unary
        StringOrVariable treeLabel = new StringOrVariable(label,false);
        List children = makeChildrenList(Tree.create(new StringOrVariable("?1", true)));
        Tree homTree = Tree.create(treeLabel,children);
        return homTree;
    }
    
    // ist es richtig, dass Homomorphismus-Terme vom Typ <StringOrVariable> sein müssen?
    private Tree makeHomTree(String label, boolean reverse) { // binary
        StringOrVariable treeLabel = new StringOrVariable(label,false);
        Tree<StringOrVariable> firstVarTree = Tree.create(new StringOrVariable("?1", true));
        Tree<StringOrVariable> secondVarTree = Tree.create(new StringOrVariable("?2", true));
        List children = makeChildrenList(firstVarTree,secondVarTree,reverse);
        Tree homTree = Tree.create(treeLabel,children);
        return homTree;        
    }
    
    private String gensym() {
        return grammarRule.getLabel() + "b" + (nextGensym++);
    }
    
    
    private interface Item {
       // states in outputAutomaton are Strings
       public String getSymbolInRule(); 
    }

    private class CItem implements Item {

        E leftState;
        F rightState;

        public CItem(E leftState, F rightState) {
            this.leftState = leftState;
            this.rightState = rightState;
        }
        
        @Override
        public String getSymbolInRule() {
            return grammarRule.getLabel() + leftState.toString() + rightState.toString();
        }        

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CItem other = (CItem) obj;
            if (this.leftState != other.leftState && (this.leftState == null || !this.leftState.equals(other.leftState))) {
                return false;
            }
            if (this.rightState != other.rightState && (this.rightState == null || !this.rightState.equals(other.rightState))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + (this.leftState != null ? this.leftState.hashCode() : 0);
            hash = 23 * hash + (this.rightState != null ? this.rightState.hashCode() : 0);
            return hash;
        }
    }

    private class LItem implements Item {

        E state;

        public LItem(E state) {
            this.state = state;
        }
        
        @Override
        public String getSymbolInRule() {
            return grammarRule.getLabel() + "L" + state.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LItem other = (LItem) obj;
            if (this.state != other.state && (this.state == null || !this.state.equals(other.state))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 11 * hash + (this.state != null ? this.state.hashCode() : 0);
            return hash;
        }
    }

    private class RItem implements Item {

        F state;

        public RItem(F state) {
            this.state = state;
        }
        
        @Override
        public String getSymbolInRule() {
            return grammarRule.getLabel() + "R" + state.toString();
        }        

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RItem other = (RItem) obj;
            if (this.state != other.state && (this.state == null || !this.state.equals(other.state))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 97 * hash + (this.state != null ? this.state.hashCode() : 0);
            return hash;
        }
    }
}
