/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.io;

import java.io.IOException;

/**
 *
 * @author koller
 */
public interface StringCodec {
    public long writeString(String s) throws IOException;
    public String readString() throws IOException;
}
