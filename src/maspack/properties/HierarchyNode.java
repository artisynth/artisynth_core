/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.properties;

import java.util.*;

/**
 * An object which is a node in a hierachy.
 */
public interface HierarchyNode {
   public Iterator<? extends HierarchyNode> getChildren();

   public boolean hasChildren();

   public HierarchyNode getParent();
}
