/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.util.Comparator;

import maspack.graph.Node;

public class MenuCompareByNameButDemosFirst implements
Comparator<Node<MenuNode>> {
   public int compare(Node<MenuNode> o1, Node<MenuNode> o2) {

      // if one has no children, put it first
      if ((o1.getNumberOfChildren() == 0) && (o2.getNumberOfChildren() == 0)) {
	 return o1.getData().compareTo(o2.getData());
      } else if (o1.getNumberOfChildren() == 0) {
	 return -1;
      } else if (o2.getNumberOfChildren() == 0) { return 1; }

      // if both have children (i.e. submenus), compare by name
      return o1.getData().compareTo(o2.getData());
   }
}