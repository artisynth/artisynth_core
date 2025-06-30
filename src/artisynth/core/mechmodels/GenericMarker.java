package artisynth.core.mechmodels;

import java.util.List;
import java.util.Map;
import java.util.Deque;
import java.io.PrintWriter;
import java.io.IOException;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils.ClassInfo;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.Point3d;
import maspack.matrix.SparseBlockMatrix;
import maspack.properties.PropertyList;
import maspack.util.ReaderTokenizer;
import maspack.util.NumberFormat;

public class GenericMarker extends Marker {

   protected PointAttachment myPointAttachment;
   
   public static PropertyList myProps =
      new PropertyList (GenericMarker.class, Point.class);
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * No-args constructor needed for scan/write.
    */
   public GenericMarker() {
   }

   public GenericMarker(Point3d pnt) {
      this.setPosition (pnt);
      myPointAttachment = null;
   }

   public GenericMarker(PointAttachment attach) {
      super();
      myPointAttachment = attach;
      setAttached (myPointAttachment);
   }
   
   @Override
   public void setAttached (DynamicAttachment attachment) {
      if (!(attachment instanceof PointAttachment)) {
         throw new IllegalArgumentException ("Attachment must be of type PointAttachment");
      }
      myPointAttachment = (PointAttachment)attachment;
      super.setAttached (attachment);
   }
   
   @Override
   public PointAttachment getAttachment () {
      return myPointAttachment;
   }
   
// John Lloyd, Dec 2020: markers now have state, since state is
// now saved and restored for attached components (since Nov 26)  
//   public boolean hasState() {
//      return myPointAttachment.hasState ();
//   }
   
   /** 
    * {@inheritDoc}
    */
  public int addTargetJacobian (SparseBlockMatrix J, int bi) {
      if (!isControllable()) {
         throw new IllegalStateException (
            "Target marker is not controllable");
      }
      return myPointAttachment.addTargetJacobian (J, bi);
   }
   
   @Override
   public void getAttachments (List<DynamicAttachment> list) {
      list.add (getAttachment());
   }

   @Override
   public void updateState () {
      myPointAttachment.updatePosStates();
      myPointAttachment.updateVelStates();
   }
   
   public void updatePosState() {
      myPointAttachment.updatePosStates();
   }

   public void updateVelState() {
      myPointAttachment.updateVelStates();
   }
   
   public void applyForces() {
      myPointAttachment.applyForces();
   }
   
   public void updateAttachment() {
      myPointAttachment.updateAttachment();
   }
   
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      super.transformGeometry (gtr, context, flags);
      // John Lloyd, Oct 2015: don't think we need this
      //updateAttachment ();
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "attachment")) {
         ClassInfo<?> classInfo =
            ScanWriteUtils.scanClassInfo (rtok, PointAttachment.class);
         myPointAttachment = (PointAttachment)ScanWriteUtils.newComponent (
            rtok, classInfo, /*warnOnly=*/false);      
         tokens.offer (new StringToken ("attachment", rtok.lineno()));
         myPointAttachment.scan (rtok, tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "attachment")) {
         myPointAttachment.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myPointAttachment != null) {
         pw.print ("attachment="+ScanWriteUtils.getClassTag(myPointAttachment));
         myPointAttachment.write (pw, fmt, ancestor);
      }
   }


//   @Override
//   public void connectToHierarchy (CompositeComponent hcomp) {
//      super.connectToHierarchy (hcomp);
//      if (hcomp == getParent()) {
//         DynamicComponent masters[] = myPointAttachment.getMasters();
//         if (masters != null) {
//            for (DynamicComponent master : masters) {
//               master.addMasterAttachment (myPointAttachment);
//            }
//         }
//      }
//   }
//   
//   @Override
//   public void disconnectFromHierarchy (CompositeComponent hcomp) {
//      super.disconnectFromHierarchy(hcomp);
//      System.out.println (
//         "disconnecting " + getName() + " from " + hcomp.getName());
//      if (hcomp == getParent()) {
//         DynamicComponent masters[] = myPointAttachment.getMasters();
//         if (masters != null) {
//            for (DynamicComponent master : masters) {
//               master.removeMasterAttachment (myPointAttachment);
//            }
//         }
//      }
//   }

   /**
    * {@inheritDoc}
    */
   public boolean isDuplicatable() {
      return true;
   }

   /**
    * {@inheritDoc}
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      return true;
   }

   @Override
   public GenericMarker copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      GenericMarker m = (GenericMarker)super.copy (flags, copyMap);
      
      // bit of a hack: enter the new marker in the copyMap so that
      // PointFem3dAttachment.copy() will be able to find it
      if (copyMap != null) {
         copyMap.put (this, m);       // EDIT: if copyMap is null, this throws an error!  Sanchez, Nov 30,2011
      }
      m.myPointAttachment = myPointAttachment.copy (flags, copyMap);
         // (PointFem3dAttachment)ComponentUtils.maybeGetCopy (
         //    flags, copyMap, myNodeAttachment);
      m.setAttached (m.myPointAttachment);        

      return m;
   }

}
