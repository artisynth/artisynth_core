package artisynth.core.modelbase;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;

import artisynth.core.util.ScanToken;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;

/**
 * Template ModelComponent instance illustrating the code needed to implement
 * scan and write.
 */
public class ScanWriteTemplate extends ModelComponentBase {

   ModelComponent myRefComp;
   Vector3d myVec = new Vector3d();
   double myScalar = 0;
   String myString;

   public ScanWriteTemplate () {
   }

   public ScanWriteTemplate (ModelComponent refComp) {
      myRefComp = refComp;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      if (myRefComp != null) {
         pw.println (
            "refComp="+ComponentUtils.getWritePathName (ancestor,myRefComp));
      }
      pw.print ("vec=");
      myVec.write (pw, fmt, /*withBrackets=*/true);
      pw.println ("");
      pw.print ("scalar=" + myScalar);
      pw.print ("string=\"" + myString + "\"");
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "refComp", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "vec")) {
         myVec.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "scalar")) {
         myScalar = rtok.scanNumber();
         return true;
      }
      else if (scanAttributeName (rtok, "string")) {
         myString = rtok.scanQuotedString('"');
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "refComp")) {
         myRefComp = 
            postscanReference (tokens, ModelComponent.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }


}
