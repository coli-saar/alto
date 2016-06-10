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
    public int readInt() throws IOException;
    public int readSignedInt() throws IOException;
    public long readLong() throws IOException;
    public long readSignedLong() throws IOException;
    public double readDouble() throws IOException;
    
    public long writeInt(int value) throws IOException;
    public long writeSignedInt(int value) throws IOException;
    public long writeLong(long value) throws IOException;
    public long writeSignedLong(long value) throws IOException;
    public long writeDouble(double value) throws IOException;    
}
