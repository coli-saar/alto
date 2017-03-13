/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.codec;

import de.up.ling.irtg.io.NumberCodec;
import de.up.ling.irtg.io.StringCodec;
import de.up.ling.irtg.signature.Signature;
import java.io.IOException;

/**
 * Use this with NoSignatureBinaryIrtgInputCodec, together with a reference IRTG that contains
 * the signatures that are not written here (note that the mapping from IDs to symbols
 * in the reference IRTG must be identical to the one in the IRTG written here).
 * @author Jonas
 */
public class NoSignatureBinaryIrtgOutputCodec extends BinaryIrtgOutputCodec {

    @Override
    protected long writeSignature(Signature sig, NumberCodec nc, StringCodec sc) throws IOException {
        long bytes = 0;

        bytes += nc.writeInt(0);

        return bytes;
    }

    
    
    
}
