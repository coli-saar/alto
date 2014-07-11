/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.script;

import de.up.ling.irtg.algebra.graph.IsiAmrParser;
import de.up.ling.irtg.algebra.graph.LambdaGraph;
import de.up.ling.irtg.algebra.graph.ParseException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLStreamException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author koller
 */
public class AmrbankReader extends DefaultHandler {

    enum Reading {

        NOTHING, SENTENCE, AMR
    }
    Reading currentlyReading = Reading.NOTHING;
    StringBuilder buf = new StringBuilder();
    String id = null;

    public static void main(String[] args) throws XMLStreamException, ParseException, FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        SAXParserFactory parserFactor = SAXParserFactory.newInstance();
        SAXParser parser = parserFactor.newSAXParser();

        DefaultHandler handler = new AmrbankReader();

        parser.parse(new File(args[0]), handler);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if ("sentence".equals(qName)) {
            currentlyReading = Reading.SENTENCE;
            id = attributes.getValue("id");
        } else if ("amr".equals(qName)) {
            currentlyReading = Reading.AMR;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (currentlyReading == Reading.AMR) {
            String x = String.copyValueOf(ch, start, length);
            buf.append(x);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (currentlyReading == Reading.AMR) {
            String x = buf.toString().trim();

            if (!x.matches("\\s*")) {
                try {
                    // TODO - #17 uses node name i before node i/i is declared
//                    System.err.println("||" + x + "||");
//XXX                    LambdaGraph lg = IsiAmrParser.parse(new StringReader(x));
                    LambdaGraph lg = null;
                    System.out.println(lg.toIsiAmrString());
                } catch (Exception ex) {
                    System.err.println("Exception while parsing " + id + ": " + ex);
                }
            }
        }

        currentlyReading = Reading.NOTHING;
        buf = new StringBuilder();
    }
}
