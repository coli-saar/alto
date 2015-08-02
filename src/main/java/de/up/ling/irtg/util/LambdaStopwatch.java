/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import static de.up.ling.irtg.util.Util.cputime;
import static de.up.ling.irtg.util.Util.formatTime;
import java.io.PrintStream;
import java.util.function.Supplier;

/**
 *
 * @author koller
 */
public class LambdaStopwatch {
    private PrintStream ps;

    public LambdaStopwatch(PrintStream ps) {
        this.ps = ps;
    }

    public <E> E t(String description, Supplier<E> fn) {
        if (ps == null) {
            return fn.get();
        } else {
            long start = cputime();
            E val = fn.get();
            long end = cputime();

            if (description != null) {
                ps.println(description + ": " + formatTime(end - start));
            }

            return val;
        }
    }
}
