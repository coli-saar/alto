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
public interface NumberCodec {
    int readInt() throws IOException;
    int readSignedInt() throws IOException;
    long readLong() throws IOException;
    long readSignedLong() throws IOException;
    double readDouble() throws IOException;
    
    long writeInt(int value) throws IOException;
    long writeSignedInt(int value) throws IOException;
    long writeLong(long value) throws IOException;
    long writeSignedLong(long value) throws IOException;
    long writeDouble(double value) throws IOException;
}
