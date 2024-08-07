/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.util.Comparator;

import maspack.graph.Node;

public class MenuCompareByNameButDemosLast implements
Comparator<MenuNode> {
   public int compare(MenuNode o1, MenuNode o2) {

      // if one has no children, put it first
      if ((o1.numChildren() == 0) && (o2.numChildren() == 0)) {
	 return o1.compareTo(o2);
      }
      else if (o1.numChildren() == 0) {
	 return 1;
      }
      else if (o2.numChildren() == 0) { 
	 return -1; 
      }

      // if both have children (i.e. submenus), compare by name
      return o1.compareTo(o2);
   }
}
