/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import com.google.common.base.Supplier;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 *
 * @author koller
 */
public class ByteArrayInputStreamSupplier implements Supplier<InputStream> {
    private byte[] byteArray;

    public ByteArrayInputStreamSupplier(byte[] byteArray) {
        this.byteArray = byteArray;
    }
    
    public InputStream get() {
        return new ByteArrayInputStream(byteArray);
    }    
}
