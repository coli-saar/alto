/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.amrtools.aligner;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A set of rules mapping AMR labels to corresponding words.
 * @author Jonas
 */
public class FixedNodeToWordRules {
    
    private final static Map<String, String[]> FIXED_RULES = new HashMap<>();
    private final static Map<String, String[]> FIXED_SECONDARY_RULES = new HashMap<>();
    
    static {
        FIXED_RULES.put("-", new String[]{"no", "non", "not", "dont", "don't", "n't", "without", 
                "ain't", "aint", "doesn't", "doesnt", "didn't", "didnt"});//TODO should we check whether this is actually attached to a :polarity edge?
        FIXED_RULES.put("+", new String[]{"please"});//TODO should we check whether this is actually attached to a :polite edge?
        FIXED_RULES.put("1",new String[]{"one", "first", "Monday", "January", "Jan"});
        FIXED_RULES.put("2",new String[]{"two", "second", "Tuesday", "February", "Feb"});
        FIXED_RULES.put("3",new String[]{"three", "third", "Wednesday", "March", "Mar"});
        FIXED_RULES.put("4",new String[]{"four", "fourth", "Thursday", "april", "apr"});
        FIXED_RULES.put("5",new String[]{"five", "fifth", "Friday", "may"});
        FIXED_RULES.put("6",new String[]{"six", "sixth", "Saturday", "June", "Jun"});
        FIXED_RULES.put("7",new String[]{"seven", "seventh", "Sunday", "July", "Jul"});
        FIXED_RULES.put("8",new String[]{"eight", "eighth", "August", "Aug"});
        FIXED_RULES.put("9",new String[]{"nine", "ninth", "September", "Sep"});
        FIXED_RULES.put("10",new String[]{"ten", "tenth", "October", "Oct"});
        FIXED_RULES.put("11",new String[]{"eleven", "November", "Nov"});
        FIXED_RULES.put("12",new String[]{"twelve", "December", "Dec"});
        FIXED_RULES.put("13",new String[]{"thirteen"});
        FIXED_RULES.put("14",new String[]{"fourteen"});
        FIXED_RULES.put("15",new String[]{"fifteen"});
        FIXED_RULES.put("16",new String[]{"sixteen"});
        FIXED_RULES.put("17",new String[]{"seventeen"});
        FIXED_RULES.put("18",new String[]{"eighteen"});
        FIXED_RULES.put("19",new String[]{"nineteen"});
        FIXED_RULES.put("20",new String[]{"twenty"});
        FIXED_RULES.put("30",new String[]{"thirty"});
        FIXED_RULES.put("40",new String[]{"fourty", "forty"});
        FIXED_RULES.put("50",new String[]{"fifty"});
        FIXED_RULES.put("60",new String[]{"sixty"});
        FIXED_RULES.put("70",new String[]{"seventy"});
        FIXED_RULES.put("80",new String[]{"eighty"});
        FIXED_RULES.put("90",new String[]{"ninety"});
        FIXED_RULES.put("100",new String[]{"hundred"});
        FIXED_RULES.put("1000",new String[]{"thousand"});
        FIXED_RULES.put("possible-01",new String[]{"can", "ca", "could", "couldn't", "might", "may", "can't", "cannot", "able"});//be able to, been able to
        FIXED_RULES.put("conform-01",new String[]{"within"});
        FIXED_RULES.put("recommend-01",new String[]{"should"});
        FIXED_RULES.put("cause-01",new String[]{"as", "because", "since", "due", "if", "such", "reason", "therefore", "thus", "hence"});
        FIXED_RULES.put("infer-01",new String[]{"therefore", "since", "thus", "hence"});
        FIXED_RULES.put("contrast-01",new String[]{"while", "however", "but"});
        FIXED_RULES.put("have-concession-91",new String[]{"although", "though", "anyway", "but"});
        FIXED_RULES.put("obligate-01",new String[]{"must"});//TODO: word combinations such as "has to"
        //fixedRules.put("HAVE_ORG_ROLE",new String[]{"at"});
        FIXED_RULES.put("before",new String[]{"ago", "since"});
        FIXED_RULES.put("lack-01",new String[]{"without"});
        FIXED_RULES.put("and",new String[]{"addition"});
        FIXED_RULES.put("multi-sentence",new String[]{".", ";"});
        FIXED_RULES.put("mean-01",new String[]{":", "i.e."});
        FIXED_RULES.put("oppose-01",new String[]{"anti"});
        FIXED_RULES.put("counter-01",new String[]{"anti"});
        FIXED_RULES.put("this",new String[]{"these"});
        FIXED_RULES.put("that",new String[]{"those"});
        FIXED_RULES.put("okay",new String[]{"ok", "o.k."});
        FIXED_RULES.put("have-03",new String[]{"with"});
        FIXED_RULES.put("say-01",new String[]{"according"});
        FIXED_RULES.put("include-91",new String[]{"of", "also"});
        FIXED_RULES.put("expressive",new String[]{"!","..", "...", "...."});
        FIXED_RULES.put("interrogative",new String[]{"?"});
        FIXED_RULES.put("slash",new String[]{"/"});
        FIXED_RULES.put("percentage-entity",new String[]{"%"});
        FIXED_RULES.put("dollar",new String[]{"$"});
        FIXED_RULES.put("seismic-quanity",new String[]{"magnitude"});
        FIXED_RULES.put("date-entity",new String[]{"o'clock"});
        FIXED_RULES.put("amr-unknown",new String[]{"who", "why", "what", "how", "where", "when", "which", "whose", "whom"});
        FIXED_RULES.put("et-cetera", new String[]{"etc", "etc.", "etcetera"});
        FIXED_RULES.put("byline-91", new String[]{"by"});
        FIXED_RULES.put("rate-entity-91", new String[]{"per", "every", "each"});
        FIXED_RULES.put("choose-01", new String[]{"choice"});
        FIXED_RULES.put("have-condition-91", new String[]{"if", "otherwise"});
        
        FIXED_SECONDARY_RULES.put("multi-sentence", new String[]{","});
        FIXED_SECONDARY_RULES.put("we",new String[]{"us", "our", "ours", "ourselves"});
        FIXED_SECONDARY_RULES.put("i",new String[]{"me", "my", "mine", "myself"});
        FIXED_SECONDARY_RULES.put("he",new String[]{"him", "his", "himself"});
        FIXED_SECONDARY_RULES.put("she",new String[]{"her", "hers", "herself"});
        FIXED_SECONDARY_RULES.put("it",new String[]{"its", "itself"});
        FIXED_SECONDARY_RULES.put("you",new String[]{"your", "yours", "yourself", "yourselves"});
        FIXED_SECONDARY_RULES.put("they",new String[]{"them", "their", "theirs", "themselves"});
        FIXED_SECONDARY_RULES.put("and",new String[]{",", ";"});
        FIXED_SECONDARY_RULES.put("mean",new String[]{",", "-"});
        FIXED_SECONDARY_RULES.put("1",new String[]{"a", "an"});
        FIXED_SECONDARY_RULES.put("after",new String[]{"in"});
        FIXED_SECONDARY_RULES.put("interrogative",new String[]{"if", "whether"});
        FIXED_SECONDARY_RULES.put("infer-01",new String[]{"so", "for", "must"});
        FIXED_SECONDARY_RULES.put("person",new String[]{"they"});
    }
    
    
    /**
     * Gets directly related words for a node label.
     * @param nodeLabel
     * @return 
     */
    static Set<String> getDirectWords(String nodeLabel) {
        Set<String> ret = new HashSet<>();
        
        //e.g. for run-01, add run
        String word = "";
        try {
            word = (nodeLabel.matches(Util.VERB_PATTERN))? nodeLabel.substring(0, nodeLabel.lastIndexOf("-")) : nodeLabel;
        } catch (java.lang.NullPointerException ex) {
            ex.printStackTrace();
        }
        ret.add(word.toLowerCase());
        
        //add result of manual rules defined above
        if (FIXED_RULES.containsKey(nodeLabel)) {
            ret.addAll(Arrays.asList(FIXED_RULES.get(nodeLabel)));
        }
        
        if (nodeLabel.matches("[a-z]+-[a-z].*")) {
            // e.g. for look-up-01, add look.
            if (nodeLabel.matches(Util.VERB_PATTERN)) {
                ret.add(nodeLabel.split("-")[0]);
            }
        }
        
        if (nodeLabel.matches("[0-9]+")) {
            String withoutZeroes = nodeLabel;
            while (withoutZeroes.endsWith("0")) {
                withoutZeroes = withoutZeroes.substring(0, withoutZeroes.length()-1);
            }
            ret.add(withoutZeroes);
        }
        
        return ret;
    }
    
    /**
     * Gets indirectly related words for a node label.
     * Contains the direct words. For now the direct words + pronouns.
     * @param nodeLabel
     * @return 
     */
    static Set<String> getIndirectWords(String nodeLabel) {
        Set<String> ret = getDirectWords(nodeLabel);
        if (FIXED_SECONDARY_RULES.containsKey(nodeLabel)) {
            ret.addAll(Arrays.asList(FIXED_SECONDARY_RULES.get(nodeLabel)));
        }
        if (nodeLabel.matches("[a-z]+-[a-z].*")) {
            if (!nodeLabel.matches(Util.VERB_PATTERN)) {
                String[] parts = nodeLabel.split("-");
                int max = Arrays.stream(parts).map(part -> part.length()).collect(Collectors.maxBy(Comparator.naturalOrder())).get();
                for (String part : parts) {
                    if (part.length() >= Math.min(max, 4)) {
                        ret.add(part);
                    }
                }
            }
        }
        if (nodeLabel.matches("[1-2][0-9][0-9][0-9]")) {
            ret.add(nodeLabel.substring(2));//years
        }
        return ret;
    }
    
}
