/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import com.google.common.base.Strings;
import java.io.PrintStream;

/**
 *
 * @author koller
 */
public class ConsoleProgressBar {

    private int barWidth;
    private PrintStream strm;
    private int previousPosbar = -1;
    private String previousString = null;

    public ConsoleProgressBar(int barWidth, PrintStream strm) {
        this.barWidth = barWidth - 2; // leave room for [ ]
        this.strm = strm;
    }

    public void update(long current, long max, String str) {
        int posbar = (int) ((barWidth * current) / max);

        if (posbar != previousPosbar || !str.equals(previousString)) {
            previousPosbar = posbar;

            StringBuffer buf = new StringBuffer("\r[");
            buf.append(Strings.repeat("=", posbar));
            buf.append(Strings.repeat("-", barWidth - posbar));
            buf.append("] ");
            buf.append(str);
            strm.print(buf);
        }
    }

    public void finish() {
        strm.println();
    }
    
    public ProgressListener createListener() {
        return (current, max, str) -> {
            ConsoleProgressBar.this.update(current, max, str);
        };
    }
}
