/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.gui.ProgressBarDialog;
import java.awt.Frame;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author koller
 */
public abstract class InputCodec<E> {

    private static Map<String, InputCodec> codecByName = null;
    private static Map<String, InputCodec> codecByExtension = null;

    protected ProgressBarDialog progressBar;

    public abstract E read(InputStream is) throws ParseException, IOException;

    public E read(String s) throws ParseException, IOException {
        return read(new ByteArrayInputStream(s.getBytes()));
    }

    public CodecMetadata getMetadata() {
        return (CodecMetadata) getClass().getAnnotation(CodecMetadata.class);
    }

    private static Iterable<InputCodec> getAllInputCodecs() {
        return ServiceLoader.load(InputCodec.class);
    }

    public static <T> List<InputCodec<T>> getInputCodecs(Class<T> forClass) {
        List<InputCodec<T>> ret = new ArrayList<>();

        for (InputCodec codec : getAllInputCodecs()) {
            if (forClass.isAssignableFrom(codec.getMetadata().type())) {
                ret.add(codec);
            }
        }

        return ret;
    }

    public static InputCodec getInputCodecByName(String name) {
        if (codecByName == null) {
            codecByName = new HashMap<>();
            for (InputCodec ic : getAllInputCodecs()) {
                codecByName.put(ic.getMetadata().name(), ic);
            }
        }

        return codecByName.get(name);
    }

    public static InputCodec getInputCodecByExtension(String extension) {
        if (codecByExtension == null) {
            codecByExtension = new HashMap<>();
            for (InputCodec ic : getAllInputCodecs()) {
                codecByExtension.put(ic.getMetadata().extension(), ic);
            }
        }

        return codecByExtension.get(extension);
    }

    public JDialog showProgressDialog(Frame parent) {
        progressBar = new ProgressBarDialog("Reading " + getMetadata().description() + " ...", 10000, parent, false);
        progressBar.getProgressBar().setStringPainted(true);
        progressBar.setVisible(true);
        return progressBar;
    }

    protected void updateProgressDialog(int value, int maximumValue, String text) {
        if (progressBar != null) {
                SwingUtilities.invokeLater(() -> {
                    JProgressBar bar = progressBar.getProgressBar();
                    bar.setMaximum(maximumValue);
                    bar.setValue(value);
                    bar.setString(text);
                });
        }
    }

    public void hideProgressDialog() {
        if (progressBar != null) {
            progressBar.setVisible(false);
            progressBar = null;
        }
    }

    public static void main(String[] args) throws Exception {
        List<InputCodec<InterpretedTreeAutomaton>> irtgCodecs = InputCodec.getInputCodecs(InterpretedTreeAutomaton.class);
        for (InputCodec i : irtgCodecs) {
            System.err.println(i.getMetadata());
        }
    }
}
