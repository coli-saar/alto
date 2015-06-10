/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.codec;

import de.saar.basic.StringTools;
import de.up.ling.irtg.util.Util;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;

/**
 *
 * @author koller
 */
@CodecMetadata(name = "list", description = "space-separated (e.g. words)", type = List.class)
public class ListOutputCodec extends OutputCodec<List> {
    @Override
    public void write(List list, OutputStream ostream) throws IOException, UnsupportedOperationException {
        PrintWriter w = new PrintWriter(new OutputStreamWriter(ostream));
        w.write(StringTools.join(Util.mapToList(list, x -> x.toString()), " "));
        w.flush();
    }
    
}
