/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.corpus;

import de.up.ling.irtg.InterpretedTreeAutomaton;
import de.up.ling.irtg.automata.ConcreteTreeAutomaton;
import de.up.ling.irtg.automata.TreeAutomaton;
import java.util.Iterator;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

/**
 *
 * @author koller
 */
public class Charts implements Iterable<TreeAutomaton> {

    static {
        System.setProperty("objectdb.conf", "./objectdb.conf");
    }
    private EntityManager em;

    public Charts(String connectionURL) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(connectionURL);
        em = emf.createEntityManager();
    }

    private class ResultsIterator implements Iterator<TreeAutomaton> {

        private long i = 1;
        private long numResults;

        public ResultsIterator() {
            Query countQuery = em.createQuery("select count(o) from ConcreteTreeAutomaton o");
            numResults = (Long) countQuery.getSingleResult();
        }

        public boolean hasNext() {
            return i <= numResults;
        }

        public TreeAutomaton next() {
            return em.find(ConcreteTreeAutomaton.class, i++);
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    public Iterator<TreeAutomaton> iterator() {
        return new ResultsIterator();
    }

    public static void computeCharts(Corpus corpus, InterpretedTreeAutomaton irtg, String connectionURL) throws Exception {
        System.setProperty("objectdb.conf", "./objectdb.conf");
        
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(connectionURL);
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();

        for (Instance inst : corpus) {
            TreeAutomaton chart = irtg.parseInputObjects(inst.getInputObjects());
            ConcreteTreeAutomaton x = chart.asConcreteTreeAutomaton();
            em.persist(x);
        }

        em.getTransaction().commit();
    }
}
