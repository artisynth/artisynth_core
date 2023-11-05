package artisynth.core.femmodels;

import java.io.*;
import java.util.*;

import maspack.util.*;
import artisynth.core.modelbase.ComponentUtils;

/**
 * Writes a set of FEM nodes to a text file in the form of their node numbers.
 * The format is described in the class documentation for {@link
 * NodeNumberReader}.
 */
public class NodeNumberWriter {

   /**
    * Flag that causes the written numbers to be enclosed in square brackets
    * {@code [ ]}.
    */
   public static int USE_BRACKETS = 0x01;

   /**
    * Flag that causes the output to start with a comment identifying the FEM
    * model to which the nodes belong.
    */
   public static int ADD_HEADER_COMMENT = 0x02;

   protected IndentingPrintWriter myPw;
   protected int myMaxColumns = 10;
   protected int myFlags = 0;

   public NodeNumberWriter (PrintWriter writer) {
      myPw = new IndentingPrintWriter(writer);
   }

   public NodeNumberWriter (Writer writer) {
      this (new PrintWriter (new BufferedWriter (writer)));
   }

   public NodeNumberWriter (File file) throws IOException {
      this (new FileWriter (file));
   }

   public NodeNumberWriter (String fileName) throws IOException {
      this (new File (fileName));
   }

   public int getMaxColumns() {
      return myMaxColumns;
   }

   public void setMaxColumns (int max) {
      myMaxColumns = max;
   }

   public int getFlags() {
      return myFlags;
   }

   public void setFlags (int flags) {
      myFlags = flags;
   }

   /**
    * Writes the numbers of a set of nodes, using the format settings specified
    * in the class.
    *
    * @param nodes nodes whose numbers should be written
    * @throws IOException if an I/O error occurred or if the nodes do not
    * belong to the same FEM model.
    */
   public void write (Collection<FemNode3d> nodes) throws IOException {

      int col = 0;
      FemModel3d femModel = null;
      // find the FEM model for each node and make sure it's the same
      for (FemNode3d node : nodes) {
         if (node.getGrandParent() instanceof FemModel3d) {
            FemModel3d fem = (FemModel3d)node.getGrandParent();
            if (femModel == null) {
               femModel = fem;
            }
            else if (fem != femModel) {
               throw new IOException (
                  "node "+node.getNumber()+" belongs to a different FEM model");
            }
         }
         else {
            throw new IOException (
               "node "+node.getNumber()+" does not belong to FEM model");
         }
      }
      boolean useBrackets = ((myFlags & USE_BRACKETS) != 0);
      if ((myFlags & ADD_HEADER_COMMENT) != 0) {
         myPw.println (
            "# node numbers from "+ComponentUtils.getPathName(femModel));
      }
      if (useBrackets) {
         myPw.print ("[ ");
         IndentingPrintWriter.addIndentation (myPw, 2);
      }
      for (FemNode3d node : nodes) {
         myPw.print (node.getNumber() + " ");
         if (++col >= myMaxColumns) {
            myPw.println ("");
            col = 0;
         }
      }
      if (col > 0) {
         myPw.println ("");
      }
      if (useBrackets) {
         IndentingPrintWriter.addIndentation (myPw, -2);
         myPw.println ("]");
      }
   }

   public void close() {
      if (myPw != null) {
         myPw.close();
      }
   }

   public void finalize() {
      close();
   }

   /**
    * Writes the numbers of a set of nodes to a file. The output format
    * consists of integers (the node numbers), separated by white space and
    * with up to 10 numbers per line.
    *
    * @param filePath path to the file to be written
    * @param nodes nodes whose numbers should be written
    * @throws IOException if an I/O error occurred or if the nodes do
    * not belong to the same FEM model
    */
   public static void write (String filePath, Collection<FemNode3d> nodes)
      throws IOException {
      write (new File(filePath), nodes, /*maxCols=*/10, /*flags=*/0);
   }

   /**
    * Writes the numbers of a set of nodes to a file. The output format
    * consists of integers (the node numbers), separated by white space and
    * with up to 10 numbers per line.
    *
    * @param file the file to be written
    * @param nodes nodes whose numbers should be written
    * @throws IOException if an I/O error occurred or if the nodes do
    * not belong to the same FEM model
    */
   public static void write (File file, Collection<FemNode3d> nodes)
      throws IOException {
      write (file, nodes, /*maxCols=*/10, /*flags=*/0);
   }

   /**
    * Writes the numbers of a set of nodes to a file. The output format
    * consists of integers (the node numbers), separated by white space and
    * with up to {@code maxCols} numbers per line. The {@code flags} argument
    * may contain the flags {@link #USE_BRACKETS} and {@code
    * #ADD_HEADER_COMMENT}.
    *
    * @param file the file to be written
    * @param nodes nodes whose numbers should be written
    * @param maxCols maximum number of node numbers per line
    * @param flags flags to control the output.
    * combination of {@code USE_BRACKETS} and {@code ADD_HEADER_COMMENT}.
    * @throws IOException if an I/O error occurred or if the nodes do
    * not belong to the same FEM model
    */
   public static void write (
      File file, Collection<FemNode3d> nodes, int maxCols, int flags) 
      throws IOException {      

      NodeNumberWriter writer = null;
      try {
         writer = new NodeNumberWriter (file);
         writer.setMaxColumns (maxCols);
         writer.setFlags (flags);
         writer.write (nodes);
      }
      catch (Exception e) {
         throw e;
      }
      finally {
         if (writer != null) {
            writer.close();
         }
      }
   }
}

