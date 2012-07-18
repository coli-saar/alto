/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.penguin.irtg.binarization;

import com.google.common.base.Predicate;
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
    public static final String VARIABLE_MARKER = "?"; //nich sicher, ob ich das brauche
    public static final String PLACEHOLDER = ".";
    private int nextGensym;
    private Set<CorrespondenceItem> correspondenceChart;
    private Set<LeftItem> leftChart;
    private Set<RightItem> rightChart;
    private Queue<Item> agenda;
    private TreeAutomaton<E> leftAuto;
    private TreeAutomaton<F> rightAuto;
    private Rule grammarRule;
    private ConcreteTreeAutomaton<String> outputAutomaton;
    private Homomorphism leftHomOut;
    private Homomorphism rightHomOut;

    // eigentlich: ConcreteTreeAutomaton<Pair<E, F>> outputAutomaton
    public void binarize(Rule grammarRule, TreeAutomaton<E> leftAuto, TreeAutomaton<F> rightAuto, ConcreteTreeAutomaton<String> outputAutomaton, Homomorphism leftHomOut, Homomorphism rightHomOut) {
        correspondenceChart = new HashSet<CorrespondenceItem>();
        leftChart = new HashSet<LeftItem>();
        rightChart = new HashSet<RightItem>();
        agenda = new LinkedList<Item>();

        this.leftAuto = leftAuto;
        this.rightAuto = rightAuto;
        this.grammarRule = grammarRule;
        this.outputAutomaton = outputAutomaton;
        this.leftHomOut = leftHomOut;
        this.rightHomOut = rightHomOut;
        nextGensym = 1;

        // initialize agenda and charts with (Var), (Const-L) and (Const-R)
        for (Rule<E> leftRule : leftAuto.getRuleSet()){
            if (leftRule.getArity() == 0){
                String label = leftRule.getLabel();
                if (label.startsWith(VARIABLE_MARKER)) {
                    for (Rule<F> rightRule : rightAuto.getRulesBottomUp(label, new ArrayList())) { //nicht schön
                        var(leftRule,rightRule);
                    }
                } else { 
                    constL(leftRule);             
                }               
            }
        }    
        for (Rule<F> rightRule : rightAuto.getRuleSet()){
            if (rightRule.getArity() == 0 && !rightRule.getLabel().startsWith(VARIABLE_MARKER)){
                constR(rightRule);
            }
        }
        
        while (!agenda.isEmpty()) {
            Item item = agenda.remove();

            if (item instanceof SynchronousBinarization.CorrespondenceItem) {
                CorrespondenceItem itemAsC = (CorrespondenceItem) item;
                //System.out.println("["+itemAsC.leftState+","+itemAsC.rightState+","+itemAsC.variables+"]");
                for (CorrespondenceItem other : new ArrayList<CorrespondenceItem>(correspondenceChart)) {
                    union(itemAsC, other);
                    unionReverse(itemAsC, other);
                }
                for (LeftItem other : new ArrayList<LeftItem>(leftChart)) {
                    binary1L(itemAsC, other);
                    binary2L(other, itemAsC);
                }
                for (RightItem other : new ArrayList<RightItem>(rightChart)) {
                    binary1R(itemAsC,other);
                    binary2R(other,itemAsC);
                }
                unaryL(itemAsC);
                unaryR(itemAsC);
                        
            } else if (item instanceof SynchronousBinarization.LeftItem) {
                LeftItem itemAsL = (LeftItem) item;
                //System.out.println("<"+itemAsL.state+">");
                for (CorrespondenceItem other : new ArrayList<CorrespondenceItem>(correspondenceChart)) {
                    binary1L(other, itemAsL);
                    binary2L(itemAsL, other);
                }
                for (LeftItem other : new ArrayList<LeftItem>(leftChart)) {
                    binaryConstL(itemAsL, other);
                }
                unaryConstL(itemAsL);
                
            } else if (item instanceof SynchronousBinarization.RightItem){
                RightItem itemAsR = (RightItem) item;
                //System.out.println("<<"+itemAsR.state+">>");            
                for (CorrespondenceItem other : new ArrayList<CorrespondenceItem>(correspondenceChart)) {
                    binary1R(other, itemAsR);
                    binary2R(itemAsR, other);
                }
                for (RightItem other : new ArrayList<RightItem>(rightChart)) {
                    binaryConstR(itemAsR, other);
                }
                unaryConstR(itemAsR);                           
            }
        }
    }

    private void union(CorrespondenceItem item, CorrespondenceItem other) { 
        union(item,other,false);
    }

    private void unionReverse(CorrespondenceItem item, CorrespondenceItem other) {
        union(item,other,true);
    }
        
    private void union(CorrespondenceItem item, CorrespondenceItem other, boolean reverse) { 
        List leftChildren = makeChildrenList(item.leftState,other.leftState); // (p1,p2)
        List rightChildren = makeChildrenList(item.rightState,other.rightState,reverse); // (q1,q2) oder (q2,q1)

        for (String leftLabel : leftAuto.getAllLabels()) { 
            for (Rule<E> leftRule : leftAuto.getRulesBottomUp(leftLabel, leftChildren)) { 
                for (String rightLabel : rightAuto.getAllLabels()) { 
                    for (Rule<F> rightRule: rightAuto.getRulesBottomUp(rightLabel, rightChildren)){
                        Set<String> newVariableSet = new HashSet<String>(item.variables);
                        newVariableSet.addAll(other.variables);
                        CorrespondenceItem newItem = new CorrespondenceItem(leftRule.getParent(),rightRule.getParent(),newVariableSet);
                        
                        newItem.leftTree = Tree.create(PLACEHOLDER);
                        newItem.rightTree = Tree.create(PLACEHOLDER);
                        //newItem.leftPath = new ArrayList<Integer>();
                        //newItem.rightPath = new ArrayList<Integer>();
                        String newRuleLabel = gensym();
                        newItem.symbolInRule = newRuleLabel.toUpperCase();
                           
                        // add rule and homomorphism-trees for new items
                        if ( addItem(newItem,correspondenceChart) ) {
                            List ruleChildren = makeChildrenList(item.symbolInRule,other.symbolInRule);
                            Rule<String> newRule = new Rule<String>(newItem.symbolInRule,newRuleLabel,ruleChildren);
                            this.outputAutomaton.addRule(newRule);

                            Tree<StringOrVariable> leftHomTree = makeHomTree(item.leftTree,other.leftTree,leftLabel,false);
                            Tree<StringOrVariable> rightHomTree = makeHomTree(item.rightTree,other.rightTree,rightLabel,reverse);
                            leftHomOut.add(newRuleLabel, leftHomTree);                            
                            rightHomOut.add(newRuleLabel, rightHomTree);  
                            
                            // add rule for parent state of orignal rule if both states are final states
                            if (leftAuto.getFinalStates().contains(newItem.leftState) && rightAuto.getFinalStates().contains(newItem.rightState)) {
                                String newLabel = gensym();
                                this.outputAutomaton.addRule(newLabel, ruleChildren, (String) grammarRule.getParent());
                                leftHomOut.add(newLabel, leftHomTree);
                                rightHomOut.add(newLabel, rightHomTree);
                            }
                        }
                        //System.out.println("Union: "+newItem.leftState+" "+newItem.rightState+" "+newItem.variables+" : "+newItem.symbolInRule); 
                    }
                }
            }
        }
    }    
    
    private Tree makeHomTree(Tree tree1, Tree tree2, String label, boolean reverse) {   
        
        // ?!?  // aber dann brauche ich den Pfad ja gar nicht...
        Predicate<Tree> isOpenPosition = new Predicate<Tree>() {
            @Override
            public boolean apply(Tree tree) {
                return ( tree.getLabel().equals(PLACEHOLDER) );
            }
        };            
        Tree<StringOrVariable> firstVarTree = Tree.create(new StringOrVariable("?1", true));
        Tree<StringOrVariable> secondVarTree = Tree.create( new StringOrVariable("?2", true));             
        List HomChildren = makeChildrenList( tree1.substitute(isOpenPosition, firstVarTree),
                                             tree2.substitute(isOpenPosition, secondVarTree), reverse );
        return Tree.create(label, HomChildren);       
    }
    
    
    private void var(Rule<E> leftRule, Rule<F> rightRule){
        Set<String> varSet = new HashSet<String>();
        varSet.add(leftRule.getLabel());
        CorrespondenceItem newItem = new CorrespondenceItem(leftRule.getParent(),rightRule.getParent(),varSet);
        
        newItem.leftTree = Tree.create(PLACEHOLDER);
        newItem.rightTree = Tree.create(PLACEHOLDER);
        //newItem.leftPath = new ArrayList<Integer>();
        //newItem.rightPath = new ArrayList<Integer>();
        int index = Homomorphism.getIndexForVariable(new StringOrVariable(leftRule.getLabel(),true));
        newItem.symbolInRule = (String) grammarRule.getChildren()[index];      
        addItem(newItem,correspondenceChart);
        //System.out.println("Var: "+newItem.leftState+" "+newItem.rightState+" "+newItem.variables+" : "+newItem.symbolInRule); 
    }
    
    private void constL(Rule<E> leftRule){
        E leftState = leftRule.getParent();
        LeftItem newItem = new LeftItem(leftState);
        newItem.tree = Tree.create(leftRule.getLabel());
        addItem(newItem,leftChart);          
        //System.out.println("ConstL: "+newItem.state+" "+newItem.tree);
    }
    
    private void constR(Rule<F> rightRule){
        F rightState = rightRule.getParent();
        RightItem newItem = new RightItem(rightState);
        newItem.tree = Tree.create(rightRule.getLabel());
        addItem(newItem,rightChart);
        //System.out.println("ConstR: "+newItem.state+" "+newItem.tree);        
    }
    
    
    private void binary1L(CorrespondenceItem itemAsC, LeftItem other) {
        binaryL(itemAsC,other,true);
    }

    private void binary2L(LeftItem other, CorrespondenceItem itemAsC) {
        binaryL(itemAsC,other,false);
    }
      
    private void binaryL(CorrespondenceItem itemAsC, LeftItem other, boolean first) {
        List leftChildren = makeChildrenList(itemAsC.leftState,other.state, !first);

        for (String leftLabel: leftAuto.getAllLabels()) {
            for(Rule<E> leftRule : leftAuto.getRulesBottomUp(leftLabel, leftChildren)) {
                E leftState = leftRule.getParent();
                CorrespondenceItem newItem = new CorrespondenceItem(leftState,itemAsC.rightState,itemAsC.variables);
                
                List leftTreeChildren = makeChildrenList(itemAsC.leftTree,other.tree, !first);
                newItem.leftTree = Tree.create(leftLabel,leftTreeChildren);
                newItem.rightTree = itemAsC.rightTree; 
//                List leftPath = new ArrayList<Integer>();
//                if (first) {
//                    leftPath.add(0);                    
//                } else {
//                    leftPath.add(1);
//                }   
//                leftPath.addAll(itemAsC.leftPath);
//                newItem.leftPath = leftPath;                
//                newItem.rightPath = itemAsC.rightPath;
                newItem.symbolInRule = itemAsC.symbolInRule;                
                
                // add unary rule if both states are final states [incomplete]
                if ( addItem(newItem,correspondenceChart) && leftAuto.getFinalStates().contains(newItem.leftState) && rightAuto.getFinalStates().contains(newItem.rightState) ) {
                    String newLabel = gensym();
                    List ruleChildren = new ArrayList<String>(1);
                    ruleChildren.add(newItem.symbolInRule);
                    this.outputAutomaton.addRule(newLabel, ruleChildren, (String) grammarRule.getParent());  
                    //leftHomOut
                    //rightHomOut
                }
                //System.out.println("BinaryL: "+newItem.leftState+" "+newItem.rightState+" "+newItem.variables+" : "+newItem.symbolInRule);        
            }
        }
    }    
       
    private void binary1R(CorrespondenceItem itemAsC, RightItem other) {
        binaryR(itemAsC,other,true);
    }

    private void binary2R(RightItem other, CorrespondenceItem itemAsC) {
        binaryR(itemAsC,other,false);
    }
    
    private void binaryR(CorrespondenceItem itemAsC, RightItem other, boolean first) {
        List rightChildren = makeChildrenList(itemAsC.rightState,other.state, !first);

        for (String rightLabel: rightAuto.getAllLabels()) {
            for(Rule<F> rightRule : rightAuto.getRulesBottomUp(rightLabel, rightChildren)) {
                F rightState = rightRule.getParent();
                CorrespondenceItem newItem = new CorrespondenceItem(itemAsC.leftState,rightState,itemAsC.variables);
                
                List rightTreeChildren = makeChildrenList(itemAsC.rightTree,other.tree,!first);
                newItem.rightTree = Tree.create(rightLabel,rightTreeChildren);
                newItem.leftTree = itemAsC.leftTree; 
//                List rightPath = new ArrayList<Integer>();
//                if (first) {
//                    rightPath.add(0);                    
//                } else {
//                    rightPath.add(1);
//                }
//                rightPath.addAll(itemAsC.rightPath);
//                newItem.rightPath = rightPath;                
//                newItem.leftPath = itemAsC.leftPath;
                newItem.symbolInRule = itemAsC.symbolInRule;
             
                addItem(newItem,correspondenceChart);
                //System.out.println("BinaryR: "+newItem.leftState+" "+newItem.rightState+" "+newItem.variables+" : "+newItem.symbolInRule);            
            }
        }
    }   
    
    private void unaryL(CorrespondenceItem item){
        List children = new ArrayList();
        children.add(item.leftState);
        for (String label: leftAuto.getAllLabels()) {
            for (Rule<E> rule: leftAuto.getRulesBottomUp(label,children)){
                CorrespondenceItem newItem = new CorrespondenceItem(rule.getParent(),item.rightState,item.variables);
                
                List leftTreeChildren = new ArrayList<Tree<String>>(1);
                leftTreeChildren.add(item.leftTree);
                newItem.leftTree = Tree.create(label,leftTreeChildren);
                newItem.rightTree = item.rightTree;
                //List leftPath = new ArrayList<Integer>();                
                //leftPath.add(0);                                          
                //leftPath.addAll(item.leftPath);
                //newItem.leftPath = leftPath;                
               // newItem.rightPath = item.rightPath;
                newItem.symbolInRule = item.symbolInRule;                
                
                addItem(newItem,correspondenceChart);
                //System.out.println("UnaryL: "+newItem.leftState+" "+newItem.rightState+" "+newItem.variables+" : "+newItem.symbolInRule);              
            }
        }
    }
    
    private void unaryR(CorrespondenceItem item){
        List children = new ArrayList();
        children.add(item.rightState);
        for (String label: rightAuto.getAllLabels()) {
            for (Rule<F> rule: rightAuto.getRulesBottomUp(label,children)){
                CorrespondenceItem newItem = new CorrespondenceItem(item.leftState,rule.getParent(),item.variables);
                
                List rightTreeChildren = new ArrayList<Tree<String>>(1);
                rightTreeChildren.add(item.rightTree);
                newItem.rightTree = Tree.create(label,rightTreeChildren);
                //List rightPath = new ArrayList<Integer>();                
                //rightPath.add(0);                                           
                //rightPath.addAll(item.rightPath);
                //newItem.rightPath = rightPath;                
                //newItem.leftPath = item.leftPath;
                newItem.leftTree = item.leftTree;
                newItem.symbolInRule = item.symbolInRule;                   
                
                addItem(newItem,correspondenceChart);
                //.out.println("UnaryR: "+newItem.leftState+" "+newItem.rightState+" "+newItem.variables+" : "+newItem.symbolInRule); 
            }
        }        
    }    

    private void binaryConstL(LeftItem itemAsL, LeftItem other) {
        List children = makeChildrenList(itemAsL.state,other.state);
        for (String label : leftAuto.getAllLabels()){
            for (Rule<E> rule : leftAuto.getRulesBottomUp(label, children)) {
                LeftItem newItem = new LeftItem(rule.getParent());
                List treeChildren = makeChildrenList(itemAsL.tree,other.tree);
                newItem.tree = Tree.create(label,treeChildren);
                
                addItem(newItem,leftChart);
                //System.out.println("binaryConstL: "+newItem.state+" "+newItem.tree);                
            }
        }
    }
    
    private void binaryConstR(RightItem itemAsR, RightItem other){
        List children = makeChildrenList(itemAsR.state,other.state);
        for (String label : rightAuto.getAllLabels()){
            for (Rule<F> rule : rightAuto.getRulesBottomUp(label, children)) {
                RightItem newItem = new RightItem(rule.getParent());
                List treeChildren = makeChildrenList(itemAsR.tree,other.tree);
                newItem.tree = Tree.create(label,treeChildren);
                                
                addItem(newItem,rightChart);
                //System.out.println("binaryConstR: "+newItem.state+" "+newItem.tree);   
            }
        }
    }
    
    private void unaryConstL(LeftItem itemAsL){
        List children = new ArrayList(1);
        children.add(itemAsL.state);
        for (String label : leftAuto.getAllLabels()) {
            for (Rule<E> rule: leftAuto.getRulesBottomUp(label, children)) {
                LeftItem newItem = new LeftItem(rule.getParent());
                
                ArrayList<Tree<String>> treeChildren = new ArrayList<Tree<String>>(1);
                treeChildren.add(itemAsL.tree);
                newItem.tree = Tree.create(label,treeChildren);
                
                addItem(newItem,leftChart);
                //System.out.println("unaryConstL: "+newItem.state+" "+newItem.tree); 
            }
        }
    }
    
    private void unaryConstR(RightItem itemAsR){
        List children = new ArrayList(1);
        children.add(itemAsR.state);
        for (String label : rightAuto.getAllLabels()) {
            for (Rule<F> rule: rightAuto.getRulesBottomUp(label, children)) {
                RightItem newItem = new RightItem(rule.getParent());
                
                ArrayList<Tree<String>> treeChildren = new ArrayList<Tree<String>>(1);
                treeChildren.add(itemAsR.tree);
                newItem.tree = Tree.create(label,treeChildren);                
                
                addItem(newItem,rightChart);
                //System.out.println("unaryConstR: "+newItem.state+" "+newItem.tree); 
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
    
    private List makeChildrenList(Object state1, Object state2, boolean reverse) {
        if (reverse) {
            return makeChildrenList(state2, state1);
        } else {
            return makeChildrenList(state1, state2);
        }
    }
    
    private List makeChildrenList(Object state1, Object state2) {
        List children = new ArrayList(2);
        children.add(state1);
        children.add(state2);
        return children;
    }   

//    private String makeBinaryLabel(String ruleLabel, Set<String> variables) { 
//        String newRuleLabel = ruleLabel;
//        for (String var : variables) {
//            int index = Homomorphism.getIndexForVariable(new StringOrVariable(var,true));
//            String nonterminal = (String) grammarRule.getChildren()[index];  
//            newRuleLabel = newRuleLabel.concat(nonterminal);
//        }
//        while (outputAutomaton.getAllLabels().contains(newRuleLabel)) {
//            newRuleLabel = newRuleLabel + "b";
//        }        
//        return newRuleLabel;
//    }
    
    private String gensym() {
        return grammarRule.getLabel() + (nextGensym++);
    }
    
    
    private interface Item {
    }

    private class CorrespondenceItem implements Item {

        E leftState;
        F rightState;
        Set<String> variables;
        Tree leftTree;
        Tree rightTree;
        //List leftPath;
        //List rightPath;
        String symbolInRule;

        public CorrespondenceItem(E leftState, F rightState, Set<String> variables) {
            this.leftState = leftState;
            this.rightState = rightState;
            this.variables = variables;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CorrespondenceItem other = (CorrespondenceItem) obj;
            if (this.leftState != other.leftState && (this.leftState == null || !this.leftState.equals(other.leftState))) {
                return false;
            }
            if (this.rightState != other.rightState && (this.rightState == null || !this.rightState.equals(other.rightState))) {
                return false;
            }
            if (this.variables != other.variables && (this.variables == null || !this.variables.equals(other.variables))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 23 * hash + (this.leftState != null ? this.leftState.hashCode() : 0);
            hash = 23 * hash + (this.rightState != null ? this.rightState.hashCode() : 0);
            hash = 23 * hash + (this.variables != null ? this.variables.hashCode() : 0);
            return hash;
        }
    }

    private class LeftItem implements Item {

        E state;
        Tree<String> tree; // Tree in equals und hashCode berücksichtigen?

        public LeftItem(E state) {
            this.state = state;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final LeftItem other = (LeftItem) obj;
            if (this.state != other.state && (this.state == null || !this.state.equals(other.state))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 67 * hash + (this.state != null ? this.state.hashCode() : 0);
            return hash;
        }
    }

    private class RightItem implements Item {

        F state;
        Tree<String> tree;

        public RightItem(F state) {
            this.state = state;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final RightItem other = (RightItem) obj;
            if (this.state != other.state && (this.state == null || !this.state.equals(other.state))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + (this.state != null ? this.state.hashCode() : 0);
            return hash;
        }
    }
}
