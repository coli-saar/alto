/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import de.up.ling.tree.Tree;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;

/**
 *
 * @author koller
 */
public class Util {

    private static final RandomGenerator rnd = new Well19937c();

    private static AtomicLong nextGensym = new AtomicLong(0L);

    public static String gensym(String prefix) {
        return prefix + (nextGensym.getAndIncrement());
    }

    public static Tree<String> makeBinaryTree(String symbol, List<String> leaves) {
        return makeBinaryTree(symbol, leaves, 0);
    }

    private static Tree<String> makeBinaryTree(String symbol, List<String> leaves, int pos) {
        int remaining = leaves.size() - pos;

        if (remaining == 1) {
            return Tree.create(leaves.get(pos));
        } else {
            Tree<String> left = Tree.create(leaves.get(pos));
            Tree<String> right = makeBinaryTree(symbol, leaves, pos + 1);
            return Tree.create(symbol, left, right);
        }
    }

    public static Tree<String> makeTreeWithArities(Tree<String> tree) {
        if (tree.getChildren().isEmpty()) {
            if (tree.getLabel().startsWith("?")) {
                return tree;
            } else {
                return Tree.create(tree.getLabel() + "_0");
            }
        } else {
            List<Tree<String>> mappedChildren = mapToList(tree.getChildren(), Util::makeTreeWithArities);
            return Tree.create(tree.getLabel() + "_" + mappedChildren.size(), mappedChildren);
        }
    }

    public static <I, O> List<O> mapToList(Iterable<I> values, Function<I, O> fn) {
        return StreamSupport.stream(values.spliterator(), false).map(fn).collect(Collectors.toList());
    }
    
    public static <I> IntList mapToIntList(Iterable<I> values, ToIntFunction<I> fn) {
        IntList ret = new IntArrayList();
        for( I x : values ) {
            ret.add(fn.applyAsInt(x));
        }
        return ret;
    }

    public static <I, O> Set<O> mapToSet(Iterable<I> values, Function<I, O> fn) {
        return StreamSupport.stream(values.spliterator(), false).map(fn).collect(Collectors.toSet());
    }

    public static <E, Up> Tree<Up> mapTree(Tree<E> tree, com.google.common.base.Function<E, Up> fn) {
        return tree.dfs((node, children) -> Tree.create(fn.apply(node.getLabel()), children));
    }
    
    public static int[] mapIntArray(int[] array, IntUnaryOperator fn) {
        int[] ret = new int[array.length];
        mapIntoIntArray(array, ret, fn);
        return ret;
    }
    
    public static void mapIntoIntArray(int[] source, int[] target, IntUnaryOperator fn) {
        for( int i = 0; i < source.length; i++ ) {
            target[i] = fn.applyAsInt(source[i]);
        }
    }

    public static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    public static String getFilenameExtension(String fileName) {
        String extension = "";

        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }

        return extension;
    }

    public static String stripExtension(String filename) {
        Pattern p = Pattern.compile("(.*)\\.[^.]*");
        Matcher m = p.matcher(filename);

        if (m.matches()) {
            return m.group(1);
        } else {
            return filename;
        }
    }

    public static String formatTime(long timeInNs) {
        if (timeInNs < 1000) {
            return timeInNs + " ns";
        } else if (timeInNs < 1000000) {
            return timeInNs / 1000 + " \u03bcs";
        } else if (timeInNs < 1000000000) {
            return timeInNs / 1000000 + " ms";
        } else {
            StringBuilder buf = new StringBuilder();

            if (timeInNs > 60000000000L) {
                buf.append(timeInNs / 60000000000L + "m ");
            }

            timeInNs %= 60000000000L;

            buf.append(String.format("%d.%03ds", timeInNs / 1000000000, (timeInNs % 1000000000) / 1000000));
            return buf.toString();
        }
    }

    public static String formatTimeSince(long start) {
        return formatTime(System.nanoTime() - start);
    }
    
    private static final DateFormat NAMEDATEFORMAT = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss:SSS");
    
    public static String getCurrentDateAndTimeAsString() {
        return NAMEDATEFORMAT.format(new Date());
    }

    public interface BottomUpTreeVisitor<L, V> {

        V combine(Tree<L> node, List<V> childValues);
    }

    public static <L, V> V dfs(Tree<L> tree, BottomUpTreeVisitor<L, V> visitor) {
        List<V> childValues = mapToList(tree.getChildren(), u -> dfs(u, visitor));
        return visitor.combine(tree, childValues);
    }

    public static <V> void forEachNode(Tree<V> tree, Consumer<V> visitor) {
        Void x = tree.dfs((node, children) -> {
            visitor.accept(node.getLabel());
            return null;
        });
    }

    public static <E, F> com.google.common.base.Function<E, F> gfun(Function<E, F> javafun) {
        return x -> javafun.apply(x);
    }

    public static <E> List<E> makeList(int n, Supplier<E> sup) {
        List<E> ret = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            ret.add(sup.get());
        }

        return ret;
    }

    public static <E> List<E> makeList(int n, IntFunction<E> sup) {
        List<E> ret = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            ret.add(sup.apply(i));
        }

        return ret;
    }

    public static int[] makeIntArray(int n, IntUnaryOperator sup) {
        int[] ret = new int[n];

        for (int i = 0; i < n; i++) {
            ret[i] = sup.applyAsInt(i);
        }

        return ret;
    }

    public static void printToFile(String filename, String content) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(filename);
            fw.write(content);
            fw.close();
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fw.close();
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static long parseNumberWithPrefix(String str) {
        int indexStartPos = 0;
        int ret = 0;
        boolean foundIndex = false;

        while (indexStartPos < str.length() && !isDigit(str.charAt(indexStartPos))) {
            indexStartPos++;
        }

        while (indexStartPos < str.length()) {
            char c = str.charAt(indexStartPos++);

            if (isDigit(c)) {
                foundIndex = true;
                ret = 10 * ret + (c - '0');
            }
        }

        if (foundIndex) {
            return ret;
        } else {
            return -1;
        }
    }

    private static boolean isDigit(char character) {
        return (character >= '0') && (character <= '9');
    }

    public static double mult(DoubleStream stream) {
        MutableDouble ret = new MutableDouble(1);
        stream.forEach(ret::multiplyBy);
        return ret.getValue();
    }

    public static int sampleMultinomial(int[] values, IntToDoubleFunction prob) {
        double totalProb = Arrays.stream(values).mapToDouble(prob::applyAsDouble).sum();
        return sampleMultinomial(values, prob, totalProb);
    }

    public static int sampleMultinomial(int[] values, IntToDoubleFunction prob, double totalProb) {
        double selectProb = rnd.nextDouble() * totalProb;
        double cumulativeProb = 0;

        for (int i = 0; i < values.length; i++) {
            cumulativeProb += prob.applyAsDouble(values[i]);
            if (cumulativeProb >= selectProb) {
                return values[i];
            }
        }

        return -1;
    }

    public static String repeat(String s, int repetitions) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < repetitions; i++) {
            buf.append(s);
        }
        return buf.toString();
    }

    public static long cputime() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported()
                ? bean.getCurrentThreadCpuTime() : 0L;
    }
    
    
    public static String getStackTrace(Throwable ex) {
        //as given in http://stackoverflow.com/questions/1149703/how-can-i-convert-a-stack-trace-to-a-string
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
    
    public static <E,F> ListMultimap<E,F> groupBy(Iterable<F> elements, Function<F,E> by) {
        ListMultimap<E,F> groups = ArrayListMultimap.create();
        
        for( F e : elements ) {
            groups.put(by.apply(e), e);
        }
        
        return groups;
    }
    
    
    /**
     * Appends the suffix to all strings in the set, returning a new set with those
     * strings. If keepOld is true, the new set also contains the original strings.
     * @param set
     * @param suffix
     * @param keepOld
     * @return 
     */
    public static Set<String> appendToAll(Set<String> set, String suffix, boolean keepOld) {
        Set<String> ret = new HashSet<>();
        if (keepOld) {
            ret.addAll(set);
        }
        for (String string : set) {
            ret.add(string+suffix);
        }
        return ret;
    }
    
    /**
     * Appends the suffix to all strings in the set, returning a new set with those
     * strings. If keepOld is true, the new set also contains the original strings.
     * In this version, the suffix is only added to the strings which test true
     * with addIf; for other strings the original is kept.
     * @param set
     * @param suffix
     * @param keepOld
     * @param addIf
     * @return 
     */
    public static Set<String> appendToAll(Set<String> set, String suffix, boolean keepOld, Predicate<String> addIf) {
        if (addIf == null) {
            return appendToAll(set, suffix, keepOld);
        }
        Set<String> ret = new HashSet<>();
        if (keepOld) {
            ret.addAll(set);
        }
        for (String string : set) {
            if (addIf.test(string)) {
                ret.add(string+suffix);
            } else {
                ret.add(string);
            }
        }
        return ret;
    }
    
}
 