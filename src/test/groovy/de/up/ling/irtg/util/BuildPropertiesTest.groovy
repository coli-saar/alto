/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.up.ling.irtg.util

import org.junit.*
import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author koller
 */
class BuildPropertiesTest {
    @Test
    public void testVersion() {
        assertThat(BuildProperties.getVersion(), is(not(null)))
        assertThat(BuildProperties.getVersion(), is(not("(undefined)")))
    }
    
    @Test
    public void testBuild() {
        assertThat(BuildProperties.getBuild(), is(not(null)))
        assertThat(BuildProperties.getBuild(), is(not("(undefined)")))
    }
}

