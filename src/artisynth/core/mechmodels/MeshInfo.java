/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.*;

import artisynth.core.util.*;

import maspack.geometry.*;
import maspack.matrix.*;
import maspack.render.*;
import maspack.util.*;

/**
 * Contains information about a mesh, including the mesh itself, and it's
 * possible file name and transformation with respect to the original file
 * definition.
 */ 
public class MeshInfo {

   MeshBase myMesh;
   String myFileName;
   // meshModifiedP means mesh has been changed such that it can no longer
   // be recovered from the file information.
   boolean myMeshModifiedP;
   AffineTransform3d myFileTransform;
   boolean myFileTransformRigidP = true;
   boolean myFlippedP = false;

   public String getFileName() {
      return myFileName;
   }

   public void setFileName (String filename) {
      myFileName = filename;
      myMeshModifiedP = false;
   }

   public MeshInfo() {
      myMesh = null;
      myFileName = null;
      myMeshModifiedP = false;
      myFileTransform = new AffineTransform3d();
      myFileTransformRigidP = true;
      myFlippedP = false;
   }       

   public MeshBase getMesh() {
      return myMesh;
   }
   
   /**
    * Returns the file transform associated with the mesh.
    * 
    * @return mesh file transform (should not be modified)
    * @see #setFileTransform
    */
   public AffineTransform3dBase getFileTransform() {
      if (myFileTransformRigidP) {
         RigidTransform3d T = new RigidTransform3d();
         T.set (myFileTransform);
         return T;
      }
      else {
         return myFileTransform.copy();
      }
   }

   /**
    * Sets the transform used to modify a mesh originally read from a file. It
    * is only meaningful if there is a also mesh file name.
    * 
    * @param X
    * new mesh file transform, or <code>null</code>
    */
   protected void setFileTransform (AffineTransform3dBase X) {
      if (X != null) {
         myFileTransform.set (X);
         myFileTransformRigidP = (X instanceof RigidTransform3d);
      }
      else {
         myFileTransform.setIdentity();
         myFileTransformRigidP = true;
      }
   }

   public void set (MeshBase mesh, String fileName, AffineTransform3dBase X) {

      if (myMesh != null) {
         myMesh.setRenderBuffered (false);
      }
      myMesh = mesh;
      if (myMesh != null) {
         //myMesh.setMeshToWorld (myState.XFrameToWorld);
         myMesh.setRenderBuffered (true);
         setFileName (fileName);
         setFileTransform (X);
      }
      else {
         setFileName (null);
         setFileTransform (null);
      }
      myFlippedP = false; // assume mesh is in correct orientation
   }   

   public void set (MeshBase mesh) {
      set (mesh, null, null);
   }

   public int numVertices() {
      if (myMesh != null) {
         return myMesh.numVertices();
      }
      else {
         return 0;
      }
   }        

   protected void preMultiplyFileTransform (AffineTransform3dBase X) {
      myFileTransform.mul (X, myFileTransform);
      if (X instanceof AffineTransform3d) {
         myFileTransformRigidP = false;
      }
   }

   public void scale (double sx, double sy, double sz) {
      if (sx != 1 || sy != 1 || sz != 1) {
         myMesh.scale (sx, sy, sz);
         AffineTransform3d S = AffineTransform3d.createScaling (sx, sy, sz);
         preMultiplyFileTransform (S);
         if (sx*sy*sz < 0 && myMesh instanceof PolygonalMesh) {
            myFlippedP = !myFlippedP;
            ((PolygonalMesh)myMesh).flip();
         }
      }
   }

   protected void saveOrRestoreModBitsIfNecessary (
      GeometryTransformer gtr, boolean oldRigidP, boolean oldModifiedP) {
      if (gtr.isSaving()) {
         gtr.saveObject (oldRigidP);
         gtr.saveObject (oldModifiedP);
      }
      else if (gtr.isRestoring()) {
         myFileTransformRigidP = gtr.restoreObject (myFileTransformRigidP);
         myMeshModifiedP = gtr.restoreObject (myMeshModifiedP);
      }
   }
   
   public void transformGeometryOld (GeometryTransformer gtr) {
      gtr.transform (myMesh);
      
      boolean oldRigidP = myFileTransformRigidP;
      boolean oldModifiedP = myMeshModifiedP;
      
      if (gtr.isAffine()) {
         gtr.transform (myFileTransform);
         if (!gtr.isRigid()) {
            myFileTransformRigidP = false;
         }
         if (gtr.isReflecting() && myMesh instanceof PolygonalMesh) {
            myFlippedP = !myFlippedP;
            ((PolygonalMesh)myMesh).flip();
         }
      }
      else {
         myMeshModifiedP = true;
      }
      saveOrRestoreModBitsIfNecessary (gtr, oldRigidP, oldModifiedP);
   }
   
   void transformGeometry (GeometryTransformer gtr) {
      transformGeometry (gtr, /*constrainer=*/null);
   }
   
   public void transformGeometry (
      GeometryTransformer gtr, GeometryTransformer.Constrainer constrainer) {

      if (myMesh != null) {
         gtr.transform (myMesh, constrainer);
      
         boolean oldRigidP = myFileTransformRigidP;
         boolean oldModifiedP = myMeshModifiedP;
         
         if (gtr.isAffine() || constrainer != null) {
            boolean reflecting;
            if (gtr.isRestoring()) {
               myFileTransform.set (gtr.restoreObject (myFileTransform));
               reflecting = gtr.restoreObject (new Boolean(false));
            }
            else {
               AffineTransform3dBase XC =
                  gtr.computeLinearizedTransform (Vector3d.ZERO);
               if (constrainer != null) {
                  constrainer.apply (XC);
               }
               reflecting = (XC.getMatrix().determinant() < 0);
               if (gtr.isSaving()) {
                  gtr.saveObject (new AffineTransform3d(myFileTransform));
                  gtr.saveObject (new Boolean(reflecting));
               }
               preMultiplyFileTransform (XC);
            }
            if (reflecting && myMesh instanceof PolygonalMesh) {
               myFlippedP = !myFlippedP;
               ((PolygonalMesh)myMesh).flip();
            }           
         }
         else {
            myMeshModifiedP = true;
         }
         saveOrRestoreModBitsIfNecessary (gtr, oldRigidP, oldModifiedP);
      }
   }
   
   public boolean transformGeometryAndPose (
      GeometryTransformer gtr, GeometryTransformer.Constrainer constrainer) {

      boolean meshWasTransformed = false;

      if (myMesh != null) {
         if (!gtr.isRigid()) {
            boolean oldRigidP = myFileTransformRigidP;
            boolean oldModifiedP = myMeshModifiedP;

            if (gtr.isAffine() || constrainer != null) {
               boolean reflecting;
               if (gtr.isRestoring()) {
                  myFileTransform.set (gtr.restoreObject (myFileTransform));
                  reflecting = gtr.restoreObject (new Boolean(false));
               }
               else {
                  // Pre-multiply myFileTransform by the local affine
                  // transform XL, which adjusts local vertex positions
                  // for the non-rigid parts of the transform that cannot
                  // be accommodated by the mesh-to-world transform 
                  // of the mesh-to-world transform
                  AffineTransform3d XL = gtr.computeLocalAffineTransform (
                     myMesh.getMeshToWorld(), constrainer);
                  reflecting = (XL.A.determinant() < 0);
                  if (gtr.isSaving()) {
                     gtr.saveObject (new AffineTransform3d(myFileTransform));
                     gtr.saveObject (new Boolean(reflecting));
                  }
                  preMultiplyFileTransform (XL);
               }
               if (reflecting && myMesh instanceof PolygonalMesh) {
                  myFlippedP = !myFlippedP;
                  ((PolygonalMesh)myMesh).flip();
               }
            }
            else {
               myMeshModifiedP = true;
            }
            gtr.transformWorld (myMesh, constrainer);
            meshWasTransformed = true;
            saveOrRestoreModBitsIfNecessary (gtr, oldRigidP, oldModifiedP);
         }
         else {
            RigidTransform3d XMW = 
               new RigidTransform3d(myMesh.getMeshToWorld());
            gtr.transform (XMW);
            myMesh.setMeshToWorld (XMW);
         }
      }

      return meshWasTransformed;
   }

   public void scan (ReaderTokenizer rtok) throws IOException {
      MeshBase mesh;
      String fileName = null;
      AffineTransform3dBase X = null;

      String meshClassName = "maspack.geometry.PolygonalMesh"; // default mesh class
      int dotSave = rtok.getCharSetting ('.');
      rtok.wordChar ('.');

      // rtok.scanWord ("mesh");
      try {
         if (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
            meshClassName = rtok.sval; // explicit mesh class name
         }
         else {
            rtok.pushBack();
         }
         rtok.scanToken ('[');
         rtok.nextToken();
         if (rtok.tokenIsQuotedString ('"') || rtok.ttype != ']') {
            mesh = (MeshBase)Class.forName (meshClassName).newInstance();
            if (rtok.tokenIsQuotedString ('"')) {
               File file = new File (rtok.sval);
               if (!file.isAbsolute()) {
                  file =
                     new File (ArtisynthPath.getWorkingDir() + File.separator
                               + rtok.sval);
               }
               if (!file.canRead()) {
                  throw new IOException (
                     "file '" + rtok.sval + "' not found or unreadable");
               }
               mesh.read (new BufferedReader (new FileReader (file)), false);
               fileName = new String (rtok.sval);
               while (rtok.nextToken() != ']') {
                  if (rtok.ttype == ReaderTokenizer.TT_WORD) {
                     String fieldName = rtok.sval;
                     if (fieldName.equals ("transform")) {
                        rtok.scanToken ('=');
                        rtok.scanWord();
                        if (rtok.sval.equals ("RigidTransform3d")) {
                           X = new RigidTransform3d();
                           ((RigidTransform3d)X).scan (rtok);
                        }
                        else if (rtok.sval.equals ("AffineTransform3d")) {
                           X = new AffineTransform3d();
                           ((AffineTransform3d)X).scan (rtok);
                        }
                        else {
                           throw new IOException (
                              "mesh transform type " + rtok.sval + " unknown");
                        }
                        mesh.transform (X);
                     }
                     else if (fieldName.equals ("triangular")){
                        rtok.scanToken ('=');
                        boolean isTriangular = rtok.scanBoolean();
                        if (isTriangular && mesh instanceof PolygonalMesh) {
                           ((PolygonalMesh)mesh).triangulate();
                        }
                     }
                     else if (fieldName.equals ("flipped")){
                        rtok.scanToken ('=');
                        myFlippedP = rtok.scanBoolean();
                        if (myFlippedP && mesh instanceof PolygonalMesh) {
                           ((PolygonalMesh)mesh).flip();
                        }
                     }
                     else {
                        throw new IOException (
                           "Unrecognized field name: '"+fieldName+"', "+rtok);
                     }
                  }
                  else {
                     throw new IOException ("Unexpected token: " + rtok);
                  }
               }
            }
            else { // rtok.ttype != ']' 
               rtok.pushBack();
               mesh.read (rtok, false);
               rtok.scanToken (']');
            }
         }
         else {
            mesh = null;
         }
      }
      catch (IOException e) {
         throw e;
      }
      catch (Exception e) {
         e.printStackTrace(); 
         throw new IOException ("Cannot create instance of " + meshClassName);
      }
      finally {
         rtok.setCharSetting ('.', dotSave);            
      }
      myMesh = mesh;
      if (myMesh != null) {
         myMesh.setRenderBuffered (true);
      }
      myFileName = fileName;
      myMeshModifiedP = false;
      setFileTransform (X);
   }

   private void writeTransformIfNecessary (PrintWriter pw, NumberFormat fmt)
      throws IOException {

      AffineTransform3dBase X = getFileTransform();
      if (X != null && !X.isIdentity()) {
         if (X instanceof RigidTransform3d) {
            pw.println ("transform=RigidTransform3d"
                        + ((RigidTransform3d)X).toString (
                           fmt, RigidTransform3d.AXIS_ANGLE_STRING));
         }
         else {
            pw.println ("transform=AffineTransform3d");
            pw.print ("[ ");
            IndentingPrintWriter.addIndentation (pw, 2);
            X.write (pw, fmt);
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
         }
      }            
   }

   public void write (PrintWriter pw, NumberFormat fmt) throws IOException {
      if (myMesh == null) {
         pw.println ("mesh=[ ]");
      }
      else {
         pw.println ("mesh="+myMesh.getClass().getName()+"[");
         IndentingPrintWriter.addIndentation (pw, 2);
         if (!myMeshModifiedP  && myFileName != null && myFileName.length() > 0) {
            pw.println (Write.getQuotedString (myFileName));
            writeTransformIfNecessary (pw, fmt);
            if (myMesh instanceof PolygonalMesh) {
               if (((PolygonalMesh)myMesh).isTriangular()) {
                  pw.println ("triangular=true");
               }
               if (myFlippedP) {
                  pw.println ("flipped=true");
               }
            }
         }
         else {
            myMesh.write (pw, fmt, /* zeroIndexed= */false);
            pw.println ("EOF");            
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");        
      }
   }
 
   public void prerender (RenderProps props) {
      if (myMesh != null) {
         myMesh.prerender (props);
      }
   }

   public void render (
      Renderer renderer, RenderProps props, boolean selected) {
      int flags = selected ? Renderer.HIGHLIGHT : 0;
      render(renderer, props, flags);         
   }
   
   public void render (
      Renderer renderer, RenderProps props, boolean selected, 
      int flags) {
      flags |= selected ? Renderer.HIGHLIGHT : 0;
      render(renderer, props, flags);
   }
   
   public void render (
      Renderer renderer, RenderProps props, int flags) {

      if (myMesh != null) {
         myMesh.render (renderer, props, flags);
      }         
   }
 
   public MeshInfo clone() {
      MeshInfo out = new MeshInfo();
      out.myFileName = myFileName;
      out.myMeshModifiedP = myMeshModifiedP;
      out.myFileTransform = myFileTransform.copy();
      out.myFlippedP = myFlippedP;
      out.myMesh = myMesh.copy();
      return out;
   }
   
   public MeshInfo copy() {
      return clone();
   }
   
}
