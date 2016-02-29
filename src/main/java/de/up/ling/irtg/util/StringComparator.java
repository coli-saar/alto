/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.up.ling.irtg.util;

import java.util.Comparator;

/**
 *
 * @author groschwitz
 */
public class StringComparator  implements Comparator<String> {
    
  @Override
  public int compare(String obj1, String obj2) {
    if (obj1 == null) {
        return -1;
    }
    if (obj2 == null) {
        return 1;
    }
    return obj1.compareTo(obj2);
  }
}
