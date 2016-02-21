/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.data_creation;

import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 *
 * @author christoph_teichmann
 */
public class MakeAlignments {

    /**
     *
     * @param alignments
     * @param supp
     * @param useRight
     * @throws IOException
     */
    public static void makeStringFromStandardAlign(InputStream alignments, Supplier<OutputStream> supp, boolean useRight)
            throws IOException {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(alignments))) {
            String line;

            while ((line = input.readLine()) != null) {
                line = line.trim();

                String[] parts = line.isEmpty() ? new String[0] : line.split("\\s+");
                Map<String, Set<String>> outmap = new HashMap();
                int num = 0;
                for (String part : parts) {

                    String marker = Integer.toString(++num);
                    String[] alParts = part.trim().split("-");
                    int pos = Integer.parseInt(alParts[useRight ? 1 : 0].trim());

                    String state = "'" + pos + "-" + (pos + 1) + "'";
                    Set<String> set = outmap.get(state);

                    if (set == null) {
                        set = new HashSet<>();
                        outmap.put(state, set);
                    }

                    set.add(marker);
                }

                OutputStream out = supp.get();
                dumpResults(out, outmap);
            }
        }
    }

    /**
     *
     * @param out
     * @param outmap
     * @throws IOException
     */
    private static void dumpResults(OutputStream out, Map<String, Set<String>> outmap) throws IOException {
        try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(out))) {
            boolean first = true;

            for (Map.Entry<String, Set<String>> entry : outmap.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    output.newLine();
                }

                output.write(entry.getKey());
                output.write(" |||");
                for (String alignment : entry.getValue()) {
                    output.write(" ");
                    output.write(alignment);
                }
            }
        }
    }

    /**
     *
     * @param alignments
     * @param trees
     * @param supp
     * @param useRight
     * @throws IOException
     * @throws ParseException
     */
    public static void makePreorderTreeFromStandard(InputStream alignments, InputStream trees, Supplier<OutputStream> supp, boolean useRight)
            throws IOException, ParseException {
        try (BufferedReader treeInput = new BufferedReader(new InputStreamReader(trees));
                BufferedReader alignmentInput = new BufferedReader(new InputStreamReader(alignments))) {
            String tLine;
            while ((tLine = treeInput.readLine()) != null) {
                tLine = tLine.trim();
                if (tLine.isEmpty()) {
                    continue;
                }

                Tree<String> t = TreeParser.parse(tLine);
                Map<String, Set<String>> outmap = new HashMap();

                AtomicInteger ai = new AtomicInteger(0);
                Int2ObjectMap<String> posToAddress = new Int2ObjectOpenHashMap<>();
                addAddresses(t, "0-0-0", posToAddress, ai);

                String alignLine = alignmentInput.readLine().trim();
                if (!alignLine.isEmpty()) {
                    String[] parts = alignLine.trim().split("\\s+");

                    int code = 0;
                    for (String part : parts) {
                        String marker = Integer.toString(++code);

                        String posStr = part.trim().split("-")[useRight ? 1 : 0].trim();
                        int pos = Integer.parseInt(posStr);

                        String address = posToAddress.get(pos);

                        Set<String> set = outmap.get(address);
                        if (set == null) {
                            set = new HashSet<>();
                            outmap.put(address, set);
                        }

                        set.add(marker);
                    }
                }

                dumpResults(supp.get(), outmap);
            }
        }
    }

    /**
     *
     * @param t
     * @param addressString
     * @param posToAddress
     * @param ai
     */
    private static void addAddresses(Tree<String> t, String addressString,
            Int2ObjectMap<String> posToAddress, AtomicInteger ai) {
        int num = ai.getAndIncrement();
        posToAddress.put(num, addressString);

        for (int i = 0; i < t.getChildren().size(); ++i) {
            addAddresses(t.getChildren().get(i), addressString + "-" + i, posToAddress, ai);
        }
    }
}
