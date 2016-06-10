/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.io

import org.junit.*
import java.util.*
import java.io.*
import static org.junit.Assert.*
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.objects.ObjectIterator
import it.unimi.dsi.fastutil.objects.ObjectSet

/**
 *
 * @author koller
 */
class NumberCodecTest {
    @Test
    public void testVariableCodecIntOneByte() {
        runVariableCodecTest(120, false);
    }

    @Test
    public void testVariableCodecIntNegative() {
        runVariableCodecTest(-1, true);
    }

    @Test
    public void testVariableCodecIntTwoBytes() {
        runVariableCodecTest(200, false);
    }

    @Test
    public void testVariableCodecDoubleOne() {
        long len = runVariableCodecTestDouble(1.0);
        assertEquals(1, len);
    }

    @Test
    public void testVariableCodecDoubleNotOne() {
        long len = runVariableCodecTestDouble(3.1415e-2);
        assertEquals(9, len);
    }

    private void runVariableCodecTest(int value, boolean useSigned) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        NumberCodec nc = new VariableLengthNumberCodec(oos);
        
        if( useSigned ) {
            nc.writeSignedInt(value);
        } else {
            nc.writeInt(value);
        }
        oos.flush();
        
        byte[] buf = baos.toByteArray();
        // System.err.println(Arrays.toString(buf));

        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bais);
        nc = new VariableLengthNumberCodec(ois);
        int readValue = useSigned ? nc.readSignedInt() : nc.readInt();

        assertEquals(value, readValue);
    }

    private long runVariableCodecTestDouble(double value) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        NumberCodec nc = new VariableLengthNumberCodec(oos);
        
        long len = nc.writeDouble(value);
        oos.flush();
        
        byte[] buf = baos.toByteArray();
        //System.err.println(Arrays.toString(buf));

        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        ObjectInputStream ois = new ObjectInputStream(bais);
        nc = new VariableLengthNumberCodec(ois);
        double readValue = nc.readDouble();

        assertEquals(value, readValue, 0.00001);
        return len;
    }
}

