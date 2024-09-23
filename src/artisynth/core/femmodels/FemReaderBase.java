/**
 * Copyright (c) 2024, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;

import artisynth.core.femmodels.FemElement.ElementClass;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNi;
import maspack.util.ReaderTokenizer;
import maspack.util.ArraySupport;
import maspack.util.DynamicIntArray;
import maspack.util.InternalErrorException;

/**
 * Base class that can be used to implement FEM geometry readers.
 * 
 * @author John E Lloyd
 */
public abstract class FemReaderBase extends FemReaderWriterBase
   implements FemReader {

   /**
    * Option flag for static method calls that tells the reader to number nodes
    * and elements according to {@link Numbering#RESET}.  This option takes
    * precedence over {@link #PRESERVE_NUMBERING_BASE0} and {@link
    * #PRESERVE_NUMBERING}
    */   
   public static int RESET_NUMBERING = 0x1;

   /**
    * Option flag for static method calls that tells the reader to number nodes
    * and elements according to {@link Numbering#PRESERVE}.  This option takes
    * precedence over {@link #PRESERVE_NUMBERING_BASE0}.
    */   
   public static int PRESERVE_NUMBERING = 0x2;

   /**
    * Option flag for static method calls that tells the reader to number nodes
    * and elements according to {@link Numbering#PRESERVE_BASE0}.
    */   
   public static int PRESERVE_NUMBERING_BASE0 = 0x4;

   /**
    * Option flag for static method calls that tells the reader to suppress
    * warning messages.
    */   
   public static int SUPPRESS_WARNINGS = 0x8;

   protected File myFile = null; // file associated with the FEM reader, if any
   protected Reader myReader;    // reader associated with this FEM reader, if any
   protected int myLineCnt;      // number of lines read
   // indicates if orientation warnings have been given for specific element types.
   protected boolean[] myOrientationWarningGiven =
      new boolean[ElemType.values().length];

   // attributes controlling how the FEM is read
   static public boolean DEFAULT_SUPRESS_WARNINGS = false;
   protected boolean mySuppressWarnings = DEFAULT_SUPRESS_WARNINGS;

   static public double DEFAULT_SHELL_THICKNESS = 0.001;
   protected double myShellThickness = DEFAULT_SHELL_THICKNESS;

   static public ShellType DEFAULT_SHELL_TYPE = ShellType.SHELL;
   protected ShellType myShellType = DEFAULT_SHELL_TYPE;

   static public boolean DEFAULT_USE_MEMBRANES = false;
   protected boolean myUseMembranes = DEFAULT_USE_MEMBRANES;

   static public Numbering DEFAULT_NUMBERING = Numbering.RESET;
   protected Numbering myNumbering = DEFAULT_NUMBERING;

   static public boolean DEFAULT_CHECK_ORIENTATION = true;
   protected boolean myCheckOrientation = DEFAULT_CHECK_ORIENTATION;

   static public Vector3d DEFAULT_SCALING = new Vector3d(1,1,1);
   protected Vector3d myScaling = new Vector3d(DEFAULT_SCALING);

   /**
    * Controls how node and element components are numbered when read from
    * input.
    */
   public enum Numbering {
      /**
       * Nodes and elements are numbered according to the ArtiSynth default,
       * with numbers starting at 0 and equal to the component indices.
       * Numbers specified in the input are ignored.
       */
      RESET,

      /**
       * Attempts to preserve the node and element numbering given in the
       * input. This means numbers may not be consecutive, and numbering may
       * not start at 0. If the input numbering is one-based, then the node and
       * element lists will be set to use one-based numbering.
       */
      PRESERVE,

      /**
       * Attempts to preserve the node and element numbering given in the
       * input, expect that if the input numbering is one-based, all numbers
       * will be decremented by one to make the numbering zero-based.
       */
      PRESERVE_BASE0
   }

   public FemReaderBase (Reader reader) {
      myReader = reader;
   }

   public FemReaderBase (File file) throws IOException {
      myReader = new BufferedReader (new FileReader (file));
      myFile = file;
   }

   private void closeQuietly(Reader r) {
      if (r != null) {
         try {
            r.close();
         } catch (IOException e) {}
      }
   }

   public void close() {
      closeQuietly(myReader);
   }   

   /**
    * Prints an orientation warning for a specific element type if that
    * warning has not been printed before.
    */
   void maybePrintOrientationWarning (ElemType type) {
      if (!mySuppressWarnings && !myOrientationWarningGiven[type.ordinal()]) {
         System.out.println (
            "WARNING: fixed orientation for some " +
            type.toString().toLowerCase() + " elements");
         myOrientationWarningGiven[type.ordinal()] = true;
      }
   }

   // used when reseting numbering
   protected int[] myNodeNumMap;

   /**
    * Sets the default thickness value to be used for shell and membrane
    * elements, when it cannot otherwise be inferred from the input.
    *
    * @param thickness default shell/membrane thickness
    */
   public void setShellThickness (double thickness) {
      myShellThickness = thickness;
   }

   /**
    * Queries the default thickness value for shell and membrane
    * elements.
    *
    * @return default shell/membrane thickness
    */
   public double getShellThickness () {
      return myShellThickness;
   }

   /**
    * Sets the default type to use for shell elements when this can not be
    * inferred from the input. The default value is {@link ShellType#SHELL}.
    *
    * @param type default shell type
    */
   public void setShellType (ShellType type) {
      myShellType = type;
   }

   /**
    * Queries the default type to use for shell elements when this can not
    * be inferred from the input. 
    *
    * @return default shell type
    */
   public ShellType getShellType () {
      return myShellType;
   }

   /**
    * Sets how nodes and elements are numbered based on the input.  The default
    * value is {@link Numbering#RESET}.
    *
    * @param numbering controls how components are numbered
    */
   public void setNumbering (Numbering numbering) {
      myNumbering = numbering;
   }

   /**
    * Queries how nodes and elements are numbered based on the input.
    *
    * @return how components are numbered
    */
   public Numbering getNumbering () {
      return myNumbering;
   }

   /**
    * Sets whether or not element orientation should be checked, and if
    * possible fixed. Peforming the check requires computing or estimating
    * the volume of each element. The default value is true.
    *
    * @param enable if {@code true}, enables orientation checking
    */
   public void setCheckOrientation (boolean enable) {
      myCheckOrientation = enable;
   }

   /**
    * Queries whether element orientations are checked and fixed.
    *
    * @return {@code true} if orientation checking is enabled
    */
   public boolean getCheckOrientation () {
      return myCheckOrientation;
   }

   /**
    * Sets whether or not warning messages are suppressed.
    *
    * @param enable if {@code true}, suppresses warnings
    */
   public void setSuppressWarnings(boolean enable) {
      mySuppressWarnings = enable;
   }

   /**
    * Queries whether or not warning messages are suppressed.
    *
    * @return {@code true} if warnings are suppressed
    */
   public boolean getSuppressWarnings() {
      return mySuppressWarnings;
   }
   
   /**
    * Queries the x-y-z scale factors which are applied to nodal coordinates.
    *
    * @return scale factors
    */
   public Vector3d getScaling() {
      return myScaling;
   }

   /**
    * Sets the x-y-z scale factors which are applied to nodal coordinates.
    * Default value is (1,1,1).
    *
    * @param scaling nee scale factors
    */
   public void setScaling (Vector3d scaling) {
      myScaling.set (scaling);
   }

   /**
    * Used by static methods to set options based on a flags argument.
    */
   protected void setOptionsFromFlags (int flags) {
      if ((flags & RESET_NUMBERING) != 0) {
         setNumbering (Numbering.RESET);
      }
      else if ((flags & PRESERVE_NUMBERING) != 0) {
         setNumbering (Numbering.PRESERVE);
      }
      else if ((flags & PRESERVE_NUMBERING_BASE0) != 0) {
         setNumbering (Numbering.PRESERVE_BASE0);
      }
      if ((flags & SUPPRESS_WARNINGS) != 0) {
         setSuppressWarnings (true);
      }
   }

   /**
    * Creates a volumetric element of a given type from a set of nodes.
    */
   protected FemElement3d createVolumetricElement (
      ElemType type, FemNode3d[] nodes) {
      switch (type) {
         case TET: {
            return new TetElement (nodes);
         }
         case HEX: {
            return new HexElement (nodes);
         }
         case PYRAMID: {
            return new PyramidElement (nodes);
         }
         case WEDGE: {
            return new WedgeElement (nodes);
         }
         case QUADTET: {
            return new QuadtetElement (nodes);
         }
         case QUADHEX: {
            return new QuadhexElement (nodes);
         }
         case QUADPYRAMID: {
            return new QuadpyramidElement (nodes);
         }
         case QUADWEDGE: {
            return new QuadwedgeElement (nodes);
         }
         default: {
            throw new InternalErrorException (
               "Unknown volumetric element type " + type);
         }
      }
   }

   /**
    * Creates a volumetric element of a given type from a set of nodes,
    * using an alternate node ordering.
    */
   protected FemElement3d createAlternateElement (
      ElemType type, FemNode3d[] nodes, int[] altNodeOrder) {
      FemNode3d[] inodes = new FemNode3d[nodes.length];
      for (int i=0; i<inodes.length; i++) {
         inodes[i] = nodes[altNodeOrder[i]];
      }
      return createVolumetricElement (type, inodes);
   }
   
   /**
    * Creates a volumetric element of a given type from a set of nodes.  If
    * {@code checkOrientation} is {@code true}, the element's orientation is
    * checked by computing its volume. If the volume is negative, another is
    * element is created using an 'inverse' node ordering that attempts to make
    * the volume positive.
    */
   protected FemElement3d createVolumetricElement (
      ElemType type, FemNode3d[] nodes, boolean checkOrientation) {

      if (!checkOrientation) {
         return createVolumetricElement (type, nodes);
      }
      else {
         switch (type) {
            case QUADTET: {
               if (TetElement.computeVolume (nodes) >= 0) {
                  return new QuadtetElement (nodes);
               }
               else {
                  maybePrintOrientationWarning (type);
                  return createAlternateElement (
                     type, nodes, QuadtetElement.myInverseNodeOrdering);
               }
            }
            case QUADHEX: {
               if (HexElement.computeVolume (nodes) >= 0) {
                  return new QuadhexElement (nodes);
               }
               else {
                  maybePrintOrientationWarning (type);
                  return createAlternateElement (
                     type, nodes, QuadhexElement.myInverseNodeOrdering);
               }
            }
            case QUADPYRAMID: {
               if (PyramidElement.computeVolume (nodes) >= 0) {
                  return new QuadpyramidElement (nodes);
               }
               else {
                  maybePrintOrientationWarning (type);
                  return createAlternateElement (
                     type, nodes, QuadpyramidElement.myInverseNodeOrdering);
               }
            }
            case QUADWEDGE: {
               if (WedgeElement.computeVolume (nodes) >= 0) {
                  return new QuadwedgeElement (nodes);
               }
               else {
                  maybePrintOrientationWarning (type);
                  return createAlternateElement (
                     type, nodes, QuadwedgeElement.myInverseNodeOrdering);
               }
            }
            default: {
               FemElement3d elem = createVolumetricElement (type, nodes);
               if (elem.computeVolumes() < 0) {
                  maybePrintOrientationWarning (type);
                  elem = createAlternateElement (
                     type, nodes, elem.getInverseNodeOrdering());
               }
               return elem;
            }
         }
      }
   }

   /**
    * Creates a shell element of a given type from a set of nodes.
    */
   protected ShellElement3d createShellElement (
      ElemType type, FemNode3d[] nodes, double thickness, ShellType shellType) {
      if (thickness < 0) {
         thickness = myShellThickness;
      }
      if (shellType == null) {
         shellType = myShellType;
      }
      boolean membrane = (shellType==ShellType.MEMBRANE);
      switch (type) {
          case SHELL_TRI: {
             return new ShellTriElement (nodes, thickness, membrane);
         }
         case SHELL_QUAD: {
            return new ShellQuadElement (nodes, thickness, membrane);
         }
         default: {
            throw new InternalErrorException (
               "Unknown shell element type " + type);
         }
      }
   }

   /**
    * Creates an element (either shell or volumetric).
    *
    * @param type element type
    * @param nodes references to the element nodes
    */
   protected FemElement3dBase createElement (ElemType type, FemNode3d[] nodes) {

      if (type.isShell()) {
         return createShellElement (type, nodes, myShellThickness, myShellType);
      }
      else {
         return createVolumetricElement (type, nodes, myCheckOrientation);
      }
   }      

   /**
    * Creates an element (shell or volumetric) and adds it to the FEM model.
    *
    * @param fem FEM model to add element to
    * @param edesc element descriptor
    */
   protected FemElement3dBase createAndAddElement (
      FemModel3d fem, ElemDesc edesc) {

      ElemType type = edesc.myType;
      FemNode3d[] nodes = edesc.myNodes;
      if (type.isShell()) {
         ShellType shellType = edesc.myShellType;
         if (shellType == null) {
            shellType = myShellType;
         }
         ShellElement3d elem =
            createShellElement (type, nodes, myShellThickness, shellType);
         addShellElement (fem, elem, edesc.myTag);
         return elem;
      }
      else {
         FemElement3d elem = createVolumetricElement (
            type, nodes, myCheckOrientation);
         addVolumetricElement (fem, elem, edesc.myTag);
         return elem;
      }
   }      

   /**
    * Adds an element (shell or volumetric) to the FEM model.
    *
    * @param fem FEM model to add element to
    * @param elem element to add
    * @param elemId element number according to the input
    */
   protected void addElement (
      FemModel3d fem, FemElement3dBase elem, int elemId) {

      if (elem.getElementClass() == ElementClass.VOLUMETRIC) {
         addVolumetricElement (fem, (FemElement3d)elem, elemId);
      }
      else {
         addShellElement (fem, (ShellElement3d)elem, elemId);
      }
   }      

   /**
    * Adds a volumetric element to the FEM model.
    *
    * @param fem FEM model to add element to
    * @param elem element to add
    * @param elemId element number according to the input
    */
   protected void addVolumetricElement (
      FemModel3d fem, FemElement3d elem, int elemId) {

      switch (myNumbering) {
         case RESET: {
            fem.addElement (elem);
            break;
         }
         case PRESERVE_BASE0: {
            fem.addNumberedElement (elem, elemId-1);
            break;
         }
         case PRESERVE: {
            fem.addNumberedElement (elem, elemId);
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unknown numbering method: " + myNumbering);
         }
      }
   }

   /**
    * Adds a shell element to the FEM model.
    *
    * @param fem FEM model to add element to
    * @param elem element to add
    * @param elemId element number according to the input
    */
   protected void addShellElement (
      FemModel3d fem, ShellElement3d elem, int elemId) {

      switch (myNumbering) {
         case RESET: {
            fem.addShellElement (elem);
            break;
         }
         case PRESERVE_BASE0: {
            fem.addNumberedShellElement (elem, elemId-1);
            break;
         }
         case PRESERVE: {
            fem.addNumberedShellElement (elem, elemId);
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unknown numbering method: " + myNumbering);
         }
      }
   }

   /**
    * Adds a node to the FEM model.
    *
    * @param fem FEM model to add node to
    * @param node node to add
    * @param nodeId node number according to the input
    */
   protected FemNode3d addNode (
      FemModel3d fem, FemNode3d node, int nodeId) {

      switch (myNumbering) {
         case RESET: {
            fem.addNode (node);
            break;
         }
         case PRESERVE_BASE0: {
            fem.addNumberedNode (node, nodeId-1);
            break;
         }
         case PRESERVE: {
            fem.addNumberedNode (node, nodeId);
            break;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unknown numbering method: " + myNumbering);
         }
      }
      return node;
   }

   /**
    * See if a string starts a given word, ignoreing initial white space.
    */
   protected boolean startsWith (String str, String word) {
      int idx = 0;
      while (idx < str.length() && Character.isWhitespace(str.charAt(idx))) {
         idx++;
      }
      return str.startsWith (word, idx);
   }
   
}
