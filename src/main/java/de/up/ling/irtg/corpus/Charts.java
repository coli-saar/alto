/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Iterator;
import java.util.Properties;
import javax.jdo.Extent;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

/**
 *
 * @author koller
 */
public class Charts implements Iterable<TreeAutomaton> {
    private PersistenceManager pm;

    public Charts(String connectionURL) {
        Properties properties = new Properties();
        properties.setProperty("javax.jdo.PersistenceManagerFactoryClass", "com.objectdb.jdo.PMF");
        properties.setProperty("javax.jdo.option.ConnectionURL", connectionURL);
        
        PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(properties);
        pm = pmf.getPersistenceManager();
    }

    public Iterator<TreeAutomaton> iterator() {
        Extent extent = pm.getExtent(ConcreteTreeAutomaton.class, false);
        Iterator itr = extent.iterator();
        return itr;
    }

    public static void computeCharts(Corpus corpus, InterpretedTreeAutomaton irtg, String connectionURL) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("javax.jdo.PersistenceManagerFactoryClass", "com.objectdb.jdo.PMF");
        properties.setProperty("javax.jdo.option.ConnectionURL", connectionURL);
        
        PersistenceManagerFactory pmf = JDOHelper.getPersistenceManagerFactory(properties);
        PersistenceManager pm = pmf.getPersistenceManager();

        try {
            pm.currentTransaction().begin();

            for (Instance inst : corpus) {
                TreeAutomaton chart = irtg.parseInputObjects(inst.getInputObjects());
                pm.makePersistent(chart.asConcreteTreeAutomaton());
            }

            pm.currentTransaction().commit();
        } finally {
            // Close the database and active transaction:
            if (pm.currentTransaction().isActive()) {
                pm.currentTransaction().rollback();
            }
            if (!pm.isClosed()) {
                pm.close();
            }
        }
    }
}
