/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author koller
 */
public class BuildProperties {
    private static final Properties props = new Properties();
    
    private static final String IMPL_VERSION = "implementation-version";
    private static final String IMPL_BUILD = "implementation-build";
    
//    private static BuildProperties sing = new BuildProperties();
    
    static {
        try {
            props.load(new FileReader("build.properties"));
        } catch (IOException ex) {
            props.put(IMPL_VERSION, "(undefined)");
            props.put(IMPL_BUILD, "(undefined)");
        }
    }
    
    public static String getVersion() {
        return (String) props.get(IMPL_VERSION);
    }
    
    public static String getBuild() {
        return (String) props.get(IMPL_BUILD);
    }
    
}
