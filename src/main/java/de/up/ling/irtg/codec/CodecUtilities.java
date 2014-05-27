/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import org.antlr.v4.runtime.RuleContext;

/**
 *
 * @author koller
 */
public class CodecUtilities {
    public static String stripOuterChars(String s) {
        assert s.length() >= 2 : "string -" + s + "- should have length at least 2";
        return s.substring(1, s.length() - 1);
    }
    
    public static String extractName(RuleContext context, boolean isQuoted) {
        if (isQuoted) {
            String s = context.getText();
            return stripOuterChars(s);
        } else {
            return context.getText();
        }
    }
}
