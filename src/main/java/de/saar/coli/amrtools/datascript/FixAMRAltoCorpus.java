/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.datascript;

import de.up.ling.tree.ParseException;
import de.up.ling.tree.Tree;
import de.up.ling.tree.TreeParser;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fixes minor problems when transforming an AMR corpus into alto format,
 * concerning things such as root sources, single quotes and tokenization.
 * @author Jonas
 */
public class FixAMRAltoCorpus {
    
    public static void main(String[] args) throws IOException, ParseException {
        String outputPath = args[0];
        if (!outputPath.endsWith("/")) {
            outputPath = outputPath+"/";
        }
        
        fixAMRCorpus(outputPath, true);
    }
    
    /**
     * Fixes minor problems when transforming an AMR corpus into alto format,
     * concerning things such as root sources, single quotes and tokenization.
     * @param path
     * @param useTrees
     * @throws IOException
     * @throws ParseException 
     */
    static void fixAMRCorpus(String path, boolean useTrees) throws IOException, ParseException {
        FixAMRAltoCorpus fixer = new FixAMRAltoCorpus(useTrees);
        
        fixer.replaceSinglequotes(path);
        fixer.makeAnonNodesExplicit(path);
        fixer.changeTreeFormatInCorpus(path);
        fixer.fixRefOrder(path);
        fixer.tokenizeTree(path);
        fixer.addRootSources(path);
    }
    
    private FixAMRAltoCorpus(boolean useTrees) {
        if (useTrees) {
            START_LINE = 8;
            STRING_LINE = 0;
            TREE_LINE = 1;
            GRAPH_LINE = 2;
            TOTAL_LINES = 3;
        } else {
            START_LINE = 7;
            STRING_LINE = 0;
            TREE_LINE = -1;
            GRAPH_LINE = 1;
            TOTAL_LINES = 2;
        }
    }
    
    final int START_LINE;
    final int STRING_LINE;
    final int TREE_LINE;
    final int GRAPH_LINE;
    final int TOTAL_LINES;

    public static final String SINGLEQUOTE_REPLACEMENT = "AsinglequoteA";
    private static final String TOKEN_NT = "TKN";
    private static final String W_PLACEHOLDER = "WHITESPACEINANGLEBRACKETSJG";
         
    private void replaceSinglequotes(String path) throws FileNotFoundException, IOException {
        
        String corpusPath = path+"raw.corpus";//args[0];
        String targetPath = path+"NSQ.corpus";//args[1];
        
        
        BufferedReader corpusReader = new BufferedReader(new FileReader(corpusPath));
        FileWriter writer = new FileWriter(targetPath);
        
        String line = null;
        int i = 0;
        int actualLineCounter = 0;
        while ((line = corpusReader.readLine()) != null) {
            if (i>= START_LINE) {
                if (actualLineCounter%TOTAL_LINES == STRING_LINE) {
                    writer.write(line+"\n");
                } else if (actualLineCounter%TOTAL_LINES == GRAPH_LINE) {
                    writer.write(line.replaceAll("\'", SINGLEQUOTE_REPLACEMENT)+"\n");
                } else if (actualLineCounter%TOTAL_LINES == TREE_LINE) {
                    StringBuilder sb = new StringBuilder();
                    boolean inDoubleQuotes = false;
                    for (int j = 0; j<line.length(); j++) {
                        char c = line.charAt(j);
                        if (c == '"') {
                            inDoubleQuotes = !inDoubleQuotes;
                            //do not want to append this double quote since we won't need it
                        } else if (c == '\'' && inDoubleQuotes) {
                            sb.append(SINGLEQUOTE_REPLACEMENT);
                        } else {
                            sb.append(c);
                        }
                    }
                    writer.write(sb.toString()+"\n");
                } else {
                    writer.write(line+"\n");
                }
                if (!line.equals("")) {
                    actualLineCounter++;
                }
            } else {
                writer.write(line+"\n");
            }
            
            i++;
        }
        
        
        writer.close();
    }
    
    
    
    private void changeTreeFormatInCorpus(String path) throws IOException, FileNotFoundException {
        BufferedReader corpusReader = new BufferedReader(new FileReader(path+"ENSQ.corpus"));
        FileWriter writer = new FileWriter(path+"finalAMRstrings.corpus");
        int i = 0;
        
        while (corpusReader.ready()) {
            String line = corpusReader.readLine();
            if (line.startsWith("[tree]")) {
                line = line.substring("[tree] ".length());
                //replace singlequotes
                line = line.replaceAll("\\'", "AsinglequoteA");
                //replace whitespace in angle brackets (i.e. within one label) with special marker
//                Pattern p = Pattern.compile("(\\<[^\\>]*) ([^\\>]*\\>)");
//                Matcher m = p.matcher(line);
//                while (m.find()) {
//                    line = m.replaceFirst("$1"+W_PLACEHOLDER+"$2");
//                    m.reset(line);
//                }
                //add singlequotes around labels with special characters
                line = line.replaceAll("(\\(*)([^ \\(\\)]*[^A-Za-z\\(\\) ][^ \\(\\)]*)(\\)*)", "$1\\'$2\\'$3");
                //insert commata for explicit tree structure
                line = line.replaceAll("\\) \\(", "\\), \\(");
                //move nonterminals in front of opening bracket
                line = line.replaceAll("\\(([^ ]*) ", "$1\\( ");
                //replace weird CD pattern of single quotes
                line = line.replaceAll("\\' \\'", " ");
                //remove special whitespace marker from earlier
                line = line.replaceAll(W_PLACEHOLDER, " ");
                //normalize (separate accent from accented letter) and remove accents
                line = Normalizer.normalize(line, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                //replace non-ascii characters
                line = line.replaceAll("[^\\x00-\\x7F]", "_UNK_CHAR_");
            } else {
                line = line.replaceAll("\\[(string|graph)\\] ", "");
                //normalize (separate accent from accented letter) and remove accents
                line = Normalizer.normalize(line, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                //replace non-ascii characters
                line = line.replaceAll("[^\\x00-\\x7F]", "_UNK_CHAR_");
            }
            writer.write(line+"\n");
        }
        writer.close();
    }
    
    
    private void makeAnonNodesExplicit(String path) throws FileNotFoundException, IOException {
        
        BufferedReader corpus = new BufferedReader(new FileReader(path+"NSQ.corpus"));
        
        FileWriter writer = new FileWriter(path+"ENSQ.corpus");
        
        String line = null;
        int i = 0;
        int actualLineCounter = 0;
        while ((line = corpus.readLine()) != null) {
            if (i>= START_LINE) {
                if (actualLineCounter%TOTAL_LINES == GRAPH_LINE) {
                    String res = makeAnonExpl(line);
                    try {
                        writer.write(res+"\n");
                    } catch (IOException ex) {
                        System.err.println(ex);
                    }
                } else {
                    writer.write(line+"\n");
                }
                if (!line.equals("")) {
                    actualLineCounter++;
                }
            } else {
                writer.write(line+"\n");
            }
            
            i++;
        }
        
        writer.close();
    }
    
    /**
     * makes anonymous nodes (e.g.~for numbers or names) explicit, i.e.~gives
     * them node names.
     * @param graph
     * @return 
     */
    public static String makeAnonExpl(String graph) {
        int explCounter = 0;
        String regex = "( )(:[A-Za-z0-9-]+) (\"[^\"]+\"|\\+|\\-[0-9.,]*|interrogative|expressive|imperative|[0-9.,]+)";
        Pattern p = Pattern.compile(regex);
        String res = graph;
        Matcher m = p.matcher(res);
        while (m.find()) {
            res = m.replaceFirst("$1$2 (explicitanon"+explCounter+" / $3)");//put in quotation marks to allow all symbols to be parsed (in particular the '+')
            m.reset(res);
            explCounter++;
        }
        res = res.replaceAll("(explicitanon[0-9]+ / )\\+", "$1\"\\+\"");
        return res;
    }
    
    
    private void fixRefOrder(String path) throws FileNotFoundException, IOException {
        
        String regex = "\\(([a-z][0-9]*) /";//only need names confirming to the standard naming conventions, for now
        BufferedReader corpus = new BufferedReader(new FileReader(path+"finalAMRstrings.corpus"));
        
        Pattern p = Pattern.compile(regex);
        FileWriter writer = new FileWriter(path+"RefOrderFixed.corpus");
        
        String line = null;
        int i = 0;
        int actualLineCounter = 0;
        while ((line = corpus.readLine()) != null) {
            if (i>= START_LINE) {
                if (actualLineCounter%TOTAL_LINES == GRAPH_LINE) {
                    Matcher m = p.matcher(line);
                    List<String> names = new ArrayList<>();
                    while (m.find()) {
                        names.add(m.group(1));
                    }
                    boolean changed = true;
                    while (changed) {
                        changed = false;
                        for (String n : names) {
                            int withLabel = line.indexOf("("+n+" /");
                            Matcher refM = Pattern.compile(" :[a-zA-Z0-9-]+ "+n+"[^0-9]").matcher(line);
                            if (refM.find() && refM.start() < withLabel) {
                                changed = true;
                                int openBs = 0;
                                int labelGroupClosing = 0;
                                boolean inQuote = false;
                                OUTER:
                                for (int pos = withLabel+1; pos < line.length(); pos++) {
                                    if (!inQuote) {
                                        switch (line.charAt(pos)) {
                                            case '(':
                                                openBs++;
                                                break;
                                            case ')':
                                                if (openBs == 0) {
                                                    labelGroupClosing = pos+1;
                                                    break OUTER;
                                                } else {
                                                    openBs--;
                                                }
                                                break;
                                            case '"':
                                                inQuote = true;
                                                break;
                                            default:
                                                break;
                                        }
                                    } else {
                                        if (line.charAt(pos) == '"') {
                                            inQuote = false;
                                        }
                                    }
                                }
                                String s1 = line.substring(0, refM.end()-1-n.length());// -1 because of lookahead in refM
                                String s2 = line.substring(refM.end()-1, withLabel);
                                String labelGroup = line.substring(withLabel, labelGroupClosing);
                                String s3 = line.substring(labelGroupClosing);
                                line = s1+labelGroup+s2+n+s3;
                            }
                        }
                    }




                    writer.write(line+"\n");
                } else {
                    writer.write(line+"\n");
                }
                if (!line.equals("")) {
                    actualLineCounter++;
                }
            } else {
                writer.write(line+"\n");
            }
            i++;
        }
        
        
        
        
        writer.close();
    }
    
    private void addRootSources(String path) throws FileNotFoundException, IOException {
        
        
        
        String corpusPath = path+"tokenizedTrees.corpus";//"C:/Users/Jonas/Documents/Work/experimentData/Corpora/semeval2017Stripped/train20.corpus";//BitBuckets/alto-experimental/corpus100Dev3.txt";//args[0];
        String targetPath = path+"finalAlto.corpus";//"C:/Users/Jonas/Documents/Work/experimentData/Corpora/semeval2017Stripped/train20Rooted.corpus";//BitBuckets/alto-experimental/corpus100DevRooted.txt";//args[1];
        
        
        BufferedReader corpusReader = new BufferedReader(new FileReader(corpusPath));
        FileWriter writer = new FileWriter(targetPath);
        
        String line = null;
        int i = 0;
        int actualLineCounter = 0;
        while ((line = corpusReader.readLine()) != null) {
            if (i>= START_LINE) {
                if (actualLineCounter%TOTAL_LINES == GRAPH_LINE) {
                    int index = line.indexOf("/");
                    writer.write(line.substring(0, index)+"<root> "+line.substring(index)+"\n");
                } else {
                    writer.write(line+"\n");
                }
                if (!line.equals("")) {
                    actualLineCounter++;
                }
            } else {
                writer.write(line+"\n");
            }
            
            i++;
        }
        
        writer.close();
    }
    
    /**
     * Replaces each leaf in the tree that contains whitespace with a node that
     * has children for each part of the leaf label split along whitespace.
     * @param path
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private void tokenizeTree(String path) throws IOException, ParseException {
        
        String corpusPath = path+"RefOrderFixed.corpus";//"C:/Users/Jonas/Documents/Work/experimentData/Corpora/semeval2017Stripped/train20.corpus";//BitBuckets/alto-experimental/corpus100Dev3.txt";//args[0];
        String targetPath = path+"tokenizedTrees.corpus";//"C:/Users/Jonas/Documents/Work/experimentData/Corpora/semeval2017Stripped/train20Rooted.corpus";//BitBuckets/alto-experimental/corpus100DevRooted.txt";//args[1];
        
        
        BufferedReader corpusReader = new BufferedReader(new FileReader(corpusPath));
        FileWriter writer = new FileWriter(targetPath);
        
        String line = null;
        int i = 0;
        int actualLineCounter = 0;
        while ((line = corpusReader.readLine()) != null) {
            if (i>= START_LINE) {
                if (actualLineCounter%TOTAL_LINES == TREE_LINE) {
                    try {
                        Tree<String> tree = TreeParser.parse(line);
                        tree = tree.substitute(t -> {
                            if (t.getChildren().size() == 1) {
                                Tree<String> leaf = t.getChildren().iterator().next();
                                if (leaf.getChildren().isEmpty() && leaf.getLabel().matches(".+ .+")) {
                                    String[] labelParts = leaf.getLabel().split(" ");
                                    List<Tree<String>> newChildren = new ArrayList<>();
                                    int k = 0;
                                    for (String part : labelParts) {
                                        newChildren.add(Tree.create(TOKEN_NT+k, Collections.singletonList(Tree.create(part, Collections.EMPTY_LIST))));
                                        k++;
                                    }
                                    return Tree.create(t.getLabel(), newChildren);
                                } else {
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        });
                        writer.write(tree.toString()+"\n");
                    } catch (ParseException ex) {
                        System.err.println(line);
                    }
                } else {
                    writer.write(line+"\n");
                }
                if (!line.equals("")) {
                    actualLineCounter++;
                }
            } else {
                writer.write(line+"\n");
            }
            
            i++;
        }
        
        writer.close();
    }
    
    
}
