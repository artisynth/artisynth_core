/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScanToken;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.MassDistribution;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolylineMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.spatialmotion.SpatialInertia;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class RigidMeshComp extends DynamicMeshComponent 
   implements PointAttachable, HasSurfaceMesh {

   private static double DEFAULT_VOLUME = 0;
   protected double myVolume = 0;
   protected boolean myVolumeExplicit = false;

   protected double myMeshVolume = 0;

   private static double DEFAULT_DENSITY = 0;
   protected double myDensity = 0;
   protected boolean myDensityExplicit = false;
   
   private static double DEFAULT_MASS = 0;
   protected double myMass = 0;
   protected boolean myMassExplicit = false;

   public static boolean DEFAULT_HAS_MASS = true;
   protected boolean myHasMassP = DEFAULT_HAS_MASS;

   protected static final boolean DEFAULT_IS_COLLIDABLE = true;
   protected boolean myCollidableP = DEFAULT_IS_COLLIDABLE;

   protected static final MassDistribution DEFAULT_MASS_DISTRIBUTION =
      MassDistribution.DEFAULT;
   protected MassDistribution myMassDistribution = DEFAULT_MASS_DISTRIBUTION;

   public static PropertyList myProps = new PropertyList(
      RigidMeshComp.class, MeshComponent.class);

   static {
      myProps.add(
         "hasMass hasMass setHasMass", 
         "controls whether the mesh contributes to inertia", 
         DEFAULT_HAS_MASS);
      myProps.add(
         "massDistribution", 
         "controls how inertia is computed from the geometry", 
         DEFAULT_MASS_DISTRIBUTION);
      myProps.add (
         "volume", "volume of the mesh", DEFAULT_VOLUME, "NW");
      myProps.add (
         "mass", "mass of the mesh", DEFAULT_MASS, "NW");
      myProps.add (
         "density", "density of the mesh", DEFAULT_DENSITY, "NW");
      myProps.add (
         "isCollidable isCollidable setIsCollidable", 
         "controls whether or not the mesh is collidable", 
         DEFAULT_IS_COLLIDABLE);
   }
   
   public RigidMeshComp() {
      super();
      myHasMassP = DEFAULT_HAS_MASS;
   }
   
   public RigidMeshComp(String name) {
      this();
      setName(name);
   }
   
   public RigidMeshComp (
      MeshBase mesh, String fileName, AffineTransform3dBase X) {
      this();
      setMesh (mesh, fileName, X);
   }

   public RigidMeshComp (MeshBase mesh) {
      this (mesh, null, null);
   }

   @Override
   public void setMesh (
      MeshBase mesh, String fileName, AffineTransform3dBase X) {
      super.setMesh (mesh, fileName, X);
      if (mesh instanceof PolygonalMesh) {
         myCollidableP = true;
      } else {
         myCollidableP = false;
      }
   }

   private void updateMeshVolume() {
      MeshBase mesh = getMesh();
      if (mesh != null && mesh instanceof PolygonalMesh) {
         myMeshVolume = ((PolygonalMesh)mesh).computeVolume();
      }
      else {
         myMeshVolume = 0;
      }
      if (!myVolumeExplicit) {
         myVolume = Math.max (0, myMeshVolume);
      }
   }

   @Override
   protected void setMeshFromInfo () {
      super.setMeshFromInfo();
      updateMeshVolume();
      MeshBase mesh = getMesh();
      if (myMassDistribution == MassDistribution.DEFAULT ||
          (mesh != null && !mesh.supportsMassDistribution (myMassDistribution))) {
         myMassDistribution = getDefaultMassDistribution (mesh);
      }
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Queries whether or not this RigidMeshComp has mass.  See {@link
    * #setHasMass} for a description of what this means.
    * 
    * @return {@code true} if this RigidMeshComp has mass
    */
   public boolean hasMass() {
      return myHasMassP;
   }

   /**
    * Sets whether or not this RigidMeshComp has mass. If it does,
    * then it contributes to the computation of the inertia of the associated
    * rigid body when the <i>inertiaMode</i>} for that body is either {@link
    * RigidBody.InertiaMethod#DENSITY} or {@link RigidBody.InertiaMethod#MASS}.

    * @param enable if {@code true}, sets this RigidMeshComp to have mass
    */
   public void setHasMass (boolean enable) {
      if (myHasMassP != enable) {
         myHasMassP = enable;
         updateBodyForMeshChanges();
      }
   }

   /**
    * Queries the mass distribution for this RigidMeshComp.  See {@link
    * #getMassDistribution} for a description of what this means.
    * 
    * @return mass distribution for this RigidMeshComp
    */
   public MassDistribution getMassDistribution() {
      return myMassDistribution;
   }

   /**
    * Sets the mass distribution for this RigidMeshComp. This controls
    * how inertia is computed from the mesh's geometric features.
    * TODO FINISH
    * then it contributes to the computation of the inertia of the associated
    * rigid body when the <i>inertiaMode</i>} for that body is either {@link
    * RigidBody.InertiaMethod#DENSITY} or {@link RigidBody.InertiaMethod#MASS}.

    * @param dist new mass distribution for this RigidMeshComp
    */
   public void setMassDistribution (MassDistribution dist) {
      MeshBase mesh = getMesh();
      if (mesh == null) {
         myMassDistribution = dist;
      }
      else {
         if (dist == MassDistribution.DEFAULT) {
            dist = getDefaultMassDistribution (mesh);
         }
         else if (!mesh.supportsMassDistribution (dist)) {
            return;
         }
         if (myMassDistribution != dist) {
            myMassDistribution = dist;
            updateBodyForMeshChanges();
         }
      }
   }

   protected SpatialInertia createInertia () {
      SpatialInertia M = new SpatialInertia();
      MeshBase mesh = getMesh();
      if (mesh != null) {
         return mesh.createInertia (getMass(), myMassDistribution);
      }
      return M;
   }

   public MassDistribution getDefaultMassDistribution (MeshBase mesh) {
      if (mesh == null) {
         return DEFAULT_MASS_DISTRIBUTION;
      }
      else if (mesh instanceof PolygonalMesh) {
         if (((PolygonalMesh)mesh).isClosed()) {
            return MassDistribution.VOLUME;
         }
         else {
            return MassDistribution.AREA;
         }
      }
      else if (mesh instanceof PolylineMesh) {
         return MassDistribution.LENGTH;
      }
      else {
         return MassDistribution.POINT;
      }
   }

   /**
    * Returns the volume of this RigidMeshComp. If the mesh is a 
    * {@link PolygonalMesh}, this is the value returned by 
    * {@link PolygonalMesh#computeVolume()}. Otherwise, the volume is 0, unless 
    * {@link #setVolume} is used to explicitly set a non-zero volume value.
    * 
    * @return volume of this RigidMeshComp
    * @see #setVolume 
    */
   public double getVolume() {
      return myVolume;
   }
   
   /**
    * Explicitly sets a volume value for this RigidMeshComp if
    * the {@code vol >= 0} and the mesh is not a {@link PolygonalMesh}.
    * Otherwise, causes the value returned by 
    * {@code #getVolume} to return to its default value.
    * 
    * @param vol explicit volume value if {@code >= 0}
    * @see #getVolume
    */
   public void setVolume (double vol) {
      if (vol < 0) {
         myVolumeExplicit = false;
         myVolume = Math.max (0, myMeshVolume);
      }
      else {
         myVolumeExplicit = true;
         myVolume = vol;
      }
      updateBodyForVolumeChanges();
   }
   
   /**
    * Queries if a volume has been explicitly set by {@link #setVolume}.
    * 
    * @return {@code true} if a volume has been explicitly set
    * @see #setVolume
    */
   public boolean hasExplicitVolume() {
      return myVolumeExplicit;
   }

   private double getRigidBodyDensity() {
      RigidBody body = getRigidBody();
      if (body != null) {
         return body.getDensity();
      }
      else {
         return 0;
      }     
   }

   /**
    * Returns the mass of this RigidMeshComp. By default, this is the volume
    * (as returned by {@link #getVolume}) times the density (as returned by
    * {@link #getDensity}). Otherwise, if a mass value has been explicitly set
    * using {@link #setMass}, then this explicit value is returned.
    * 
    * @return mass of this RigidMeshComp
    * @see #setMass 
    */
   public double getMass() {
      if (myMassExplicit) {
         return myMass;
      }
      else {
         return getDensity()*getVolume();
      }
   }

   /**
    * Explicitly sets a mass value for this RigidMeshComp if {@code mass >= 0}.
    * Otherwise, if {@code mass < 0}, causes the value returned by 
    * {@code #getMass} to return to its default value. In both cases,
    * any explicit density set by {@link #setDensity} is unset.
    * 
    * @param mass explicit mass value if {@code >= 0}.
    * @see #getMass
    */
   public void setMass (double mass) {
      if (mass < 0) {
         myMassExplicit = false;
      }
      else {
         myMass = mass;
         myMassExplicit = true;
      }
      myDensityExplicit = false;
      updateBodyForMeshChanges();
   }
   
   /**
    * Queries if a mass has been explicitly set by {@link #setMass}.
    * 
    * @return {@code true} if a mass has been explicitly set
    * @see #setMass
    */
   public boolean hasExplicitMass() {
      return myMassExplicit;
   }
   
   /**
    * Returns the density of this RigidMeshComp. By default, this is the
    * density of the associated RigidBody (or 0 if there is no such body).
    * Otherwise, if the density has been explicitly set (using {@link
    * #setDensity}), the density is this explicit value, or if the mass has
    * been explicitly set (using {@link #setMass}), the density is the mass
    * divided by the volume (as returned by {@link #getVolume}).
    * 
    * @return density of this RigidMeshComp
    * @see #setDensity
    */
   public double getDensity() {
      if (myDensityExplicit) {
         return myDensity;
      }
      else if (myMassExplicit) {
         double vol = getVolume();
         return vol > 0 ? myMass/vol : 0;
      }
      else {
         return getRigidBodyDensity();
      }
   }
   
  /**
    * Explicitly sets a density value for this RigidMeshComp if {@code d >= 0}.
    * Otherwise, if {@code d < 0}, unsets any explicitly set density.  In both
    * cases, any explicit mass set by {@link #setMass} is also unset.
    * 
    * @param d explicit density value if {@code >= 0}.
    * @see #getDensity
    */
   public void setDensity (double d) {
      if (d < 0) {
         myDensityExplicit = false;
      }
      else {
         myDensity = d;
         myDensityExplicit = true;
      }
      myMassExplicit = false;
      updateBodyForMeshChanges();
   }

   /**
    * Queries if a density has been explicitly set by {@link #setDensity}.
    * 
    * @return {@code true} if a density has been explicitly set
    * @see #setDensity
    */
   public boolean hasExplicitDensity() {
      return myDensityExplicit;
   }
   
   /**
    * Queries if either the mass or density has been explicitly set 
    * by {@link #setMass} or {@link #setDensity}.
    * 
    * @return {@code true} if mass or density has been explicitly set
    * @see #setMass
    */
   public boolean hasExplicitMassOrDensity() {
      return myMassExplicit || myDensityExplicit;
   }
   
   

   @Override
   public void render (Renderer renderer, RenderProps props, int flags) {

      if (renderer.isSelecting()) {
         renderer.beginSelectionQuery (0);
      }
      super.render (renderer, props, flags);
      if (renderer.isSelecting()) {
         renderer.endSelectionQuery ();
      }
   }
   
   public RigidBody getRigidBody() {
      CompositeComponent gp = getGrandParent();
      if (gp instanceof RigidBody) {
         return (RigidBody)gp;
      }
      return null;
   }

   protected void updateBodyForVolumeChanges () {
      RigidBody body = getRigidBody();
      if (body != null) {
         body.updateInertiaForVolumeChanges ();
      }
   }
   
   protected void updateBodyForMeshChanges () {
      RigidBody body = getRigidBody();
      if (body != null) {
         body.updateInertiaForMeshChanges (this);
      }
   }
   
   @Override
   public RigidMeshComp copy(int flags,
      Map<ModelComponent,ModelComponent> copyMap) {

      RigidMeshComp rmc = (RigidMeshComp)super.copy(flags, copyMap);
      rmc.myHasMassP = myHasMassP;
      rmc.myCollidableP = myCollidableP;
      
      return rmc;
   }
   
   @Override
   public int numSelectionQueriesNeeded() {
      return 1;   // trigger so we can add a rigid body
   }
   
   @Override
   public void getSelection(LinkedList<Object> list, int qid) {
      CompositeComponent gp = getGrandParent();
      if (gp instanceof RigidBody) {
         list.addLast(getGrandParent());
      }
      list.addLast(this);
   }

   /**
    * Queries whether or not this RigidMeshComp is collidable.  See {@link
    * #setIsCollidable} for a description of what this means.
    * 
    * @return {@code true} if this RigidMeshComp is collidable
    */
   public boolean isCollidable() {
      return myCollidableP;
   }
   
   /**
    * Sets whether or not this RigidMeshComp is collidable. If it is
    * collidable, then it contributes to the collision mesh used for both
    * collision detection and spring/muscle wrapping.
    * @param enable if {@code true}, makes this RigidMeshComp collidable.
    */
   public void setIsCollidable (boolean enable) {
      if (!(getMesh() instanceof PolygonalMesh)) {
         // can't collide unless we have a PolygonalMesh
         enable = false;
      }
      if (myCollidableP != enable) {
         myCollidableP = enable;
         // send a structure change event instead of a property change
         // event since the collision mesh(s) will be changed, which
         // will in turn invalidate collision state
         notifyParentOfChange (new StructureChangeEvent (this));
      }
   }

   // XXX
   public boolean isDeformable () {
      RigidBody rb = getRigidBody();
      if (rb == null) {
         throw new IllegalStateException (
            "RigidMeshComp not associated with a rigid body");
      }
      return rb.getVelStateSize() > 6;
   }
   
//   public void createVertexMasters (
//      List<ContactMaster> mlist, Vertex3d vtx, ContactPoint cpnt) {
//      RigidBody rb = getRigidBody();
//      if (rb == null) {
//         throw new IllegalStateException (
//            "RigidMeshComp not associated with a rigid body");
//      }
//      mlist.add (new CompContactMaster (rb, 1));
//   }
//   
   @Override
   public PointAttachment getVertexAttachment(int vidx) {
      return getVertexAttachment(getVertex(vidx));
   }
   
   public PointAttachment getVertexAttachment(Vertex3d vtx) {
      if (getGrandParent() instanceof RigidBody) {
         RigidBody rb = (RigidBody)getGrandParent();
         return new PointFrameAttachment (rb, null, vtx.getPosition ());
      }
      return null;
   }
   
   public boolean containsContactMaster (CollidableDynamicComponent comp) {
      return comp == getRigidBody();    
   }   

   public PointFrameAttachment createPointAttachment (Point pnt) {
      
      if (getGrandParent() instanceof RigidBody) {
         RigidBody rb = (RigidBody)getGrandParent();
         return rb.createPointAttachment (pnt);
      }
      else {
         return null;
      }
   }

   public void transformMesh (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, /*flags=*/0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      RigidBody body = getRigidBody();  

      if (body == null) {
         myMeshInfo.transformGeometry (gtr);
         updateMeshVolume();
      }
      else if ((flags & TransformableGeometry.TG_SIMULATING) == 0) {
         GeometryTransformer.Constrainer constrainer = null;
         if (body.getSurfaceMeshComp() == this) {
            // only apply constrainer to the primary surface mesh
            constrainer = body.myTransformConstrainer;
         }
         if (context.contains (body)) {
            myMeshInfo.transformGeometryAndPose (gtr, constrainer);
         }
         else {
            myMeshInfo.transformGeometry (gtr, constrainer);
         }
         updateMeshVolume();
         if (body != null && !context.contains (body)) {
            context.addAction (new RigidBody.UpdateInertiaAction(body));
         }
      }
      else {
         MeshBase mesh = myMeshInfo.getMesh();
         if (context.contains (body)) {
            mesh.setMeshToWorld (body.getPose());
         }
         else {
            // do nothing - shouldn't transform
         }
      }
   }   

   public void connectToHierarchy(CompositeComponent hcomp) {
      // no need to do anything here at the moment. RigidBody will notice
      // addition of mesh in its componentChanged() method
      super.connectToHierarchy(hcomp);
   }

   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      // no need to do anything here at the moment. RigidBody will notice
      // removal of mesh in its componentChanged() method
      super.disconnectFromHierarchy(hcomp);
   }

   public void scaleMass (double s) {
      super.scaleMass (s);
      if (myMassExplicit) {
         myMass *= s;
      }
      else if (myDensityExplicit) {
         myDensity *= s;
      }
      // XXX For now, don't update rigidBody inertia since we assume
      // this is done elsewhere
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      if (myVolumeExplicit) {
         myVolume *= (s*s*s);
      }
      updateMeshVolume();
      if (myDensityExplicit) {
         myDensity /= (s*s*s);
      }
      // XXX For now, don't update rigidBody inertia since we assume
      // this is done elsewhere      
   }

   public void scaleMesh (double sx, double sy, double sz) {
      myMeshInfo.scale (sx, sy, sz);
      if (myVolumeExplicit) {
         myVolume *= (sx*sy*sz);
      }
      updateMeshVolume();
      updateBodyForVolumeChanges();
   }  

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "mass")) {
         double mass = rtok.scanNumber();
         setMass (mass);
         return true;
      }
      else if (scanAttributeName (rtok, "volume")) {
         double volume = rtok.scanNumber();
         setVolume (volume);
         return true;
      }
      else if (scanAttributeName (rtok, "density")) {
         double density = rtok.scanNumber();
         setDensity (density);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }     

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      if (myMassExplicit) {
         pw.println ("mass=" + fmt.format (getMass()));
      }
      else if (myDensityExplicit) {
         pw.println ("density=" + fmt.format (getDensity()));
      }
      if (myVolumeExplicit) {
         pw.println ("volume=" + fmt.format (getVolume()));
      }
      super.writeItems (pw, fmt, ancestor);
   }

   public PolygonalMesh getSurfaceMesh() {
      if (getMesh() instanceof PolygonalMesh) {
         return (PolygonalMesh)getMesh();
      }
      else {
         return null;
      }
   }
   
   public int numSurfaceMeshes() {
      return getSurfaceMesh() != null ? 1 : 0;
   }
   
   public PolygonalMesh[] getSurfaceMeshes() {
      return MeshComponent.createSurfaceMeshArray (getSurfaceMesh());
   }

}
