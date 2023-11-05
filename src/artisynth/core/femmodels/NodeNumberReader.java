package artisynth.core.femmodels;

import java.io.*;
import java.util.*;

import maspack.util.*;

/**
 * Reads a set of FEM nodes from a file containing their node numbers.  The
 * file is a simple text file containing integers (the node numbers), separated
 * by white space. There is no limit to how many numbers can appear on a line
 * but typically this is limited to ten or so to make the file more
 * readable. Optionally, the numbers can be surrounded by square brackets
 * ({@code [ ]}) The special character `{@code #}' is a comment character,
 * commenting out all characters from itself to the end of the current
 * line. For a file) containing the node numbers 2, 12, 4, 8, 23 and 47, the
 * following formats are all valid:
 *
 * <pre>
 * 2 12 4 8 23 47
 * </pre>
 *
 * <pre>
 * [ 2 12 4 8 23 47 ]
 * </pre>
 *
 * <pre>
 * # this is a node number file
 * [ 2 12 4 8 
 *   23 47
 * ]
 * </pre>
 */
public class NodeNumberReader {

   protected ReaderTokenizer myRtok;
   protected boolean myParsedBrackets;

   public NodeNumberReader (ReaderTokenizer rtok) {
      myRtok = rtok;
   }

   public NodeNumberReader (Reader reader) {
      this (new ReaderTokenizer (new BufferedReader (reader)));
   }

   public NodeNumberReader (File file) throws IOException {
      this (new FileReader (file));
   }

   public NodeNumberReader (String fileName) throws IOException {
      this (new File (fileName));
   }

   /**
    * Queries whether the most recently read node numbers were enclosed in
    * brackets.
    * 
    * @return {@code true} if last nodes read were bracketed.
    */
   public boolean nodesWereBracketed() {
      return myParsedBrackets;
   }
   
   /**
    * Reads a set of nodes for a FEM model, based on the node numbers in the
    * input stream. The format is described in the class documentation.
    *
    * @param fem FEM model containing the nodes
    * @return a list of the nodes
    * @throws IOException if an I/O error occurred
    */
   public ArrayList<FemNode3d> read (FemModel3d fem)
      throws IOException {

      ArrayList<FemNode3d> nodes = new ArrayList<>();
      int endToken = ReaderTokenizer.TT_EOF;
      if (myRtok.nextToken() == '[') {
         myParsedBrackets = true;
         endToken = ']';
      }
      else {
         myParsedBrackets = false;
         myRtok.pushBack();
      }
      while (myRtok.nextToken() != endToken) {
         if (!myRtok.tokenIsInteger()) {
            throw new IOException ("Expected node number, got " + myRtok);
         }
         FemNode3d node = fem.getNodeByNumber ((int)myRtok.lval);
         if (node == null) {
            System.out.println (
               "WARNING: node number "+myRtok.lval+" not found");
         }
         else {
            nodes.add (node);
         }
      }
      return nodes;
   }

   public void close() {
      myRtok.close();
   }

   public void finalize() {
      close();
   }

   /**
    * Reads node numbers from a file and uses them to locate nodes within a
    * specified FEM model. The format is described in the class
    * documentation. Numbers which do <i>not</i> correspond to nodes with the
    * model are ignored and cause a warning message to be printed.
    *
    * @param file file to read the node numbers from
    * @param fem FEM model containing the nodes
    * @return a list of the identified nodes
    * @throws IOException if an I/O error occurred
    */
   public static ArrayList<FemNode3d> read (File file, FemModel3d fem)
      throws IOException {

      NodeNumberReader reader = null;
      ArrayList<FemNode3d> nodes = null;
      try {
         reader = new NodeNumberReader (file);
         nodes = reader.read (fem);
      }
      catch (Exception e) {
         throw e;
      }
      finally {
         if (reader != null) {
            reader.close();
         }
      }
      return nodes;
   }

   /**
    * Reads node numbers from a file and uses them to locate nodes within a
    * specified FEM model. The format is described in the class
    * documentation. Numbers which do <i>not</i> correspond to nodes with the
    * model are ignored and cause a warning message to be printed.
    *
    * @param filePath path name of the file to read the node numbers from
    * @param fem FEM model containing the nodes
    * @return a list of the identified nodes
    * @throws IOException if an I/O error occurred
    */
   public static ArrayList<FemNode3d> read (String filePath, FemModel3d fem)
      throws IOException {
      return read (new File(filePath), fem);
   }

}
