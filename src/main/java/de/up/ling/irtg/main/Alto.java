/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import de.up.ling.irtg.gui.GuiMain;
import de.up.ling.irtg.util.BuildProperties;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author koller
 */
public class Alto {
    // Class is named Alto because as of MacOS Mavericks, that determines
    // the name in the menu bar.
    
    private static final String PROGRAM_NAME = "java -jar alto.jar";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            GuiMain.main(args);
        } else {
            CmdLineParameters param = new CmdLineParameters();
            JCommander jc = new JCommander(param);
            jc.setProgramName(PROGRAM_NAME);

            try {
                jc.parse(args);
            } catch (ParameterException e) {
                printVersion();
                System.err.println(e.getMessage());
                System.err.println("Run '" + PROGRAM_NAME + " --help' to see all command-line options.\n");
                System.exit(0);
            }

            if (param.help) {
                printVersion();
                jc.usage();
                System.exit(0);
            } else {
                System.err.println(param.parameters);
            }
        }
    }
    
    private static void printVersion() {
        System.err.println("This is Alto " + BuildProperties.getVersion() + ", build " + BuildProperties.getBuild() + ".");
    }


    public static class CmdLineParameters {

        @Parameter
        private List<String> parameters = new ArrayList<>();

        @Parameter(names = "--help", help = true)
        private boolean help;

    }
}
