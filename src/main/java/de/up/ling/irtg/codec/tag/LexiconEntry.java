/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec.tag;

import de.saar.coli.featstruct.AvmFeatureStructure;
import de.saar.coli.featstruct.FeatureStructure;
import java.util.Map;
import java.util.Objects;

/**
 *
 * @author koller
 */
public class LexiconEntry {
    private String word;
    private String elementaryTreeName;
    private FeatureStructure features;
    private String secondaryLex;
    
    public LexiconEntry(String word, String elementaryTreeName, FeatureStructure fs) {
        this.word = word;
        this.elementaryTreeName = elementaryTreeName;
        this.features = fs;
    }

    public LexiconEntry(String word, String elementaryTreeName) {
        this(word, elementaryTreeName, new AvmFeatureStructure());
    }

    public String getWord() {
        return word;
    }

    public String getElementaryTreeName() {
        return elementaryTreeName;
    }
    
    public void setFeatureStructure(FeatureStructure fs) {
        this.features = fs;
    }
    
    public FeatureStructure getFeatureStructure() {
        return features;
    }

    public String getSecondaryLex() {
        return secondaryLex;
    }

    public void setSecondaryLex(String secondaryLex) {
        this.secondaryLex = secondaryLex;
    }
    
    

    @Override
    public String toString() {
        return word + ":" + elementaryTreeName + features.toString();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.word);
        hash = 47 * hash + Objects.hashCode(this.elementaryTreeName);
        hash = 47 * hash + Objects.hashCode(getSecondaryLex());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final LexiconEntry other = (LexiconEntry) obj;
        if (!Objects.equals(this.word, other.word)) {
            return false;
        }
        if (!Objects.equals(this.elementaryTreeName, other.elementaryTreeName)) {
            return false;
        }
        
        if( ! Objects.equals(getSecondaryLex(), other.getSecondaryLex()) ) {
            return false;
        }
        
        return true;
    }
    
    
    
    
    
}
