/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.align.find_rules.sampling;

import de.up.ling.irtg.rule_finding.create_automaton.HomomorphismManager;
import de.up.ling.irtg.automata.Rule;
import de.up.ling.irtg.hom.Homomorphism;
import de.up.ling.irtg.hom.HomomorphismSymbol;
import de.up.ling.tree.Tree;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author christoph_teichmann
 */
public class SplitIndicatedImage implements RuleTreeConverter<String> {

    /**
     * 
     */
    private final HomomorphismManager hm;

    /**
     * 
     */
    private final String marker;
    
    
    /**
     * 
     * @param hm 
     * @param marker 
     */
    public SplitIndicatedImage(HomomorphismManager hm, String marker) {
        this.hm = hm;
        this.marker = marker;
    }
    
    @Override
    public String convert(List<Tree<Rule>> input) {
        StringBuilder sb = new StringBuilder();
        
        for(int i=0;i<input.size();++i){
            Tree<Rule> t = input.get(i);
            if(i != 0){
                sb.append("\n");
                sb.append(this.marker);
                sb.append("\n");
            }
            
            sb.append(transform(t, this.hm.getHomomorphism1(), new AtomicInteger(0)));
            sb.append("\n");
            sb.append(transform(t, this.hm.getHomomorphism2(), new AtomicInteger(0)));
        }
        
        return sb.toString();
    }

    /**
     * 
     * @param t
     * @param homomorphism1
     * @return 
     */
    private String transform(Tree<Rule> t, Homomorphism homomorphism1, AtomicInteger ai) {
        int name = t.getLabel().getLabel();
        
        StringBuilder sb = new StringBuilder();
        Tree<HomomorphismSymbol> hom = homomorphism1.get(name);
        
        if(hom.getLabel().isVariable()){
            int pos = hom.getLabel().getValue();
            
            return this.transform(t.getChildren().get(pos), homomorphism1, ai);
        }
        
        
        if(hm.isVariable(name)){
            int i = ai.incrementAndGet();
            sb.append("'X_").append(i).append("'");
            ai = new AtomicInteger(0);
        }else{
            sb.append("'").append(homomorphism1.getTargetSignature().resolveSymbolId(hom.getLabel().getValue()))
                    .append("'");   
        }
        
        if(!hom.getChildren().isEmpty()){
            sb.append("(");
            
            List<String> l = new ArrayList<>();
            for(int i=0;i<t.getChildren().size();++i){
                Tree<Rule> tr = t.getChildren().get(i);
                
                l.add(this.transform(tr, homomorphism1, ai));
            }
            
            
            for(int i=0;i<hom.getChildren().size();++i){
                int pos = hom.getChildren().get(i).getLabel().getValue();
                
                sb.append(l.get(pos));
            }
            
            sb.append(")");
        }
        
        
        return sb.toString();
    }
}
