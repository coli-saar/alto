/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/**
 * An output codec that encodes arbitrary objects by simply
 * calling their {@link Object#toString() } method.
 * 
 * @author koller
 */
@CodecMetadata(name = "toString", description = "encodes an object using its toString method", type = Object.class, displayInPopup = false)
public class ToStringOutputCodec extends OutputCodec<Object> {
    @Override
    public void write(Object object, OutputStream ostream) {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(ostream));
        w.write(object.toString());
        w.flush();
    }
    
}
