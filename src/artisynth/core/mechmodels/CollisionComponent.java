package artisynth.core.mechmodels;

import java.io.*;
import java.util.Deque;
import java.util.List;

import maspack.util.*;
import maspack.render.*;
import maspack.properties.*;
import maspack.render.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.*;

/**
 * Base class for both CollisionBehavior and CollisionResponse objects.
 */
public class CollisionComponent extends ModelComponentBase {

   CollidablePair myPair = null;

   public CollidablePair getCollidablePair() {
      return myPair;
   }

   public Collidable getCollidable (int cidx) {
      return myPair.get(cidx);
   }

   public void setCollidablePair (CollidablePair pair) {
      myPair = new CollidablePair (pair);
   }

   public void setCollidablePair (Collidable c0, Collidable c1) {
      myPair = new CollidablePair (c0, c1);
   }

   private String getReferenceName (
      Collidable col, CompositeComponent ancestor)
      throws IOException {
      if (col instanceof Collidable.Group) {
         return col.getName();
      }
      else {
         return ComponentUtils.getWritePathName (ancestor, col);
      }
   }

   Collidable postscanReference (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      ScanToken tok = tokens.poll();
      if (tok instanceof StringToken) {
         StringToken strtok = (StringToken)tok;
         String str = strtok.value();
         if (str.equals ("Deformable")) {
            return Collidable.Deformable;
         }
         else if (str.equals ("Rigid")) {
            return Collidable.Rigid;
         }
         else if (str.equals ("AllBodies")) {
            return Collidable.AllBodies;
         }
         else if (str.equals ("All")) {
            return Collidable.All;
         }
         else if (str.equals ("Self")) {
            return Collidable.Self;
         }
         else {
            return ScanWriteUtils.postscanReference (
               strtok, Collidable.class, ancestor);
         }
      }
      else {
         throw new IOException ("Token "+tok+" is not a string token");
      }      
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "collidable0", tokens)) {
         return true;
      }         
      else if (scanAndStoreReference (rtok, "collidable1", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "collidable0")) {
         if (myPair == null) {
            myPair = new CollidablePair(null, null);
         }
         myPair.myComp0 = postscanReference (tokens, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "collidable1")) {
         if (myPair == null) {
            myPair = new CollidablePair(null, null);
         }
         myPair.myComp1 = postscanReference (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   } 

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.println ("collidable0=" + getReferenceName (myPair.myComp0, ancestor));
      pw.println ("collidable1=" + getReferenceName (myPair.myComp1, ancestor));
      super.writeItems (pw, fmt, ancestor); 
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);

      // we distinguish the special default collidables because they will not
      // have parents
      if (myPair.myComp0.getParent() != null) {
         refs.add (myPair.myComp0);
      }
      if (myPair.myComp1.getParent() != null) {
         refs.add (myPair.myComp1);
      }
   }
  

}
