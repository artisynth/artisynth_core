package artisynth.core.mechmodels;

import java.util.*;
import maspack.util.*;

import artisynth.core.mechmodels.Collidable.Group;

/**
 * Structure to store ColllisionHandlers within a CollisionManager.  The
 * structure takes the form of a n X n sparse upper triangular matrix, where n
 * is the total number of collidable bodies that can be associated with the
 * handlers. The "indices" for this matrix are provided by each body's
 * <code>getCollisionIndex()</code> method. Each handler is associated with a 
 * pair of bodies, and since these pairings are symmetric, we only need store 
 * the upper triangular part of the matrix. The "indices" for this matrix 
 * are given by the <i>collision index</i> for each body.
 *
 * <p>Each body is associated with an <code>Anchor</code> structure that
 * contains linked lists to the other handlers in both the k-th row and column. 
 * Note that entries within these lists a <i>not</i> sorted in index order. 
 * Each body's anchor is obtained using a HashMap.
 */
public class CollisionHandlerTable {

   private CollisionManager myManager;

   private class Anchor {
      CollisionHandler myRowHead;
      CollisionHandler myRowTail;
      CollisionHandler myColHead;
      CollisionHandler myColTail;

      Anchor () {
      }

      void addRow (CollisionHandler ch) {
         ch.setNext (null);
         if (myRowTail == null) {
            myRowHead = ch;
         }
         else {
            myRowTail.setNext (ch);
         }
         myRowTail = ch;
      }

      void addCol (CollisionHandler ch) {
         ch.setDown (null);
         if (myColTail == null) {
            myColHead = ch;
         }
         else {
            myColTail.setDown (ch);
         }
         myColTail = ch;
      }

      void removeInactiveHandlers () {
         CollisionHandler prev = null;
         CollisionHandler next;
         for (CollisionHandler ch=myRowHead; ch!=null; ch=next) {
            next = ch.getNext();
            if (!ch.isActive()) {
               if (prev == null) {
                  myRowHead = next;
               }
               else {
                  prev.setNext (next);
               }
               if (next == null) {
                  myRowTail = prev;
               }
            }
            else {
               prev = ch;
            }
         }
         prev = null;
         for (CollisionHandler ch=myColHead; ch!=null; ch=next) {
            next = ch.getDown();
            if (!ch.isActive()) {
               if (prev == null) {
                  myColHead = next;
               }
               else {
                  prev.setDown (next);
               }
               if (next == null) {
                  myColTail = prev;
               }
            }
            else {
               prev = ch;
            }
         }
      }

      void collectRowHandlers (ArrayList<CollisionHandler> handlers) {
         for (CollisionHandler ch=myRowHead; ch!=null; ch=ch.getNext()) {
            handlers.add (ch);
         }
      }

      void setRowActivity (boolean active) {
         for (CollisionHandler ch=myRowHead; ch!=null; ch=ch.getNext()) {
            ch.setActive (active);
         }
      }

      void setColActivity (boolean active) {
         for (CollisionHandler ch=myColHead; ch!=null; ch=ch.getDown()) {
            ch.setActive (active);
         }
      }

      void collectHandlers (ArrayList<CollisionHandler> handlers) {
         for (CollisionHandler ch=myRowHead; ch!=null; ch=ch.getNext()) {
            handlers.add (ch);
         }
         for (CollisionHandler ch=myColHead; ch!=null; ch=ch.getDown()) {
            handlers.add (ch);
         }           
      }
      
      int numRowEntries() {
         int num = 0;
         for (CollisionHandler ch=myRowHead; ch!=null; ch=ch.getNext()) {
            num++;
         }
         return num;
      }
      
      int numColEntries() {
         int num = 0;
         for (CollisionHandler ch=myColHead; ch!=null; ch=ch.getDown()) {
            num++;
         }
         return num;
      }
   }

   public CollisionHandlerTable (CollisionManager manager) {
      myAnchors = new LinkedHashMap<CollidableBody,Anchor>();
      myManager = manager;
   }

   private LinkedHashMap<CollidableBody,Anchor> myAnchors;

   void setHandlerActivity (boolean activity) {
      for (Anchor a : myAnchors.values()) {
         a.setRowActivity (activity);
      }
   }

   void removeInactiveHandlers () {
      for (Anchor a : myAnchors.values()) {
         a.removeInactiveHandlers ();
      }
   }

   void collectHandlers (ArrayList<CollisionHandler> handlers) {
      for (Anchor a : myAnchors.values()) {
         a.collectRowHandlers (handlers);
      }
   }

   public void collectHandlers (
      ArrayList<CollisionHandler> handlers, CollidablePair pair) {
      Collidable c0 = pair.myComp0;
      Collidable c1 = pair.myComp1;

      if (c0 instanceof Group) {
         // swap so DefaultCollidable is last
         Collidable tmp = c0;
         c0 = c1;
         c1 = tmp;
      }
      if (c1 == c0) {
         c1 = Collidable.Self;
      }
      if (c1 instanceof Group) {
         Group g1 = (Group)c1;
         if (g1.includesSelf()) {
            // self collision case
            if (c0.isCompound()) {
               ArrayList<CollidableBody> internals = 
                  CollisionManager.getInternallyCollidableBodies (c0);
               for (int i=0; i<internals.size(); i++) {
                  CollidableBody cbi = internals.get(i);
                  for (int j=i+1; j<internals.size(); j++) {
                     CollidableBody cbj = internals.get(j);
                     CollisionHandler handler = get (cbi, cbj);
                     if (handler != null) {
                        handlers.add (handler);
                     }
                  }
               }
            }      
         }
         if (g1.includesRigid() || g1.includesDeformable()) {
            ArrayList<CollidableBody> externals =
               CollisionManager.getExternallyCollidableBodies (c0);
            ArrayList<CollisionHandler> chlist =
               new ArrayList<CollisionHandler>();
            for (int i=0; i<externals.size(); i++) {
               CollidableBody cbi = externals.get(i);
               Anchor anchor = myAnchors.get(cbi);
               chlist.clear();
               anchor.collectHandlers (chlist);
               for (CollisionHandler ch : chlist) {
                  CollidableBody cbj = ch.getOtherCollidable (cbi);
                  if (CollisionManager.nearestCommonCollidableAncestor (
                         cbi, cbj) == null) {
                     if ((g1.includesRigid() && !cbj.isDeformable()) ||
                         (g1.includesDeformable() && cbj.isDeformable())) {
                        handlers.add (ch);
                     }
                  }
               }     
            }
         }
      }
      else {
         if ((c0.getCollidableAncestor() == c1) ||
             (c1.getCollidableAncestor() == c0)) {
            return;
         }
         else if (CollisionManager.isCollidableBody(c0) &&
                  CollisionManager.isCollidableBody(c1)) {
            // check for specific pair
            CollisionHandler handler =
               get ((CollidableBody)c0, (CollidableBody)c1);
            if (handler != null) {
               handlers.add (handler);
            }
         }
         else {
            // check for external handlers
            ArrayList<CollidableBody> externals0 =
               CollisionManager.getExternallyCollidableBodies (c0);
            ArrayList<CollidableBody> externals1 =
               CollisionManager.getExternallyCollidableBodies (c1);
            for (int i=0; i<externals0.size(); i++) {
               CollidableBody cbi = externals0.get(i);
               for (int j=0; j<externals1.size(); j++) {
                  CollidableBody cbj = externals1.get(j);
                  CollisionHandler handler = get (cbi, cbj);
                  if (handler != null) {
                     handlers.add (handler);
                  }
               }
            }
         }
      }
   }

   public void clear() {
      myAnchors.clear();
   }

   public void initialize (List<CollidableBody> collidables) {
      myAnchors.clear();
      for (CollidableBody cbody : collidables) {
         myAnchors.put (cbody, new Anchor ());
      }
   }

   /**
    * Reinitializes the table with a new set of collidables. Keeps
    * any existing active handlers for collidables that were previously
    * present, and discards handlers that are inactive or belong to
    * collidables that are not in the new set.
    *  
    * @param collidables new collidables
    */
   public void reinitialize (List<CollidableBody> collidables) {

      if (myAnchors.size() == 0) {
         // if no existing anchors, initialize is faster
         initialize (collidables);
         return;
      }
      
      LinkedHashSet<CollidableBody> newCollidables =
         new LinkedHashSet<CollidableBody>();
      newCollidables.addAll (collidables);

      boolean removeNeeded = false;
      for (Map.Entry<CollidableBody,Anchor> e : myAnchors.entrySet()) {
         CollidableBody cbody = e.getKey();         
         Anchor anchor = e.getValue();
         if (newCollidables.contains (cbody)) {
            newCollidables.remove (cbody);
         }
         else {
            anchor.setRowActivity (false);
            anchor.setColActivity (false);
            myAnchors.remove (anchor);
            removeNeeded = true;
         }
      }

      if (removeNeeded) {
         removeInactiveHandlers();
      }
      ArrayList<CollisionHandler> handlers = new ArrayList<CollisionHandler>();
      collectHandlers(handlers);
      if (!newCollidables.isEmpty()) {
         for (CollidableBody cbody : newCollidables) {
            myAnchors.put (cbody, new Anchor ());
         }
      }

      setHandlerActivity (false); // XXXX
   }

   public CollisionHandler get (CollidableBody col0, CollidableBody col1) {
      Anchor anchor0 = myAnchors.get(col0);
      Anchor anchor1 = myAnchors.get(col1);
      if (col0.getCollidableIndex() <= col1.getCollidableIndex()) {
         for (CollisionHandler ch=anchor0.myRowHead; ch!=null; ch=ch.getNext()) {
            if (ch.getCollidable(1) == col1) {
               return ch;
            }
         }
      }
      else {
         for (CollisionHandler ch=anchor1.myRowHead; ch!=null; ch=ch.getNext()) {
            if (ch.getCollidable(1) == col0) {
               return ch;
            }
         }
      }
      return null;
   }

   public CollisionHandler put (CollidableBody col0, CollidableBody col1, 
      CollisionBehavior behav) {
      
      CollisionHandler ch;
      Anchor anchor0 = myAnchors.get(col0);
      Anchor anchor1 = myAnchors.get(col1);
      if (col0.getCollidableIndex() <= col1.getCollidableIndex()) {
         ch = new CollisionHandler (myManager, col0, col1, behav);
         anchor0.addRow (ch);
         anchor1.addCol (ch);
      }
      else {
         ch = new CollisionHandler (myManager, col1, col0, behav);
         anchor1.addRow (ch);
         anchor0.addCol (ch);
      }
      return ch;
   }

   /**
    * Number of handlers currently in the table
    */
   public int size() {
      int num = 0;
      for (Anchor anchor : myAnchors.values()) {
         num += anchor.numRowEntries();
      }
      return num;
   }

   public void skipAuxState (DataBuffer data) {
      CollisionHandler stub = new CollisionHandler(myManager, null, null, null);
      int numh = data.zget();
      for (int k=0; k<numh; k++) {
         data.oskip (3);
         stub.skipAuxState (data);
      }
   }

   public void getAuxState (DataBuffer data) {
      int numh = size();
      data.zput (numh);
      for (Anchor anchor : myAnchors.values()) {
         for (CollisionHandler ch=anchor.myRowHead; ch!=null; ch=ch.getNext()) {
            data.oput (ch.getCollidable(0));
            data.oput (ch.getCollidable(1));
            data.oput (ch.getBehavior());
            ch.getAuxState (data);
         }
      }
   }

   public void getInitialAuxState (DataBuffer newData, DataBuffer oldData) {
      // just create a state in which there are no contacts
      newData.zput (0);
   }

   public void setAuxState (DataBuffer data) {
      int numh = data.zget();
      setHandlerActivity (false);
      for (int k=0; k<numh; k++) {
         CollidableBody cb0 = (CollidableBody)data.oget();
         CollidableBody cb1 = (CollidableBody)data.oget();
         CollisionBehavior behav = (CollisionBehavior)data.oget();
         CollisionHandler ch = get (cb0, cb1);
         if (ch == null) {
            ch = put (cb0, cb1, behav);
         }
         else if (ch.getBehavior() != behav) {
            // not sure if ch.behavior will be different; just being sure
            ch.setBehavior(behav);
         }
         ch.setActive (true);
         ch.setAuxState (data);
      }
      removeInactiveHandlers();
   }
}
