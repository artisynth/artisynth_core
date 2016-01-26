package maspack.render.GL.test;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import maspack.fileutil.FileCacher;
import maspack.fileutil.uri.URIx;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.io.WavefrontReader;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.Material;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderObject.BuildMode;
import maspack.render.RenderObjectFactory;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Faces;
import maspack.render.RenderProps.LineStyle;
import maspack.render.RenderProps.PointStyle;
import maspack.render.RenderProps.Shading;
import maspack.render.Renderer;
import maspack.render.Transrotator3d;
import maspack.render.GL.GLViewer;
import maspack.render.GL.test.MultiViewer.SimpleSelectable;
import maspack.render.GL.test.MultiViewer.SimpleViewerApp;

public class MultiViewerTesterBase {

   protected static void addCube(MultiViewer tester) {
      RenderObject cube = new RenderObject();
      // positions                                 // normals
      int[] pIdxs = new int[8];                    int[] nIdxs = new int[6];
      pIdxs[0] = cube.addPosition(-1f,-1f,-1f);    nIdxs[0] = cube.addNormal( 0f, 0f,-1f);
      pIdxs[1] = cube.addPosition( 1f,-1f,-1f);    nIdxs[1] = cube.addNormal( 0f, 0f, 1f);
      pIdxs[2] = cube.addPosition( 1f, 1f,-1f);    nIdxs[2] = cube.addNormal( 0f,-1f, 0f);
      pIdxs[3] = cube.addPosition(-1f, 1f,-1f);    nIdxs[3] = cube.addNormal( 0f, 1f, 0f);
      pIdxs[4] = cube.addPosition(-1f,-1f, 1f);    nIdxs[4] = cube.addNormal(-1f, 0f, 0f);
      pIdxs[5] = cube.addPosition( 1f,-1f, 1f);    nIdxs[5] = cube.addNormal( 1f, 0f, 0f);
      pIdxs[6] = cube.addPosition( 1f, 1f, 1f);
      pIdxs[7] = cube.addPosition(-1f, 1f, 1f);

      // vertices
      int[] vIdxs = new int[24];
      // bottom    indices:(position, normal, color, texture)
      vIdxs[0]  = cube.addVertex(0,0,-1,-1);  vIdxs[1]  = cube.addVertex(1,0,-1,-1);
      vIdxs[2]  = cube.addVertex(2,0,-1,-1);  vIdxs[3]  = cube.addVertex(3,0,-1,-1);
      // top
      vIdxs[4]  = cube.addVertex(4,1,-1,-1);  vIdxs[5]  = cube.addVertex(5,1,-1,-1);
      vIdxs[6]  = cube.addVertex(6,1,-1,-1);  vIdxs[7]  = cube.addVertex(7,1,-1,-1);
      // left
      vIdxs[8]  = cube.addVertex(0,2,-1,-1);  vIdxs[9]  = cube.addVertex(1,2,-1,-1);
      vIdxs[10] = cube.addVertex(4,2,-1,-1);  vIdxs[11] = cube.addVertex(5,2,-1,-1);
      // right
      vIdxs[12] = cube.addVertex(2,3,-1,-1);  vIdxs[13] = cube.addVertex(3,3,-1,-1);
      vIdxs[14] = cube.addVertex(6,3,-1,-1);  vIdxs[15] = cube.addVertex(7,3,-1,-1);
      // front
      vIdxs[16] = cube.addVertex(3,4,-1,-1);  vIdxs[17] = cube.addVertex(0,4,-1,-1);
      vIdxs[18] = cube.addVertex(7,4,-1,-1);  vIdxs[19] = cube.addVertex(4,4,-1,-1);
      // back
      vIdxs[20] = cube.addVertex(1,5,-1,-1);  vIdxs[21] = cube.addVertex(2,5,-1,-1);
      vIdxs[22] = cube.addVertex(5,5,-1,-1);  vIdxs[23] = cube.addVertex(6,5,-1,-1);

      // triangular faces
      cube.addTriangle( 2, 1, 0);  cube.addTriangle( 3, 2, 0);  // bottom
      cube.addTriangle( 4, 5, 6);  cube.addTriangle( 7, 4, 6);  // top
      cube.addTriangle( 8, 9,10);  cube.addTriangle( 9,11,10);  // left
      cube.addTriangle(12,13,14);  cube.addTriangle(13,15,14);  // right
      cube.addTriangle(16,17,18);  cube.addTriangle(17,19,18);  // front
      cube.addTriangle(20,21,22);  cube.addTriangle(21,23,22);  // back

      // add to renderer
      RenderObjectWrapper cuber = new RenderObjectWrapper(cube);
      AffineTransform3d trans = new AffineTransform3d();
      trans.setTranslation(0.5, 0.5, 0.5);
      trans.applyScaling(0.2, 0.2, 0.2);
      cuber.setTransform(trans);
      RenderProps props = cuber.getRenderProps();
      props.setFaceColor(Color.ORANGE.darker());
      tester.addRenderable(cuber);
   }

   protected static void addTransRotator(MultiViewer tester) {
      for (SimpleViewerApp app : tester.getWindows()) {
         Transrotator3d dragger = new Transrotator3d();
         dragger.setPosition(new Vector3d(1,0,0.2));
         app.viewer.addDragger(dragger);
      }
   }

   protected static void addAxes(MultiViewer tester) {
      // axes
      RenderObject axes = new RenderObject();
      axes.beginBuild(BuildMode.LINES);
      axes.color(255, 0, 0, 255);
      axes.vertex(0, 0, 0);
      axes.vertex(1, 0, 0);
      axes.color(0, 255, 0, 255);
      axes.vertex(0, 0, 0);
      axes.vertex(0, 1, 0);
      axes.color(0, 0, 255, 255);
      axes.vertex(0, 0, 0);
      axes.vertex(0, 0, 1);
      axes.endBuild();
      tester.addRenderable(new RenderObjectWrapper(axes));
   }

   protected static void addCylinder(MultiViewer tester) {
      // cylinder
      RenderObject cylinderRO = RenderObjectFactory.createCylinder(32, true);

      RenderObjectWrapper rcylinder = new RenderObjectWrapper(cylinderRO);
      AffineTransform3d cscale = new AffineTransform3d();
      cscale.setTranslation(0.3, 0.3, 0);
      cscale.setRotation(new AxisAngle(1d/Math.sqrt(2), -1d/Math.sqrt(2), 0, Math.PI/8));
      cscale.applyScaling(0.05, 0.05, 1.0);
      rcylinder.setTransform(cscale);

      RenderProps rprops2 = rcylinder.getRenderProps();
      rprops2.setFaceColor(Color.CYAN);
      rprops2.setShading(Shading.PHONG);
      tester.addRenderable(rcylinder);
   }

   protected static PolygonalMesh loadStanfordBunny() {
      // read Standford bunny directly
      String bunnyURL =
      "http://graphics.stanford.edu/~mdfisher/Data/Meshes/bunny.obj";
      // bunny
      File bunnyFile = new File("tmp/data/stanford_bunny.obj");
      
      PolygonalMesh bunny = null;
      try {
         if (!bunnyFile.exists ()) {
            bunnyFile.getParentFile().mkdirs ();
            
            // read file directly from remote
            FileCacher cacher = new FileCacher();
            cacher.initialize();
            cacher.cache (new URIx(bunnyURL), bunnyFile);  
            cacher.release ();
         }

         WavefrontReader reader = new WavefrontReader(bunnyFile);

         bunny = new PolygonalMesh();
         reader.readMesh(bunny);
         //bunny.computeVertexNormals();
         // normalize bunny
         double r = bunny.computeRadius();
         Vector3d c = new Vector3d();
         bunny.computeCentroid(c);
         c.negate();
         bunny.scale(1.0 / r);
         c.z -= 0.5;
         bunny.transform(new RigidTransform3d(c, new AxisAngle(
            1, 0, 0, Math.PI / 2)));
         reader.close();
         
      } catch (IOException e1) {
         e1.printStackTrace();
         System.out.println("Unable to load stanford bunny... requires internet connection");
         bunny = null;
      }

      return bunny;
   }

   protected static void addStanfordBunnies(MultiViewer tester, PolygonalMesh bunny) {

      RenderProps rprops = new RenderProps();
      rprops.setFaceStyle(Faces.FRONT_AND_BACK);
      rprops.setShading(Shading.PHONG);
      rprops.setFaceColor(Color.WHITE.darker());
      rprops.setBackColor(Color.BLUE);
      rprops.setLineColor(Color.ORANGE);
      rprops.setFaceColorSpecular(Color.WHITE);
      rprops.setShininess(1000);
      rprops.setPointSlices(24);
      rprops.setLineSlices(24);
      rprops.setShading(Shading.PHONG);

      if (bunny != null) {

         // one bunny
         RenderObject bunnyRO;
         RenderObjectWrapper rbunny;
         bunnyRO =
         RenderObjectFactory.createFromMesh(
            bunny, rprops.getShading() == Shading.FLAT,
            rprops.getDrawEdges());
         rbunny = new RenderObjectWrapper(bunnyRO);
         rbunny.setTransform(new RigidTransform3d(
            new Vector3d(-0.707, 0.707, 0), AxisAngle.IDENTITY));
         rbunny.setRenderProps(rprops);
         tester.addRenderable(rbunny);

         // two bunny
         bunnyRO =
         RenderObjectFactory.createFromMesh(
            bunny, rprops.getShading() == Shading.FLAT, true);
         rbunny = new RenderObjectWrapper(bunnyRO);
         rbunny.setTransform(new RigidTransform3d(
            new Vector3d(0.707, -0.707, 0), AxisAngle.IDENTITY));
         rbunny.setRenderProps(new RenderProps(rprops));
         rbunny.getRenderProps().setDrawEdges(true);
         rbunny.getRenderProps().setFaceColor(new Color(175, 153, 128));
         rbunny
         .getRenderProps().setEdgeColor(new Color(175, 153, 128).darker());
         tester.addRenderable(rbunny);

      }
   }

   protected static void addSolidBunny(MultiViewer tester, PolygonalMesh bunny) {

      RenderProps rprops = new RenderProps();
      rprops.setFaceStyle(Faces.FRONT_AND_BACK);
      rprops.setShading(Shading.PHONG);
      rprops.setFaceColor(new Color(20, 20, 20));
      rprops.setBackColor(Color.MAGENTA.darker());
      rprops.setLineColor(Color.ORANGE);
      rprops.setPointColor(Color.PINK);
      rprops.setFaceColorSpecular(Color.WHITE);
      rprops.setShininess(1000);
      rprops.setPointSlices(24);
      rprops.setLineSlices(24);
      rprops.setShading(Shading.PHONG);
      rprops.setPointStyle(PointStyle.SPHERE);
      rprops.setLineStyle(LineStyle.SOLID_ARROW);
      rprops.setLineRadius(0.001);
      rprops.setPointRadius(0.005);

      if (bunny != null) {

         // one bunny
         RenderObject bunnyRO;
         RenderObjectWrapper rbunny;
         bunnyRO =
         RenderObjectFactory.createFromMesh(
            bunny, rprops.getShading() == Shading.FLAT,
            true);
         // vertices
         for (int i=0; i<bunnyRO.numVertices(); ++i) {
            bunnyRO.addPoint(i);
         }
         rbunny = new RenderObjectWrapper(bunnyRO);
         rbunny.setTransform(new RigidTransform3d(
            new Vector3d(0, 0, 0), AxisAngle.IDENTITY));
         rbunny.setRenderProps(rprops);

         tester.addRenderable(rbunny);

      }
   }

   protected static void addHalfBunny(MultiViewer tester, PolygonalMesh bunny) {

      RenderProps rprops = new RenderProps();
      rprops.setFaceStyle(Faces.FRONT_AND_BACK);
      rprops.setShading(Shading.PHONG);
      rprops.setBackColor(Color.MAGENTA.darker());
      rprops.setFaceColorSpecular(Color.WHITE);
      rprops.setShininess(1000);

      if (bunny != null) {

         RenderObject r = new RenderObject();

         // add all appropriate info
         for (Vertex3d vtx : bunny.getVertices()) {
            Point3d pos = vtx.getPosition();
            r.addPosition((float)pos.x, (float)pos.y, (float)pos.z);
         }
         for (Vector3d nrm : bunny.getNormals()) {
            r.addNormal((float)nrm.x, (float)nrm.y, (float)nrm.z);
         }
         int nidxs[] = bunny.getNormalIndices();

         r.createTriangleGroup();  // left
         r.createTriangleGroup();  // right

         // build faces
         List<Face> faces = bunny.getFaces();
         int[] indexOffs = bunny.getFeatureIndexOffsets();
         Vector3d centroid = new Vector3d();
         final int[] invalid = new int[] {-1}; 
         for (int i=0; i<faces.size(); i++) {
            Face f = faces.get(i);
            int foff = indexOffs[f.idx];

            int[] pidxs = f.getVertexIndices();

            int[] vidxs = new int[pidxs.length]; // vertex indices
            for (int j=0; j<pidxs.length; j++) {

               // only add if unique combination
               RenderObject.VertexIndexSet v = new RenderObject.VertexIndexSet(
                  pidxs[j], 
                  nidxs[foff + j],
                  -1,
                  -1);
               vidxs[j] = r.addVertex(v.getPositionIndex(), v.getNormalIndex(), -1, -1);
            }
            // triangle fan for faces
            f.computeCentroid(centroid);
            if (centroid.x < centroid.y) {
               r.triangleGroup(0);
            } else {
               r.triangleGroup(1);
            }
            r.addTriangleFan(vidxs);

         }

         MultiTriangleGroupWrapper rbunny = new MultiTriangleGroupWrapper(r);
         rbunny.setTransform(new RigidTransform3d(
            new Vector3d(1.3, 1.3, 0.5), AxisAngle.IDENTITY));
         rbunny.setRenderProps(rprops);
         rbunny.setFaceColors(Color.RED, Color.BLUE);

         tester.addRenderable(rbunny);

      }
   }

   protected static class RenderObjectWrapper implements SimpleSelectable {

      RenderObject myRO;
      private boolean selected;
      RenderProps props;
      AffineTransform3dBase trans;

      public RenderObjectWrapper(RenderObject robj) {
         selected = false;
         myRO = robj;
         props = new RenderProps();
         trans = null;
      }

      public void setRenderProps(RenderProps props) {
         this.props = props;
      }

      public RenderProps getRenderProps() {
         return props;
      }

      @Override
      public void prerender(RenderList list) {
         selected = false;
      }

      public void setSelected(boolean sel) {
         selected = sel;
      }

      public boolean isSelected() {
         return selected;
      }

      public void setTransform(AffineTransform3dBase t) {
         trans = t.clone();
      }

      @Override
      public void render(Renderer renderer, int flags) {

         if (!(renderer instanceof GLViewer)) {
            return;
         }
         GLViewer viewer = (GLViewer)renderer;

         renderer.setFaceMode(props.getFaceStyle());
         Material frontMaterial = props.getFaceMaterial();
         Material backMaterial = props.getBackMaterial();
         if (backMaterial == null) {
            backMaterial = frontMaterial;
         }

         renderer.setMaterialAndShading(
            props, frontMaterial, null, backMaterial, null, selected);

         if (trans != null) {
            viewer.pushModelMatrix();
            viewer.mulModelMatrix(trans);
         }

         boolean enableVertexColoring = false;
         if (selected && viewer.isVertexColoringEnabled()) {
            viewer.setVertexColoringEnabled(false);
            enableVertexColoring = true;
         }

         boolean didFlatDraw = false;
         float dOffset = 0;
         if (myRO.hasTriangles()) {
            renderer.drawTriangles(myRO);
            didFlatDraw = true;
         }

         float depthOffInc = -1e-4f;

         if (myRO.hasLines()) {
            renderer.updateMaterial(
               props, frontMaterial, props.getEdgeOrLineColorArray(),
               backMaterial, null, selected);
            LineStyle lstyle = props.getLineStyle();
            if (lstyle == LineStyle.LINE) {
               if (didFlatDraw) {
                  viewer.addDepthOffset(depthOffInc);
                  dOffset += depthOffInc;
               }
               viewer.setLineWidth(props.getLineWidth());
               renderer.drawLines(myRO);
               didFlatDraw = true;
            } else {
               renderer.drawLines(myRO, lstyle, props.getLineRadius());
            }
         }

         if (myRO.hasPoints()) {
            renderer.updateMaterial(props, props.getPointMaterial(), selected);
            PointStyle pstyle = props.getPointStyle();
            if (pstyle == PointStyle.POINT) {
               if (didFlatDraw) {
                  viewer.addDepthOffset(depthOffInc);
                  dOffset += depthOffInc;
               }
               renderer.setPointSize(props.getPointSize());
               renderer.drawPoints(myRO);
               didFlatDraw = true;
            } else {
               renderer.drawPoints(myRO, pstyle, props.getPointRadius());
            }
         }

         if (dOffset != 0) {
            viewer.addDepthOffset(-dOffset);
         }

         if (enableVertexColoring) {
            viewer.setVertexColoringEnabled(true);
         }

         if (trans != null) {
            viewer.popModelMatrix();
         }
      }

      @Override
      public void updateBounds(Point3d pmin, Point3d pmax) {
      }

      @Override
      public int getRenderHints() {
         return 0;
      }

      @Override
      public boolean isSelectable() {
         return true;
      }

      @Override
      public int numSelectionQueriesNeeded() {
         return -1;
      }

      @Override
      public void getSelection(LinkedList<Object> list, int qid) {
      }
   }
   
   protected static class MultiTriangleGroupWrapper extends RenderObjectWrapper {

      private Color[] faceColors;
      
      public MultiTriangleGroupWrapper(RenderObject robj) {
         super(robj);
      }
      
      @Override
      public void render(Renderer renderer, int flags) {
         for (int i=0; i<myRO.numTriangleGroups(); ++i) {
            myRO.triangleGroup(i);
            props.setFaceColor(faceColors[i % faceColors.length]);
            super.render(renderer, flags);
         }
      }
      
      public void setFaceColors(Color... colors) {
         faceColors = colors;
      }
      
   }
   
}
