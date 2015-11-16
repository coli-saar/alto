/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.rule_finding.preprocessing;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author christoph_teichmann
 */
public class ExtractStringFunqlGeoquery {

    /**
     * 
     * @param args
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException 
     */
    public static void main(String... args) throws IOException, ParserConfigurationException, SAXException{
        try (InputStream in = new FileInputStream(args[0]); OutputStream out = new FileOutputStream(args[1])) {
            obtain(in, out, OutputFormat.ONE_PER_LINE_NEWLINE);
        }
    }
    
    /**
     * 
     * @param is
     * @param storage
     * @param form
     * @throws IOException 
     * @throws javax.xml.parsers.ParserConfigurationException 
     * @throws org.xml.sax.SAXException 
     */
    public static void obtain(final InputStream is, final OutputStream storage, final OutputFormat form) throws IOException, ParserConfigurationException, SAXException{
        SAXParserFactory spf = SAXParserFactory.newInstance();
        SAXParser parser = spf.newSAXParser();
        
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(storage));
        
        DefaultHandler handle = new DefaultHandler() {
            /**
             * 
             */
            private String en = "";
            
            /**
             * 
             */
            private String id = "";
            
            /**
             * 
             */
            private String geo_funql = "";
            
            /**
             * 
             */
            private String geo_prolog = "";
            
            /**
             * 
             */
            private boolean enActive = false;
            
            /**
             * 
             */
            private boolean funqlActive = false;
            
            /**
             * 
             */
            boolean prologActive = false;
            
            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if(this.enActive){
                    this.en += new String(Arrays.copyOfRange(ch, start, start+length));
                }else if(this.funqlActive){
                    this.geo_funql += new String(Arrays.copyOfRange(ch, start, start+length));
                }else if(this.prologActive){
                    this.geo_prolog += new String(Arrays.copyOfRange(ch, start, start+length));
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                switch (qName) {
                    case "example": {
                        try {
                            if(!id.trim().equals("")){
                                out.write(form.format(this.id, this.en, this.geo_funql, this.geo_prolog));
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(ExtractStringFunqlGeoquery.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        this.id = "";
                        this.en = "";
                        this.geo_funql = "";
                        this.geo_prolog = "";
                    }
                    break;
                    case "mrl":
                        this.funqlActive = false;
                        this.prologActive = false;
                        break;
                    case "nl":
                        this.enActive = false;
                        break;
                }
            }

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                switch (qName) {
                    case "example": {
                        this.id = attributes.getValue("id");
                    }
                    break;
                    case "nl":
                        if (attributes.getValue("lang").trim().equals("en")) {
                            this.enActive = true;
                        }
                        break;
                    case "mrl":
                        switch (attributes.getValue("lang").trim()) {
                            case "geo-prolog":
                                this.prologActive = true;
                                break;
                            case "geo-funql":
                                this.funqlActive = true;
                        }
                }
            }

        };
        
        parser.parse(is, handle);
        out.flush();
    }
    
    /**
     * 
     */
    public static enum OutputFormat {
        ONE_PER_LINE_NEWLINE {

            @Override
            public String format(String id, String sentence, String funql, String prolog) {
                return id.trim() + "\n" + sentence.trim() + "\n" + prolog.trim() + "\n" + funql.trim()+"\n\n";
            }
        };
        
        public abstract String format(String id, String sentence, String funql, String prolog);
    }
}
