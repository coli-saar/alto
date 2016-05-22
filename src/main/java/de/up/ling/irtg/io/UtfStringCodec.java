/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 * @author koller
 */
public class UtfStringCodec implements StringCodec {

    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public UtfStringCodec(ObjectInputStream ois) throws IOException {
        this.ois = ois;
        oos = null;
    }

    public UtfStringCodec(ObjectOutputStream oos) throws IOException {
        this.oos = oos;
        ois = null;
    }

    @Override
    public long writeString(String s) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream obaos = new ObjectOutputStream(baos);
        obaos.writeUTF(s);
        obaos.close();

        oos.writeUTF(s);

        return baos.toByteArray().length;
    }

    @Override
    public String readString() throws IOException {
        return ois.readUTF();
    }

}
