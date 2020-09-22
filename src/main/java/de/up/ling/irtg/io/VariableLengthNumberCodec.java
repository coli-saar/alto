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
public class VariableLengthNumberCodec implements NumberCodec {
    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public VariableLengthNumberCodec(ObjectInputStream ois) {
        this.ois = ois;
        oos = null;
    }

    public VariableLengthNumberCodec(ObjectOutputStream oos) {
        this.oos = oos;
        ois = null;
    }
    
    @Override
    public long writeInt(int value) throws IOException {
        return writeLong(value);
    }

    /**
     * This method only works for value ≥ 0. To encode
     * a value that may be negative, use {@link #writeSignedLong(long) }.
     * @throws IOException 
     */
    @Override
    public long writeLong(long value) throws IOException {
        // from https://techoverflow.net/blog/2013/01/25/efficiently-encoding-variable-length-integers-in-cc/
        long outputSize = 0;
        //While more than 7 bits of data are left, occupy the last output byte
        // and set the next byte flag
        while (value > 127) {
            int byteToWrite = (  ((int) (value & 127L)) | 128);
            // |128: Set the next byte flag
            oos.write(byteToWrite);
            // Remove the seven bits we just wrote
            value >>= 7;
            outputSize++;
        }
        
        int lastByteToWrite = (int) (value & 127L);
        oos.write(lastByteToWrite);
        outputSize++;
        return outputSize;
    }
    

    @Override
    public int readInt() throws IOException {
        // from https://techoverflow.net/blog/2013/01/25/efficiently-encoding-variable-length-integers-in-cc/
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            // max 4 bytes per int
            int next = ois.read();
            ret |= (next & 127) << (7 * i);
            // If the next-byte flag is set
            if ((next & 128) == 0) {
                break;
            }
        }

        return ret;
    }

    @Override
    public long readLong() throws IOException {
        // from https://techoverflow.net/blog/2013/01/25/efficiently-encoding-variable-length-integers-in-cc/
        long ret = 0;
        
        for (int i = 0; i < 8; i++) {
            // max 4 bytes per int
            int next = ois.read();
            ret |= (next & 127) << (7 * i);
            
            // If the next-byte flag is set
            if ((next & 128) == 0) {
                break;
            }
        }
        
        return ret;
    }

    @Override
    public int readSignedInt() throws IOException {
        int sign = ois.read();
        int ret = readInt();
        
        if( sign == 1 ) {
            return -ret;
        } else {
            return ret;
        }
    }

    @Override
    public long readSignedLong() throws IOException {
        int sign = ois.read();
        long ret = readLong();
        
        if( sign == 1 ) {
            return -ret;
        } else {
            return ret;
        }
    }

    @Override
    public long writeSignedInt(int value) throws IOException {
        if( value < 0 ) {
            oos.write(1);
            return writeInt(-value) + 1;
        } else {
            oos.write(0);
            return writeInt(value) + 1;
        }
    }

    @Override
    public long writeSignedLong(long value) throws IOException {
        if( value < 0 ) {
            oos.write(1);
            return writeLong(-value) + 1;
        } else {
            oos.write(0);
            return writeLong(value) + 1;
        }
    }
    
    
    
    
    

    @Override
    public long writeDouble(double value) throws IOException {
        if( value == 1.0 ) {
            oos.write(1);
            return 1;
        } else {
            oos.write(0);
            oos.writeDouble(value);
            return 9;
        }
    }
    
    @Override
    public double readDouble() throws IOException {
        int type = ois.read();
        
        if( type == 1 ) {
            return 1.0;
        } else {
            return ois.readDouble();
        }
    }
}
