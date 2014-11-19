/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import com.google.common.base.Supplier;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author koller
 */
public class FileInputStreamSupplier implements Supplier<InputStream> {
    private File file;

    public FileInputStreamSupplier(File file) {
        this.file = file;
    }
    
    public InputStream get() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(FileInputStreamSupplier.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    
}
