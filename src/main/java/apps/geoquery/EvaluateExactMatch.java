/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package apps.geoquery;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author christoph_teichmann
 */
public class EvaluateExactMatch {

    public static void main(String... args) throws IOException {
        String inFileName = args[0];
        String goldNumberString = args[1];
        String proposalNumberString = args[2];

        int goldPos = Integer.parseInt(goldNumberString);
        int propPos = Integer.parseInt(proposalNumberString);

        double seen = 0.0;
        double proposed = 0.0;
        double correct = 0.0;

        try (BufferedReader input = new BufferedReader(new FileReader(inFileName))) {
            List<String> lines = new ArrayList<>();
            String line;

            while ((line = input.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    if (!lines.isEmpty()) {
                        String gold = lines.get(goldPos);
                        String prop = lines.get(propPos);

                        if (prop.equals("NULL")) {
                            seen += 1.0;
                        } else {
                            seen += 1.0;
                            proposed += 1.0;
                            correct += gold.equals(prop) ? 1.0 : 0.0;
                        }
                        
                        lines.clear();
                    }
                } else {
                    lines.add(line);
                }
            }

            if (!lines.isEmpty()) {
                String gold = lines.get(goldPos);
                String prop = lines.get(propPos);

                if (prop.equals("NULL")) {
                    seen += 1.0;
                } else {
                    seen += 1.0;
                    proposed += 1.0;
                    correct += gold.equals(prop) ? 1.0 : 0.0;
                }
                
                lines.clear();
            }

            System.out.println("seen: "+seen);
            System.out.println("proposed: "+proposed);
            System.out.println("correct: "+correct);
            
            double prec;
            System.out.println("precision: "+(prec = correct/proposed));
            
            double rec;
            System.out.println("recall: "+(rec = correct/seen));
            
            System.out.println("fscore: "+(2*(prec*rec)/(prec+rec)));
        }
    }
}
