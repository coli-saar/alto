/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec.bolinas_hrg;

/**
 *
 * @author christoph_teichmann
 */
class StringSource {
    /**
     * 
     */
    private final String base;
    
    /**
     * 
     */
    private long num = 0;
    
    /**
     * 
     */
    public StringSource()
    {
        this("NULL");
    }
    
    /**
     * 
     * @param base 
     */
    public StringSource(String base)
    {
        this.base = base;
    }
    
    /**
     * 
     * @return 
     */
    public String get()
    {
        return base+"_"+this.num++;
    }
}
