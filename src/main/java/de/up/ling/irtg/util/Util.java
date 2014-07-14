/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import de.up.ling.tree.Tree;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *
 * @author koller
 */
public class Util {

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
            return Tree.create(symbol, new Tree[]{left, right});
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
            List<Tree<String>> mappedChildren = mapList(tree.getChildren(), Util::makeTreeWithArities);
            return Tree.create(tree.getLabel() + "_" + mappedChildren.size(), mappedChildren);
        }
    }

    public static <I, O> List<O> mapList(List<I> values, Function<I, O> fn) {
        return values.stream().map(fn).collect(Collectors.toList());
    }
    
    public static <I, O> Set<O> mapSet(Set<I> values, Function<I, O> fn) {
        return values.stream().map(fn).collect(Collectors.toSet());
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
            StringBuffer buf = new StringBuffer();

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
    
    public static interface BottomUpTreeVisitor<L,V> {
        public V combine(Tree<L> node, List<V> childValues);
    }
    
    public static <L,V> V dfs(Tree<L> tree, BottomUpTreeVisitor<L,V> visitor) {
        List<V> childValues = mapList(tree.getChildren(), u -> dfs(u, visitor));
        return visitor.combine(tree, childValues);
    }
    
    public static <E,F> com.google.common.base.Function<E,F> gfun(Function<E,F> javafun) {
        return new com.google.common.base.Function<E,F>() {
            @Override
            public F apply(E x) {
                return javafun.apply(x);
            }
            
        };
    }
}
