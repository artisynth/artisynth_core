/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.IOException;

public class NonUniqueNameException extends RuntimeException {

   public NonUniqueNameException () {
      super();
   }

   public NonUniqueNameException (String msg) {
      super (msg);
   }

   public static NonUniqueNameException create (
      ModelComponent comp, CompositeComponent parent) {
      String compName = ComponentUtils.getDiagnosticName (comp);
      String parentName = ComponentUtils.getDiagnosticName (parent);
      String msg = "Name for "+compName+" already taken in "+parentName;
      return new NonUniqueNameException (msg);
   }
}
