package artisynth.core.mechmodels;

import java.util.List;
import java.util.Map;

import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.SparseBlockMatrix;
import maspack.properties.PropertyList;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.TransformableGeometry;

public class GenericMarker extends Marker {

   protected PointAttachment myAttachment;
   
   public static PropertyList myProps =
      new PropertyList (GenericMarker.class, Point.class);
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public GenericMarker(PointAttachment attach) {
      super();
      myAttachment = attach;
      setAttached (myAttachment);
   }
   
   public boolean hasState() {
      return myAttachment.hasState ();
   }
   
   /** 
    * {@inheritDoc}
    */
  public int addTargetJacobian (SparseBlockMatrix J, int bi) {
      if (!isControllable()) {
         throw new IllegalStateException (
            "Target marker is not controllable");
      }
      return myAttachment.addTargetJacobian (J, bi);
   }
   
   @Override
   public void getAttachments (List<DynamicAttachment> list) {
      list.add (getAttachment());
   }

   @Override
   public void updateState () {
      myAttachment.updatePosStates();
      myAttachment.updateVelStates();
   }
   
   public void updatePosState() {
      myAttachment.updatePosStates();
   }

   public void updateVelState() {
      myAttachment.updateVelStates();
   }
   
   public void applyForces() {
      myAttachment.applyForces();
   }
   
   public void updateAttachment() {
      myAttachment.updateAttachment();
   }
   
   @Override
   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {
      super.transformGeometry (X, topObject, flags);
      updateAttachment ();
   }
   
   @Override
   public void connectToHierarchy () {
      super.connectToHierarchy ();
      DynamicComponent masters[] = myAttachment.getMasters();
      if (masters != null) {
         for (DynamicComponent master : masters) {
            master.addMasterAttachment (myAttachment);
         }
      }
   }
   
   @Override
   public void disconnectFromHierarchy() {
      super.disconnectFromHierarchy();
      DynamicComponent masters[] = myAttachment.getMasters();
      if (masters != null) {
         for (DynamicComponent master : masters) {
            master.removeMasterAttachment (myAttachment);
         }
      }
   }


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
      m.myAttachment = myAttachment.copy (flags, copyMap);
         // (PointFem3dAttachment)ComponentUtils.maybeGetCopy (
         //    flags, copyMap, myNodeAttachment);
      m.setAttached (m.myAttachment);        

      return m;
   }

}
