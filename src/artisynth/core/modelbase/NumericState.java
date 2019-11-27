/**
/* Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.util.DataBuffer.Offsets;

public class NumericState extends DataBuffer implements ComponentState {
   private static final long serialVersionUID = 1L;

   private static final int intsPerLine = 8;
   private static final int doublesPerLine = 4;

   /**
    * Used to delimit the state associated with a particular component, for
    * both diagnostic purposes and for properly restoring component states
    * within {@link HasState#getInitialState}.
    */
   public static class DataFrame {
      HasNumericState myComp;
      int myZoff;
      int myDoff;
      int myOoff;
      int myVersion;
      DataFrame myPrev;

      DataFrame (DataFrame frame, DataFrame prev) {
         myComp = frame.myComp;
         myZoff = frame.myZoff;
         myDoff = frame.myDoff;
         myOoff = frame.myOoff;
         myVersion = frame.myVersion;
         myPrev = prev;
      }

      DataFrame (NumericState state, HasNumericState comp, DataFrame prev) {
         myComp = comp;
         myZoff = state.zsize();
         myDoff = state.dsize();
         myOoff = state.osize();
         myVersion = (comp != null ? comp.getStateVersion() : 0);
         myPrev = prev;
      }

      Offsets getSizes() {
         if (myPrev == null) {
            return new Offsets (myZoff, myDoff, myOoff);
         }
         else {
            return new Offsets (
               myZoff-myPrev.myZoff, myDoff-myPrev.myDoff, 
               myOoff-myPrev.myOoff);
         }
      }

      public Offsets getOffsets() {
         if (myPrev == null) {
            return new Offsets (0, 0, 0);
         }
         else {
            return new Offsets (myPrev.myZoff, myPrev.myDoff, myPrev.myOoff);
         }
      }

      public HasNumericState getComp() {
         return myComp;
      }

      public int getVersion() {
         return myVersion;
      }
   
      public String toString() {
         StringBuilder strb = new StringBuilder();
         strb.append (" hash="+hashCode());
         strb.append (" comp="+getName(myComp));
         strb.append (" zoff="+myZoff);
         strb.append (" doff="+myDoff);
         strb.append (" ooff="+myOoff);
         strb.append (" version="+myVersion);
         strb.append (" prev="+(myPrev != null ? myPrev.hashCode() : "null"));
         return strb.toString();
      }

   }

   ArrayList<DataFrame> myFrames = null;
   protected boolean myAnnotatedP = false;
   
   public NumericState() {
      this (0, 0, 0);
   }

   public NumericState (boolean annotated) {
      this (0, 0, 0);
      setAnnotated (annotated);
   }

   public NumericState (int zcap, int dcap) {
      this (zcap, dcap, 0);
   }

   public NumericState (int zcap, int dcap, int ocap) {
      super (zcap, dcap, ocap);
   }

   public boolean hasDataFrames() {
      return myFrames != null;
   }

   public void setHasDataFrames (boolean hasFrames) {
      if (hasFrames) {
         if (myFrames == null) {
            myFrames = new ArrayList<DataFrame>();
         }
      }
      else {
         if (myFrames != null) {
            myFrames = null;
         }
      }
   }

   public int numDataFrames() {
      if (myFrames == null) {
         return -1;
      }
      else {
         return myFrames.size();
      }
   }   

   public DataFrame getDataFrame (int idx) {
      return myFrames != null ? myFrames.get(idx) : null;
   }

   public void addDataFrame (HasNumericState comp) {
      DataFrame prev = null;
      if (myFrames == null) {
         myFrames = new ArrayList<DataFrame>();
      }
      if (myFrames.size() > 0) {
         prev = myFrames.get(myFrames.size()-1);
      }
      myFrames.add (new DataFrame(this, comp, prev));
   }

   public void getState (HasNumericState comp) {
      comp.getState (this);
      if (hasDataFrames()) {
         addDataFrame (comp);
      }
   }  

   public void getState (DataFrame frame, NumericState state) {
      Offsets sizes = frame.getSizes();
      Offsets offs = frame.getOffsets();
      
      int zi0 = zsize();
      int zi1 = offs.zoff;
      zsetSize (zi0+sizes.zoff);
      for (int i=0; i<sizes.zoff; i++) {
         zbuf[zi0++] = state.zbuf[zi1++];
      }

      int di0 = dsize();
      int di1 = offs.doff;
      dsetSize (di0+sizes.doff);
      for (int i=0; i<sizes.doff; i++) {
         dbuf[di0++] = state.dbuf[di1++];
      }
      if (hasDataFrames()) {
         addDataFrame (frame.myComp);
      }
   }  

   public void set (NumericState state) {
      super.set (state);
      if (state.hasDataFrames()) {
         myFrames = new ArrayList<DataFrame>(state.myFrames.size());
         DataFrame prev = null;
         //System.out.println ("setting "+myFrames.size()+" frames");
         for (DataFrame frame : state.myFrames) {
            DataFrame newFrame = new DataFrame(frame, prev);
            myFrames.add (newFrame);
            prev = newFrame;
         }
      }
//      else {
//         myFrames = null;
//      }
   }

   public void writeBinary (DataOutputStream dos) throws IOException {
      if (myFrames != null) {
         // write out frame data
         // System.out.println (
         //   "writing num frames: " + myFrames.size() + ", state=" + hashCode());
         dos.writeInt (myFrames.size());
         for (DataFrame frame : myFrames) {
            dos.writeInt (frame.myZoff);
            dos.writeInt (frame.myDoff);
         }
      }
      else {
         // write out -1 to indicate no frame data
         dos.writeInt (-1);
      }
      int zsize = zsize();
      int dsize = dsize();
      dos.writeInt (zsize);
      dos.writeInt (dsize);
      int[] zbuf = zbuffer();
      for (int i=0; i<zsize; i++) {
         dos.writeInt (zbuf[i]);
      }
      double[] dbuf = dbuffer();
      for (int i=0; i<dsize; i++) {
         dos.writeDouble (dbuf[i]);
      }  
   }

   public void readBinary (DataInputStream dis) throws IOException {
      int nframes = dis.readInt();
      //System.out.println ("reading num frames: " + nframes);
      if (nframes > 0) {
         if (myFrames != null) {
            if (nframes != myFrames.size()) {
               throw new IOException (
                  "State data incompatible with system structure "+
                  "(incompatible frame count "+nframes+
                  ", expected "+myFrames.size()+")");
            }
            for (int i=0; i<nframes; i++) {
               DataFrame frame = myFrames.get(i);                          
               if (frame.myZoff != dis.readInt() ||
                   frame.myDoff != dis.readInt()) {
                  throw new IOException (
                  "State data incompatible with existing structure "+
                  "(frame "+i+")");
               }
            }
         }
         else {
            // simply read and discard the frame data
            for (int i=0; i<nframes; i++) {
               dis.readInt();
               dis.readInt();
            }
         }
      }
      int zsize = dis.readInt();
      int dsize = dis.readInt();
      if (myFrames != null && myFrames.size() > 0) {
         // make sure zoff and doff of last frame match zsize and dsize
         DataFrame lastf = myFrames.get(myFrames.size()-1);
         if (lastf.myZoff != zsize || lastf.myDoff != dsize) {
            throw new IOException (
               "State data incompatible with existing structure "+
               "(incompatible data sizes)");
         }
      }
      zsetSize(0);
      zEnsureCapacity (zsize);
      for (int i=0; i<zsize; i++) {
         zput (dis.readInt());
      }
      dsetSize(0);
      dEnsureCapacity (dsize);
      for (int i=0; i<dsize; i++) {
         dput (dis.readDouble());
      }
   }

   public boolean isWritable() {
      return true;
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref) 
      throws IOException {
   
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.println ("[");
      
      if (myFrames != null) {
         if (myFrames.size() == 0) {
            pw.println ("frameOffsets=[ ]");
         }
         else {
            pw.print ("frameOffsets=[");
            IndentingPrintWriter.addIndentation (pw, 2);
            for (int i=0; i<myFrames.size(); i++) {
               DataFrame frame = myFrames.get(i);
               if ((i%(intsPerLine/2)) == 0) {
                  pw.println ("");
               }
               pw.print (" " + frame.myZoff);
               pw.print (" " + frame.myDoff);
            }
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");            
         }
      }
      if (zsize() == 0) {
         pw.println ("zbuf=[ ]");
      }
      else {
         int[] zbuf = zbuffer();
         pw.print ("zbuf=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i=0; i<zsize(); i++) {
            if ((i%intsPerLine) == 0) {
               pw.println ("");
            }
            pw.print (" " + zbuf[i]);
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
      //pw.println ("zoff=" + zoffset());

      if (dsize() == 0) {
         pw.println ("dbuf=[ ]");
      }
      else {
         double[] dbuf = dbuffer();
         pw.print ("dbuf=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i=0; i<dsize(); i++) {
            if ((i%doublesPerLine) == 0) {
               pw.println ("");
            }
            pw.print (" " + fmt.format(dbuf[i]));
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
      //pw.println ("doff=" + doffset());
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      setHasDataFrames (false);
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (ScanWriteUtils.scanAttributeName (rtok, "frameOffsets")) {
            rtok.scanToken ('[');
            int nframes = 0;
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               int zoff = rtok.scanInteger();
               int doff = rtok.scanInteger();
               if (myFrames != null && nframes < myFrames.size()) {
                  DataFrame frame = myFrames.get(nframes);
                  if (frame.myZoff != zoff || frame.myDoff != doff) {
                     throw new IOException (
                        "State data incompatible with existing structure "+
                        "(frame "+nframes+")");
                  }                 
               }
               nframes++;
            }          
            if (myFrames != null && nframes != myFrames.size()) {
               throw new IOException (
                  "State data incompatible with system structure "+
                  "(incompatible frame count "+nframes+
                  ", expected "+myFrames.size()+")");
            }
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "zbuf")) {
            rtok.scanToken ('[');
            zsetSize (0);
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               zput (rtok.scanInteger());
            }
            if (myFrames != null &&
                myFrames.get(myFrames.size()-1).myZoff != zsize()) {
               throw new IOException (
                  "State data incompatible with existing structure "+
                  "(incompatible data sizes)");
            } 
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "dbuf")) {
            rtok.scanToken ('[');
            dsetSize (0);
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               dput (rtok.scanNumber());
            }
            if (myFrames != null &&
                myFrames.get(myFrames.size()-1).myDoff != dsize()) {
               throw new IOException (
                  "State data incompatible with existing structure "+
                  "(incompatible data sizes)");
            } 
         }
//         else if (ScanWriteUtils.scanAttributeName (rtok, "zoff")) {
//            zoff = rtok.scanInteger();
//         }
//         else if (ScanWriteUtils.scanAttributeName (rtok, "doff")) {
//            doff = rtok.scanInteger();
//         }
         else {
            throw new IOException ("Unrecognized input: " + rtok);
         }
      }
   }  
   
   public void set (ComponentState state) {
      try {
         set ((NumericState)state);
      }
      catch (ClassCastException e) {
         throw new IllegalArgumentException (
            "state to copy is not a VectorState");
      }
   }

   private static boolean debugEquals = true;

   /** 
    * {@inheritDoc}
    */
   public boolean equals (ComponentState state, StringBuilder msg) {
      if (state instanceof NumericState) {
         return equals ((NumericState)state, msg);
      }
      else {
         if (msg != null) {
            msg.append ("other state is not a NumericState\n");
         }
         return false;
      }
   }

//   public boolean equals (NumericState state) {
//      return super.equals (state, debugEquals);
//   }

   static String getName (HasNumericState comp) {
      if (comp instanceof ModelComponent) {
         return ComponentUtils.getPathName ((ModelComponent)comp);
      }
      else if (comp != null) {
         return comp.toString();
      }
      else {
         return "null";
      }
   }

   private boolean checkFrame (
      int k, NumericState state, StringBuilder errMsg) {

      DataFrame frame0 = (hasDataFrames() ? getDataFrame(k) : null);
      DataFrame frame1 = (state.hasDataFrames() ? state.getDataFrame(k) : null);

      Offsets sizes = null;
      Offsets offs = null;
      HasNumericState comp = null;

      boolean debug = false;

      if (frame0 != null && frame1 != null) {
         // make sure components and sizes are the same for each frame
         if (frame0.myComp != frame1.myComp) {
            if (errMsg != null) {
               errMsg.append (
                  "component "+k+" differs: "+
                  getName(frame0.myComp)+" vs. "+getName(frame1.myComp)+"\n");
            }
            return false;
         }
         comp = frame0.myComp;

         if (!frame0.getSizes().equals (frame1.getSizes())) {
            if (errMsg != null) {
               errMsg.append (
                  "sizes differ for component "+k+", "+getName(comp)+":\n"+
                  frame0.getSizes()+" vs. "+frame1.getSizes()+"\n");
            }
            return false;
         }
         sizes = frame0.getSizes();
         offs = frame0.getOffsets();
      }
      else if (frame0 != null) {
         // make sure state has sufficient elements remaining
         sizes = frame0.getSizes();
         offs = frame0.getOffsets();
         comp = frame0.myComp;
         int num;
         if (sizes.zoff > (num=(state.zsize()-offs.zoff))) {
            if (errMsg != null) {
               errMsg.append (
                  "state1 has "+num+" integers left, needs "+sizes.zoff+"\n");
            }
            return false;
         }
         if (sizes.doff > (num=(state.dsize()-offs.doff))) {
            if (errMsg != null) {
               errMsg.append (
                  "state1 has "+num+" doubles left, needs "+sizes.doff+"\n");
            }
            return false;
         }
      }
      else { 
         // assume frame1 != null; 
         // make sure this state has sufficient elements remaining
         sizes = frame1.getSizes();
         offs = frame1.getOffsets();
         comp = frame1.myComp;
         int num;
         if (sizes.zoff > (num=(zsize()-offs.zoff))) {
            if (errMsg != null) {
               errMsg.append (
                  "state0 has "+num+" integers left, needs "+sizes.zoff+"\n");
            }
            return false;
         }
         if (sizes.doff > (num=(dsize()-offs.doff))) {
            if (errMsg != null) {
               errMsg.append (
                  "state0 has "+num+" doubles left, needs "+sizes.doff+"\n");
            }
            return false;
         }
      }

      for (int i=0; i<sizes.zoff; i++) {
         int zi = offs.zoff+i;
         if (zbuf[zi] != state.zbuf[zi]) {
            if (errMsg != null) {
               errMsg.append (
                  "integer "+i+" differs for component "+k+", "+getName(comp)+
                  ":\n"+zbuf[zi]+" vs. "+state.zbuf[zi]+"\n");
            }
            return false;
         }
      }
      for (int i=0; i<sizes.doff; i++) {
         int di = offs.doff+i;
         if (dbuf[di] != state.dbuf[di]) {
            if (errMsg != null) {
               errMsg.append (
                  "double "+i+" differs for component "+k+", "+getName(comp)+
                  ":\n"+dbuf[di]+" vs. "+state.dbuf[di]+"\n");
            }
            return false;
         }
      }
      return true;
   }

   private boolean checkBeyondLastFrame (
      int numf, NumericState state, StringBuilder errMsg) {

      Offsets offs = Offsets.ZERO;
      if (numf > 0) {
         if (hasDataFrames()) {
            offs = getDataFrame (numf-1).getOffsets();
         }
         else if (state.hasDataFrames()) {
            offs = state.getDataFrame (numf-1).getOffsets();
         }
         else {
            throw new InternalErrorException ("neither state has frames");
         }
      }

      // Note:if both states have frames, it is assumed that lastFrame will
      // have the same demarcations for both, since otherwise one of the
      // previous checkFrame() operations would have failed.

      Offsets sizes0 = getNumericSizes();
      Offsets sizes1 = state.getNumericSizes();

      if (!sizes0.equals (sizes1)) {
         if (errMsg != null) {
            errMsg.append (
               "sizes beyond frames differ: "+sizes0+" vs. "+sizes1+"\n");
         }
         return false;
      }
      Offsets sizes = new Offsets();
      sizes.sub (sizes0, offs);
      if (sizes.equals (Offsets.ZERO)) {
         // no data beyond frames
         return true;
      }
      for (int i=0; i<sizes.zoff; i++) {
         int zi = offs.zoff+i;
         if (zbuf[zi] != state.zbuf[zi]) {
            if (errMsg != null) {
               errMsg.append (
                  "integer "+i+" beyond frames differs:\n" + 
                  zbuf[zi]+" vs. "+state.zbuf[zi]+"\n");
            }
            return false;
         }
      }
      for (int i=0; i<sizes.doff; i++) {
         int di = offs.doff+i;
         if (dbuf[di] != state.dbuf[di]) {
            if (errMsg != null) {
               errMsg.append (
                  "double "+i+" beyond frames differs:\n" + 
                  dbuf[di]+" vs. "+state.dbuf[di]+"\n");
            }
            return false;
         }
      }
      return true;
   }

   public boolean equals (NumericState state, StringBuilder errMsg) {
      if (hasDataFrames() && state.hasDataFrames()) {
         if (numDataFrames() != state.numDataFrames()) {
            if (errMsg != null) {
               errMsg.append (
                  "state0 has "+numDataFrames()+
                  " frames, state1 has "+state.numDataFrames()+"\n");
            }
            return false;
         }
         for (int k=0; k<numDataFrames(); k++) {
            if (!checkFrame (k, state, errMsg)) {
               return false;
            }
         }
         if (!checkBeyondLastFrame (numDataFrames(), state, errMsg)) {
            return false;
         }
      }
      else if (hasDataFrames()) {
         for (int k=0; k<numDataFrames(); k++) {
            if (!checkFrame (k, state, errMsg)) {
               return false;
            }
         }
         if (!checkBeyondLastFrame (numDataFrames(), state, errMsg)) {
            return false;
         }
      }
      else if (state.hasDataFrames()) {
         for (int k=0; k<state.numDataFrames(); k++) {
            if (!checkFrame (k, state, errMsg)) {
               return false;
            }
         }
         if (!checkBeyondLastFrame (state.numDataFrames(), state, errMsg)) {
            return false;
         }
      }
      // check the state sizes, to cover cases where one or both states do not
      // have frames.
      if (zsize() != state.zsize()) {
         if (errMsg != null) {
            errMsg.append ("zsize differs: "+zsize()+" vs. "+state.zsize()+"\n");
         }
         return false;
      }
      if (dsize() != state.dsize()) {
         if (errMsg != null) {
            errMsg.append ("dsize differs: "+dsize()+" vs. "+state.dsize()+"\n");
         }
         return false;
      }
      if (!hasDataFrames() && !state.hasDataFrames()) {
         for (int i=0; i<zsize(); i++) {
            if (zbuf[i] != state.zbuf[i]) {
               if (errMsg != null) {
                  errMsg.append (
                     "integer "+i+" differs: "+
                     zbuf[i]+" vs. "+state.zbuf[i]+"\n");
               }
               return false;
            }
         }
         for (int i=0; i<dsize(); i++) {
            if (dbuf[i] != state.dbuf[i]) {
               if (errMsg != null) {
                  errMsg.append (
                     "double "+i+" differs: "+
                     dbuf[i]+" vs. "+state.dbuf[i]+"\n");
               }
               return false;
            }
         }
      }
      return true;
   }

   public void clear () {
      super.clear();
      if (myFrames != null) {
         myFrames.clear();
      }
   }

   // public NumericState updateFramedState (NumericState state) {
   //    if (!state.hasFrames()) {
   //       throw new IllegalArgumentException (
   //          "state should have frames");
   //    }
   //    clear();
   //    HashMap<HasNumericState> compMap = new HashMap<HasNumericState>();
   //    for (Frame frame : state.myFrames) {
   //       compMap.put (frame.myComp, frame);
   //    }
   // }
      
   public ComponentState duplicate() {
      NumericState state = new NumericState ();
      state.set (this);
      return state;
   }

   public String toString () {
      return ("NumericState " + hashCode() +
              " zsize=" + zsize + 
              " zoff=" + zoff + 
              " dsize=" + dsize + 
              " doff=" + doff +
              " osize=" + osize + 
              " ooff=" + ooff +
              " numFrames=" + numDataFrames());      
   }
   
   public int byteSize() {
      return 8*dsize() + 4*zsize();
   }

   /**
    * {@inheritDoc}
    * 
    * For NumericState, annotation is equivalent to {@code hasFrames() == true}.
    */
   public boolean isAnnotated () {
      return hasDataFrames();
   }  
   
   /**
    * Requests that this state be annotated. See {@link #isAnnotated}
    * for more details.
    * 
    * <p>Annotation is supported by frames. If {@code annotation} is
    * {@code true}, frames are enabled. Otherwise, this method has
    * no effect.
    * 
    * @param annotated if {@code true}, enables frame for this state.
    */
   public void setAnnotated (boolean annotated) {
      if (annotated && !hasDataFrames()) {
         setHasDataFrames (true);
      }
   }  

   public void print() {
      System.out.println (this);
      for (int i=0; i<numDataFrames(); i++) {
         System.out.println ("  "+i+" "+getDataFrame(i));
      }
   }

}
