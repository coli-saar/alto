/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "toString", description = "toString", type = Object.class)
public class ToStringOutputCodec extends OutputCodec<Object> {
    @Override
    public void write(Object object, OutputStream ostream) throws IOException {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(ostream));
        w.write(object.toString());
        w.flush();
    }
    
}
