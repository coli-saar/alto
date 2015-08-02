/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * Credit:
 * https://stackoverflow.com/questions/527719/how-to-add-hyperlink-in-jlabel
 *
 * @author koller
 */
public class JHyperlink extends JButton implements ActionListener {

    private URI uri;

    public JHyperlink() {
        super();
        init();

        setURI("");
    }

    public JHyperlink(String text, String uri) throws URISyntaxException {
        super(text);
        init();
        
        setURI(uri);
    }

    private void init() {
//        setText("<HTML>Click the <FONT color=\"#000099\"><U>link</U></FONT>"
//                + " to go to the Java website.</HTML>");
        setHorizontalAlignment(SwingConstants.LEFT);
        setBorderPainted(false);
        setOpaque(false);
        setBackground(Color.WHITE);
        setMargin(new Insets(0, 0, 0, 0));
//        setToolTipText(uri.toString());
        addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (IOException e) {
                Logging.get().warning("Opening links is not supported on this platform.");
            }
        }
    }

    public String getURI() {
        if (uri == null) {
            return null;
        } else {
            return uri.toString();
        }
    }

    public void setURI(String uri) {
        try {
            this.uri = new URI(uri);
            setToolTipText(uri);
        } catch (URISyntaxException ex) {
            this.uri = null;
        }
    }

}
