package artisynth.core.workspace;

import java.io.*;
import java.util.*;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;

import maspack.properties.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;

/**
 * Input probe to control the visibility of one or more renderable objects by
 * adjusting the {@code alpha} value of their render properties.
 */
public class TransparencyProbe extends NumericControlProbe {

   private ArrayList<RenderableComponent> myComps;

   public TransparencyProbe () {
      super();
      myComps = new ArrayList<>();
   }

   public TransparencyProbe (
      RenderableComponent comp, double startTime, double stopTime) {
      this (new RenderableComponent[] {comp}, startTime, stopTime);
   }

   protected TransparencyProbe (double startTime, double stopTime) {
      super();
      setVsize (1);
      setStartTime (startTime);
      setStopTime (stopTime);
      VectorNd one = new VectorNd (1);
      one.set (0, 1);
      addData (0, one);
      addData (stopTime-startTime, one);
      myComps = new ArrayList<>();
   }

   public TransparencyProbe (
      Collection<? extends RenderableComponent> comps, 
      double startTime, double stopTime) {
      this (startTime, stopTime);
      for (RenderableComponent c : comps) {
         myComps.add (c);
      }
   }

   public TransparencyProbe (
      RenderableComponent[] comps, double startTime, double stopTime) {
      this (startTime, stopTime);
      for (RenderableComponent c : comps) {
         myComps.add (c);
      }
   }

   public void addFadeIn (double t0, double t1) {
      VectorNd vec = new VectorNd (1);
      vec.set (0, 0);
      addData (t0, vec);
      vec.set (0, 1);
      addData (t1, vec);
   }

   public void addFadeOut (double t0, double t1) {
      VectorNd vec = new VectorNd (1);
      vec.set (0, 1);
      addData (t0, vec);
      vec.set (0, 0);
      addData (t1, vec);
   }

   public void addKnot (double t0, double value) {
      VectorNd vec = new VectorNd (1);
      vec.set (0, value);
      addData (t0, vec);
   }

   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.print ("comps=");
      ScanWriteUtils.writeBracketedReferences (pw, myComps, ancestor);
   }

   public boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanAndStoreReferences (rtok, "comps", tokens) != -1) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   public void applyData (VectorNd vec, double t, double trel) {
      for (RenderableComponent r : myComps) {
         RenderProps.setAlpha (r, vec.get(0));
      }
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      
      if (postscanAttributeName (tokens, "comps")) {
         myComps.clear();
         ScanWriteUtils.postscanReferences (
            tokens, myComps, RenderableComponent.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
}
