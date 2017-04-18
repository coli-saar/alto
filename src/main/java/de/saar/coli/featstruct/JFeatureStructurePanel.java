/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.saar.coli.featstruct;

import de.saar.basic.IdentityHashSet;
import de.up.ling.irtg.util.MutableInteger;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author koller
 */
public class JFeatureStructurePanel extends JPanel {

    private FeatureStructure fs;
    private Layout layout;
    private Graphics2D currentGraphics;

    private static final int CHARACTER_HEIGHT = 14;
    
    private static final int PADX = 10;
    private static final int INDEX_PADX = 5;
    
    private static final int PADY = 8;
    private static final int LINE_DIST = 3;

    public JFeatureStructurePanel(FeatureStructure fs) {
        this.fs = fs;
    }

    public static void main(String[] args) throws FsParsingException {
        FeatureStructure fs = FeatureStructure.parse("[num: #1 sg, subj: [num: #1]]");
        draw(fs);
    }

    public static <E> JFrame draw(FeatureStructure fs) {
        JFrame f = new JFrame("fs: " + fs.toString());
        JFeatureStructurePanel panel = new JFeatureStructurePanel(fs);

        Container contentPane = f.getContentPane();
        contentPane.add(panel);

        f.pack();
        f.setVisible(true);

        return f;
    }

    @Override
    public void paintComponent(final Graphics grphcs) {
        final Graphics2D graphics = (Graphics2D) grphcs;
        currentGraphics = graphics;

        Map<FeatureStructure, String> reentrantFsToIndex = new IdentityHashMap<>();
        fs.computeReentrancies(reentrantFsToIndex, new MutableInteger(1));
        reentrantFsToIndex.values().removeAll(Collections.singleton(FeatureStructure.FIRST_VISIT)); // remove all non-reentrant nodes

        layout = new Layout(graphics, fs, reentrantFsToIndex);

        // paint background
        graphics.setColor(Color.white);
        graphics.fillRect(0, 0, layout.getWidth(), layout.getHeight());
        graphics.setColor(Color.black);

        // paint feature structure
        Set<FeatureStructure> visitedIndexedFs = new IdentityHashSet<>();
        paint(fs, layout, 0, new MutableInteger(0), visitedIndexedFs, reentrantFsToIndex);
    }

    @Override
    public Dimension getPreferredSize() {
        if (layout == null) {
            return new Dimension(100, 100);
        } else {
            return new Dimension(layout.getWidth(), layout.getHeight());
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    private int getLabelWidth(String label) {
        int ret = currentGraphics.getFontMetrics().stringWidth(label);
        return ret;
    }

    // returns max width of children
    private int paint(FeatureStructure fs, Layout layout, int depth, MutableInteger row, Set<FeatureStructure> visitedIndexedFs, Map<FeatureStructure, String> reentrantFsToIndex) {
        int x = layout.getX(depth) + 20;
        int y = y(row.getValue());

        if (visitedIndexedFs.contains(fs)) {
            String s = reentrantFsToIndex.get(fs);
            row.incValue();
            return x + drawIndex(x, y, s) + INDEX_PADX;
        } else {
            visitedIndexedFs.add(fs);

            if (reentrantFsToIndex.containsKey(fs)) {
                x += drawIndex(x, y, reentrantFsToIndex.get(fs)) + INDEX_PADX;
            }

            if (fs instanceof AvmFeatureStructure) {
                AvmFeatureStructure avmFeatureStructure = (AvmFeatureStructure) fs;
                int childMaxX = 0;
                
                int topY = y(row.getValue()) - CHARACTER_HEIGHT;
//                System.err.printf("%s: %d, top %d\n", fs, x, topY);

                for (String attr : avmFeatureStructure.getAttributes()) {
                    currentGraphics.drawString(attr, x, y(row.getValue()));
                    int thisChildWidth = paint(avmFeatureStructure.get(attr), layout, depth + 1, row, visitedIndexedFs, reentrantFsToIndex);
                    childMaxX = Math.max(childMaxX, thisChildWidth);
                }
                
                int bottomY = y(row.getValue()) - CHARACTER_HEIGHT;
//                System.err.printf("%s: bottom %d\n", fs, bottomY);
                
                currentGraphics.drawLine(x-LINE_DIST, topY, x-LINE_DIST, bottomY);
                currentGraphics.drawLine(x-LINE_DIST, topY, x+LINE_DIST, topY);
                currentGraphics.drawLine(x-LINE_DIST, bottomY, x+LINE_DIST, bottomY);
                
                x = childMaxX;
                currentGraphics.drawLine(x+LINE_DIST, topY, x+LINE_DIST, bottomY);
                currentGraphics.drawLine(x-LINE_DIST, topY, x+LINE_DIST, topY);
                currentGraphics.drawLine(x-LINE_DIST, bottomY, x+LINE_DIST, bottomY);
                
                return childMaxX + LINE_DIST;
            } else {
                String s = fs.toString();
                currentGraphics.drawString(s, x, y);
                row.incValue();
                return x + getLabelWidth(s);
            }
        }
    }
    
    private static final int BOX_DIST = 2;
    
    // returns width
    private int drawIndex(int x, int y, String index) {
        currentGraphics.drawString(index.substring(1), x, y);        
        currentGraphics.drawRect(x-2*BOX_DIST, y-CHARACTER_HEIGHT+2, getLabelWidth(index), BOX_DIST + CHARACTER_HEIGHT);
        return BOX_DIST + CHARACTER_HEIGHT;
    }

    private int y(int row) {
        return 2*PADY + row * (CHARACTER_HEIGHT+PADY);
    }

    private class Layout {

        private IntList maxWidthPerDepth;
        private Map<FeatureStructure, String> reentrantFsToIndex;
        private int height;
        private Graphics2D graphics;

        public Layout(Graphics2D graphics, FeatureStructure fs, Map<FeatureStructure, String> reentrantFsToIndex) {
            this.graphics = graphics;
            this.reentrantFsToIndex = reentrantFsToIndex;
            maxWidthPerDepth = new IntArrayList();

            compute(fs);
        }

        public void compute(FeatureStructure fs) {
            Set<FeatureStructure> visitedIndexedFs = new IdentityHashSet<>();
            height = 0;
            compute(fs, 0, visitedIndexedFs);
        }

        public int getWidth() {
            int width = 0;
            for (int i = 0; i < maxWidthPerDepth.size(); i++) {
                width += maxWidthPerDepth.get(i);
            }
            
            return width + PADX;
        }
        
        public int getMaxWidth(int depth) {
            return maxWidthPerDepth.get(depth);
        }

        public int getHeight() {
            return height + 20;
        }

        public int getX(int depth) {
            int x = 0;
            for (int i = 0; i < depth; i++) {
                x += maxWidthPerDepth.get(i);
            }
            return x;
        }

        private void compute(FeatureStructure fs, int depth, Set<FeatureStructure> visitedIndexedFs) {
            if (visitedIndexedFs.contains(fs)) {
                updateDepth(depth, getLabelWidth(reentrantFsToIndex.get(fs)));
                height += CHARACTER_HEIGHT + PADY;
            } else {
                visitedIndexedFs.add(fs);
                int localWidth = 0;

                if (reentrantFsToIndex.containsKey(fs)) {
                    localWidth += getLabelWidth(reentrantFsToIndex.get(fs)) + PADX;
                }

                if (fs instanceof AvmFeatureStructure) {
                    AvmFeatureStructure avmFeatureStructure = (AvmFeatureStructure) fs;

                    for (String attr : avmFeatureStructure.getAttributes()) {
                        updateDepth(depth, localWidth + getLabelWidth(attr));
                        compute(avmFeatureStructure.get(attr), depth + 1, visitedIndexedFs);
                    }

                    if (avmFeatureStructure.getAttributes().isEmpty()) {
                        // only for empty AVM, count one line
                        height += CHARACTER_HEIGHT + PADY;
                    }
                } else {
                    updateDepth(depth, localWidth + getLabelWidth(fs.toString())); // TODO this is not entirely precise
                    height = CHARACTER_HEIGHT + PADY;
                }
            }
        }

        private void updateDepth(int depth, int componentWidth) {
            componentWidth += PADX;

            if (maxWidthPerDepth.size() <= depth) {
                maxWidthPerDepth.add(0);
            }

            if (componentWidth > maxWidthPerDepth.get(depth)) {
                maxWidthPerDepth.set(depth, componentWidth);
            }
        }
    }
}
