/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.io;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author koller
 */
public class FixedNumberCodec implements NumberCodec {    
    ObjectInputStream ois;
    ObjectOutputStream oos;

    public FixedNumberCodec(ObjectInputStream ois) throws IOException {
        this.ois = ois;
        oos = null;
    }

    public FixedNumberCodec(ObjectOutputStream oos) throws IOException {
        this.oos = oos;
        ois = null;
    }
   
    @Override
    public int readInt() throws IOException {
        return ois.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return ois.readLong();
    }

    @Override
    public double readDouble() throws IOException {
        return ois.readDouble();
    }
    
    @Override
    public long writeInt(int value) throws IOException {
        oos.writeInt(value);
        return 4;
    }

    @Override
    public long writeLong(long value) throws IOException {
        oos.writeLong(value);
        return 8;
    }

    @Override
    public long writeDouble(double value) throws IOException {
        oos.writeDouble(value);
        return 8;
    }

    @Override
    public int readSignedInt() throws IOException {
        return readInt();
    }

    @Override
    public long readSignedLong() throws IOException {
        return readLong();
    }

    @Override
    public long writeSignedInt(int value) throws IOException {
        return writeInt(value);
    }

    @Override
    public long writeSignedLong(long value) throws IOException {
        return writeLong(value);
    }
    
}
