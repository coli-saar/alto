/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing.geoquery;

import de.saar.basic.Pair;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 * @author christoph_teichmann
 */
public class SplitAndHandleUnknown {
    /**
     *
     * @param anti
     * @param mainSet
     * @param devSize
     * @param lines
     * @param train
     * @param devel
     * @param test
     * @param firstLine
     * @param secondLine
     * @param facts
     * @throws IOException
     */
    public void divideAndHandle(AntiUnknown anti, IntSet mainSet,
            int devSize, InputStream lines, OutputStream train, OutputStream devel, OutputStream test,
            int firstLine, int secondLine, Supplier<InputStream> facts)
            throws IOException {
        List<Pair<String, String>> main = getMain(lines, firstLine, secondLine);

        List<Pair<String, String>> tra = new ArrayList<>();
        List<Pair<String, String>> dev = new ArrayList<>();
        List<Pair<String, String>> tes = new ArrayList<>();

        int remDev = devSize;
        for (int i = 0; i < main.size(); ++i) {
            if (mainSet.contains(i)) {
                if (remDev > 0) {
                    dev.add(main.get(i));
                    --remDev;
                } else {
                    tra.add(main.get(i));
                }
            } else {
                tes.add(main.get(i));
            }
        }

        Iterable<Pair<String, String>> reducedDevelopment = anti.reduceUnknownWithoutFacts(dev);
        dump(reducedDevelopment, devel);

        Iterable<Pair<String, String>> reducedTest = anti.reduceUnknownWithoutFacts(tes);
        dump(reducedTest, test);

        Iterable<Pair<String, String>> reducedTraining = anti.reduceUnknownWithFacts(tra);
        dump(reducedTraining, train);
    }

    /**
     *
     * @param lines
     * @param firstLine
     * @param secondLine
     * @return
     * @throws IOException
     */
    private List<Pair<String, String>> getMain(InputStream lines, int firstLine,
                                        int secondLine) throws IOException {
        List<Pair<String, String>> main = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(lines))) {
            String line;
            int count = 0;

            String one = null;
            String two = null;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    count = 0;
                    if (one != null && two != null) {
                        main.add(new Pair<>(one, two));
                    }

                    one = null;
                    two = null;

                    continue;
                }

                if (count == firstLine) {
                    one = line;
                }

                if (count == secondLine) {
                    two = line;
                }

                ++count;
            }

            if (one != null && two != null) {
                main.add(new Pair<>(one, two));
            }
        }
        
        return main;
    }

    /**
     *
     * @param reducedTraining
     * @param dumpSite
     */
    private void dump(Iterable<Pair<String, String>> reduced, OutputStream dumpSite) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(dumpSite))) {
            boolean first = true;
            for (Pair<String, String> entry : reduced) {
                if (first) {
                    first = false;
                } else {
                    bw.newLine();
                    bw.newLine();
                }

                bw.write(entry.getLeft());
                bw.newLine();
                bw.write(entry.getRight());
            }
        }
    }
}
