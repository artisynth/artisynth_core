/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolylineMesh;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.SymmetricMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.HierarchyNode;
import maspack.render.RenderList;
import maspack.spatialmotion.SpatialInertia;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentListImpl;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScanToken;
/**
 * Allows a rigid body to have multiple geometries, some used for
 * computing mass/inertia, some for display only, some for collisions
 * 
 * @author Antonio
 */
public class RigidCompositeBody extends RigidBody implements 
   CompositeComponent {
   
   MeshComponentList<RigidMeshComp> myMeshList = null;
   
   public RigidCompositeBody() {
      this (null);
   }

   public RigidCompositeBody(String name) {
      super(name);
      myComponents = 
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      myMeshList =
         new MeshComponentList<RigidMeshComp>(
            RigidMeshComp.class, "meshes", "msh");
      add(myMeshList);
   }
   
   /**
    * returns the first available PolygonalMesh, for compatibility with RigidBody
    */
   public PolygonalMesh getMesh() {
      PolygonalMesh mesh = null;
      for (RigidMeshComp mc : myMeshList) {
         if (mc.getMesh() instanceof PolygonalMesh) {
            return (PolygonalMesh)mc.getMesh();
         }
      }
      return mesh;
   }
   
   public MeshComponentList<RigidMeshComp> getMeshComps() {
      return myMeshList;
   }
   
   public RigidMeshComp getMeshComp(String name) {
      return myMeshList.get (name);
   }
   
//   public PolygonalMesh getCollisionMesh(int idx) {
//      int i = 0;
//      if (idx < 0) {
//         idx = 0;
//      }
//      for (RigidMeshComp mc : myMeshList) {
//         if (mc.getMesh() instanceof PolygonalMesh) {
//            if (idx == i) {
//               return (PolygonalMesh)mc.getMesh();
//            } else {
//               i++;
//            }
//         }
//      }
//      throw new IllegalArgumentException("Failed to determine the desired collision mesh");
//   }
      
   /**
    * Adds a mesh to this object.  Can be of any type.
    * @param mesh Instance of MeshBase
    * @return a special "mesh component" object that is created internally 
    */
   public RigidMeshComp addMesh(MeshBase mesh) {
      return addMesh(mesh, null, null, RigidMeshComp.DEFAULT_PHYSICAL);
   }
   
   /**
    * Adds a mesh to this object.  Can be of any type.
    * @param mesh Instance of MeshBase
    * @param physical true if to be used for mass/inertia calculations 
    * @return a special "mesh component" object that is created internally 
    */
   public RigidMeshComp addMesh(MeshBase mesh, boolean physical) {
      return addMesh(mesh, null, null, physical);
   }
   
   /**
    * Adds a mesh to this object
    * @param mesh Instance of MeshBase
    * @param fileName name of file (can be null)
    * @param Xh transform associated with mesh
    * @param physical if true, this mesh is used for computing mass and inertia
    * @return a special "mesh component" object that is created internally 
    */
   public RigidMeshComp addMesh (
      MeshBase mesh, String fileName, AffineTransform3dBase Xh, 
      boolean physical) {
      
      RigidMeshComp mc = new RigidMeshComp();
      mc.setMesh(mesh, fileName, Xh);
      if (mesh.getName() != null) {
         mc.setName(mesh.getName());
      }
      mc.setPhysical(physical);
      
      if (mesh.getRenderProps() != null) {
         mc.setRenderProps(mesh.getRenderProps());
      }
      
      addMeshComp(mc);
      
      return mc;
   }
   
   /**
    * Explicitly adds a mesh component.  If the flag mc.isPhysical() is true,
    * then this mesh is used for mass/inertia computations
    */
   public void addMeshComp(RigidMeshComp mc) {
      mc.getMesh().setFixed(true);
      mc.getMesh().setMeshToWorld (myState.XFrameToWorld);
      myMeshList.add(mc);
      
      if (mc.isPhysical()) {
         if (myInertiaMethod == InertiaMethod.Density) {
            setInertiaFromDensity(myDensity);
         } else if (myInertiaMethod == InertiaMethod.Mass) {
            setInertiaFromMass (getMass());            
         }
      }
   }
   
   /**
    * Number of meshes associated with this object
    */
   public int numMeshComps() {
      return myMeshList.size();
   }
   
   /**
    * Checks if this object contains a particular geometry
    */
   public boolean containsMeshComp(RigidMeshComp mc) {
      return myMeshList.contains(mc);
   }
   
   private double getMeshMass(MeshBase base, double density) {
      
      if (base instanceof PolygonalMesh) {
         double vol = ((PolygonalMesh)base).computeVolume();
         return density * vol;
      } else if (base instanceof PointMesh) {
         // return ((PointMesh)base).numVertices() * density;
         // XXX To implement along with spatial inertia
      } else if (base instanceof PolylineMesh) {
         //         PolylineMesh mesh = (PolylineMesh)base;
         //         double len = 0;
         //         for (Polyline line : mesh.getLines()) {
         //            len += line.computeLength();
         //         }
         //         return density*len;
         // XXX To implement along with spatial inertia
      }
      
      return 0;
   }
   
   private double getMeshVolume(MeshBase base) {
      
      if (base instanceof PolygonalMesh) {
         return ((PolygonalMesh)base).computeVolume();
      } else if (base instanceof PointMesh) {
         // XXX To implement along with spatial inertia
      } else if (base instanceof PolylineMesh) {
         // XXX To implement along with spatial inertia
      }
      
      return 0;
   }
   
   protected void addMeshInertia (MeshBase base, double density) {
      if (base == null) {
         throw new IllegalStateException ("Mesh has not been set");
      }
      
      if (base instanceof PolygonalMesh) {
         PolygonalMesh mesh = (PolygonalMesh)base;
         SpatialInertia M = mesh.createInertia (density);
         mySpatialInertia.add (M);
      } else if (base instanceof PointMesh) {
         //XXX to implement
      } else if (base instanceof PolylineMesh) {
         //XXX to implement
      }
      
   }
   
   protected void updateInertia() {
      
      switch (myInertiaMethod) {
         case Density: {
            setInertiaFromDensity(myDensity);
            break;
         }
         case Mass: {
            setInertiaFromMass(getMass());
            break;
         }
         case Explicit: {
            break;
         }
         default: {
            throw new InternalErrorException (
               "Unimplemented inertia method " + myInertiaMethod);
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void setInertiaMethod (InertiaMethod method) {
      
      if (method != myInertiaMethod) {
         switch (method) {
            case Density: {
               setInertiaFromDensity(myDensity);
               break;
            }
            case Mass: {
               setInertiaFromMass(getMass());
               break;
            }
            case Explicit: {
               break;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented inertia method " + method);
            }
         }
         myInertiaMethod = method;
      }
   }

   private void doSetInertia (SpatialInertia M) {
      mySpatialInertia.set (M);
      
      double volume = 0;
      for ( RigidMeshComp mc : myMeshList) {
         MeshBase mesh = mc.getMesh();
         if (mesh != null && mc.isPhysical()) {
            volume += getMeshVolume(mesh);
         }                  
      }
      
      if (volume > 0) {
         myDensity = M.getMass()/volume;
      } else {
         myDensity = 0;
      }
 
      myInertiaMethod = InertiaMethod.Explicit;
   }
   
   /**
    * Explicitly sets the spatial inertia of this body. Also sets the uniform
    * density (as returned by {@link #getDensity getDensity}) to be undefined).
    */
   public void setInertia (SpatialInertia M) {
      doSetInertia (M);
   }

   /**
    * Explicitly sets the mass and rotational inertia of this body. Also sets
    * the uniform density (as returned by {@link #getDensity getDensity}) to be
    * undefined).
    */
   public void setInertia (double m, SymmetricMatrix3d J) {
      SpatialInertia M = new SpatialInertia();
      M.set (m, J);
      doSetInertia (M);
   }

   /**
    * Explicitly sets the mass, rotational inertia, and center of mass of this
    * body. Also sets the uniform density (as returned by {@link #getDensity
    * getDensity}) to be undefined).
    */
   public void setInertia (double m, SymmetricMatrix3d J, Point3d com) {
      SpatialInertia M = new SpatialInertia();
      M.set (m, J, com);
      doSetInertia (M);
   }

   /**
    * Explicitly sets the mass and rotational inertia of this body. Also sets
    * the uniform density (as returned by {@link #getDensity getDensity}) to be
    * undefined).
    */
   public void setInertia (double m, double Jxx, double Jyy, double Jzz) {
      doSetInertia (
         new SpatialInertia (m, Jxx, Jyy, Jzz));
   }

   protected void setInertiaFromMesh (double density) {
   }
   
   /** 
    * Causes the inertia to be automatically computed from the mesh volume
    * and a given density. If the mesh is currently <code>null</code> then
    * the inertia remains unchanged. Subsequent (non-<code>null</code>) changes
    * to the mesh will cause the inertia to be recomputed.
    * The inertia method is set to
    * {@link RigidBody.InertiaMethod#Density Density}. 
    * 
    * @param density desired uniform density
    */
   public void setInertiaFromDensity (double density) {
      if (density < 0) {
         throw new IllegalArgumentException ("density must be non-negative");
      }
      myDensity = density;
      myInertiaMethod = InertiaMethod.Density;
      
      mySpatialInertia.setZero();
      double mass = 0;
      for ( RigidMeshComp mc : myMeshList) {
         MeshBase mesh = mc.getMesh();
         if (mesh != null && mc.isPhysical()) {
            mass += getMeshMass(mesh, myDensity);
            addMeshInertia(mesh, myDensity);
         }                  
      }
      mySpatialInertia.setMass (mass);
      
   }

   /** 
    * Causes the inertia to be automatically computed from the mesh volume
    * and a given mass (with the density computed by dividing the mass
    * by the mesh volume). If the mesh is currently <code>null</code> the mass 
    * of the inertia is updated but the otherwise the inertia and density
    * are left unchanged. Subsequent (non-<code>null</code>) changes
    * to the mesh will cause the inertia to be recomputed.
    * The inertia method is set to {@link RigidBody.InertiaMethod#Mass Mass}. 
    * 
    * @param mass desired body mass
    */
   public void setInertiaFromMass (double mass) {
      if (mass < 0) {
         throw new IllegalArgumentException ("mass must be non-negative");
      }
      double volume = 0;
      for ( RigidMeshComp mc : myMeshList) {
         MeshBase mesh = mc.getMesh();
         if (mesh != null && mc.isPhysical()) {
            volume += getMeshVolume(mesh);
         }                  
      }
      
      if (volume > 0) {
         myDensity = getMass() / volume;
      } else {
         myDensity = 0;
      }
      for ( RigidMeshComp mc : myMeshList) {
         MeshBase mesh = mc.getMesh();
         if (mesh != null && mc.isPhysical()) {
            addMeshInertia(mesh, myDensity);
         }                  
      }
      mySpatialInertia.setMass (mass);
      myInertiaMethod = InertiaMethod.Mass;      
   }
   
   @Override
   public double computeVolume() {
      double vol = 0;
      for ( RigidMeshComp mc : myMeshList) {
         if (mc.isPhysical()) {
            MeshBase mesh = mc.getMesh();
            if (mesh instanceof PolygonalMesh) {
               vol += ((PolygonalMesh)mesh).computeVolume();
            }
         }
      }
      return vol;
   }

   /**
    * Sets the density for the mesh, which is defined at the mass divided
    * by the mesh volume. If the mesh is currently non-null, the mass
    * will be updated accordingly. If the current InertiaMethod
    * is either {@link RigidBody.InertiaMethod#Density Density} or 
    * {@link RigidBody.InertiaMethod#Mass Mass}, the other components of
    * the spatial inertia will also be updated.
    * 
    * @param density
    * new density value
    */
   public void setDensity (double density) {
      if (density < 0) {
         throw new IllegalArgumentException ("density must be non-negative");
      }
      
      double mass = 0;
      for ( RigidMeshComp mc : myMeshList) {
         MeshBase mesh = mc.getMesh();
         if (mesh != null && mc.isPhysical()) {
            mass += getMeshMass(mesh, density);
         }                  
      }
      
      if (myInertiaMethod == InertiaMethod.Mass) {
         setInertiaFromMass(mass);
      } else if (myInertiaMethod == InertiaMethod.Density) {
         setInertiaFromDensity(density);
      }
      
      myDensity = density;
      mySpatialInertia.setMass(mass);
      
   }
   
   /**
    * Sets the mass for the mesh. If the mesh is currently non-null, then the
    * density (defined as the mass divided by the mesh volume) will be updated
    * accordingly. If the current InertiaMethod is either {@link
    * RigidBody.InertiaMethod#Density Density} or
    * {@link RigidBody.InertiaMethod#Mass Mass}, the
    * other components of the spatial inertia will also be updated.
    * 
    * @param mass
    * new mass value
    */
   public void setMass (double mass) {
      if (mass < 0) {
         throw new IllegalArgumentException ("Mass must be non-negative");
      }
      
      double volume = 0;
      for ( RigidMeshComp mc : myMeshList) {
         MeshBase mesh = mc.getMesh();
         if (mesh != null && mc.isPhysical()) {
            volume += getMeshVolume(mesh);
         }                  
      }
      
      if (volume > 0) {
         myDensity = getMass() / volume;
      } else {
         myDensity = 0;
      }
      
      if (myInertiaMethod == InertiaMethod.Mass) {
         setInertiaFromMass(mass);
      } else if (myInertiaMethod == InertiaMethod.Density) {
         setInertiaFromDensity(myDensity);
      }
      mySpatialInertia.setMass (mass);
   }
   
   
   public boolean removeMeshComp(RigidMeshComp mc) {
      return myMeshList.remove(mc);
   }
   
   public RigidMeshComp removeMeshComp(String name) {
      RigidMeshComp mesh = getMeshComp(name);
      if (mesh != null) {
         myMeshList.remove(mesh);
      }
      return null;
   }
   
   /**
    * 
    */
   public void setMesh ( PolygonalMesh mesh, String fileName, AffineTransform3dBase X) {
      RigidMeshComp rmc = new RigidMeshComp (mesh, fileName, X);
      myMeshList.add (rmc, 0);
   }
   
   
   protected void updatePosState() {
      super.updatePosState();
      for (RigidMeshComp mc : myMeshList) {
         MeshBase mesh = mc.getMesh();
         mesh.setMeshToWorld(myState.XFrameToWorld);
      }
   }
   
   public void prerender (RenderList list) {
      super.prerender(list);
      list.addIfVisible(myMeshList);
   }
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      context.addAll (myMeshList);
   }

//   public void transformGeometry (
//      GeometryTransformer X, TransformGeometryContext context, int flags) {
//      
//      RigidTransform3d Xpose = new RigidTransform3d();
//      Xpose.set (myState.XFrameToWorld);
//      X.transform (Xpose);
//      for (RigidMeshComp mc : myMeshList) {
//         if (mc.transformGeometry (X, Xpose)) {
//            mc.getRenderProps().clearMeshDisplayList();
//         }
//      }
//      super.transformGeometry (X, context, flags);
//   }   
   
   public void scaleDistance(double s) {
      super.scaleDistance(s);
      
      for (RigidMeshComp mc : myMeshList) {
         MeshBase mesh = mc.getMesh();
         mesh.scale(s);
         mesh.setMeshToWorld(myState.XFrameToWorld);
      }
      
   }
   
   
   ///////////////////////////////////////////////////
   // Composite component stuff
   ///////////////////////////////////////////////////

   protected ComponentListImpl<ModelComponent> myComponents;
   private NavpanelDisplay myDisplayMode = NavpanelDisplay.NORMAL;

   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      myComponents.updateNameMap (newName, oldName, comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent findComponent (String path) {
      return ComponentUtils.findComponent (this, path);
   }

   protected void add (ModelComponent comp) {
      myComponents.add (comp);
   }
 
   protected boolean remove (ModelComponent comp) {
      return myComponents.remove (comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (String nameOrNumber) {
      return myComponents.get (nameOrNumber);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (int idx) {
      return myComponents.get (idx);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent getByNumber (int num) {
      return myComponents.getByNumber (num);
   }

   /**
    * {@inheritDoc}
    */
   public int getNumberLimit() {
      return myComponents.getNumberLimit();
   }

   // public static int indexOf (ModelComponent comp, ModelComponent[] list)
   // {
   // if (list != null)
   // { for (int i=0; i<list.length; i++)
   // { if (list[i] == comp)
   // { return i;
   // }
   // }
   // }
   // return -1;
   // }

   /**
    * {@inheritDoc}
    */
   public int indexOf (ModelComponent comp) {
      return myComponents.indexOf (comp);
   }

   /**
    * {@inheritDoc}
    */
   public int numComponents() {
      return myComponents.size();
   }

   /**
    * {@inheritDoc}
    */
   public void componentChanged (ComponentChangeEvent e) {
      myComponents.componentChanged (e);
      notifyParentOfChange (e);
   }

   protected void notifyStructureChanged (Object comp) {
      if (comp instanceof CompositeComponent) {
         notifyParentOfChange (new StructureChangeEvent (
            (CompositeComponent)comp));
      }
      else {
         notifyParentOfChange (StructureChangeEvent.defaultEvent);
      }
   }

   
   @Override
   public void scan(ReaderTokenizer rtok, Object ref) throws IOException {
      myComponents.scanBegin();
      super.scan (rtok, ref);
   }
   
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      rtok.nextToken();
      if (ScanWriteUtils.scanProperty (rtok, this)) {
         return true;
      }
      if (myComponents.scanAndStoreComponentByName (rtok, tokens)) {
         return true;
      }
      rtok.pushBack();      
      return super.scanItem (rtok, tokens);
   }


   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) 
      throws IOException {
      
      if (myComponents.postscanComponent (tokens, ancestor)) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   @Override
   public void postscan (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      myComponents.scanEnd();
      updateInertia();  // reset inertia
      updatePosState();
   }
   
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      
      // properties
      super.writeItems(pw, fmt, ancestor);
      // components
      myComponents.writeComponentsByName (pw, fmt, ancestor);
   }      

   public Iterator<? extends HierarchyNode> getChildren() {
      return myComponents.iterator();
   }

   public boolean hasChildren() {
      return myComponents != null && myComponents.size() > 0;
   }

   public void setSelected (boolean selected) {
      super.setSelected (selected);
   }

   /**
    * {@inheritDoc}
    */
   public NavpanelDisplay getNavpanelDisplay() {
      return myDisplayMode;
   }
   
   /**
    * Sets the display mode for this component. This controls
    * how the component is displayed in a navigation panel. The default
    * setting is <code>NORMAL</code>.
    *
    * @param mode new display mode
    */
   public void setDisplayMode (NavpanelDisplay mode) {
      myDisplayMode = mode;
   }

   /**
    * {@inheritDoc}
    */
   public boolean hierarchyContainsReferences() {
      return false;
   }

   public RigidCompositeBody copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      RigidCompositeBody ccomp =
         (RigidCompositeBody)super.copy (flags, copyMap);

      ccomp.myComponents =
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      ccomp.myDisplayMode = myDisplayMode;
      
      return ccomp;
   }

   public boolean hasState() {
      return true;
   }
   
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
    
      for (RigidMeshComp mc : myMeshList) {
         mc.updateBounds(pmin, pmax);
      }
      
   }
   
//   // Pullable interface
//   @Override
//   public Point3d getOriginData (Point3d origin, Vector3d dir) {
//
//      Point3d myBodyPnt = null;
//      Point3d nearest = null;
//      double nearestDistance = Double.POSITIVE_INFINITY;
//
//      for (RigidMeshComp mc : myMeshList) {
//         MeshBase mesh = mc.getMesh();
//         if (mesh != null && mesh instanceof PolygonalMesh) {
//            PolygonalMesh smesh = (PolygonalMesh)mesh;
//            Point3d pnt = BVFeatureQuery.nearestPointAlongRay (
//               smesh, origin, dir);
//            if (pnt != null) {
//               double d = pnt.distance(origin);
//               if (nearest == null || d < nearestDistance) {
//                  nearestDistance = d;
//                  nearest = pnt;
//               }
//            }
//         }
//      }
//
//      if (nearest != null) {
//         myBodyPnt = new Point3d(nearest);
//         myBodyPnt.inverseTransform (getPose());
//      }
//      
//      return myBodyPnt;
//   }
   
   @Override
   public int numSurfaceMeshes() {
      return MeshComponent.numSurfaceMeshes (myMeshList);
   }
   
   @Override
   public PolygonalMesh[] getSurfaceMeshes() {
      return MeshComponent.getSurfaceMeshes (myMeshList);
   }
   
   @Override
   public boolean isCompound() {
      return true;
   }

}
