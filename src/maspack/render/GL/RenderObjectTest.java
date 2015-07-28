package maspack.render.GL;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

import maspack.fileutil.FileCacher;
import maspack.fileutil.uri.URIx;
import maspack.geometry.PolygonalMesh;
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
import maspack.render.RenderObjectFactory;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.RenderObject.BuildMode;
import maspack.render.RenderProps.Faces;
import maspack.render.RenderProps.Shading;
import maspack.render.GL.MultiViewerTest.SimpleSelectable;

public class RenderObjectTest {

   private static class RenderObjectWrapper implements SimpleSelectable {

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

         GLSupport.checkAndPrintGLError(viewer.getGL());

         if (myRO.hasNormals()) {
            renderer.setLightingEnabled(true);
            renderer.setMaterialAndShading(props, frontMaterial, null, backMaterial, null, selected);
         } else {
            renderer.setLightingEnabled(false);
            renderer.updateColor(frontMaterial.getDiffuse(), selected);
         }

         GLSupport.checkAndPrintGLError(viewer.getGL());

         if (trans != null) {
            viewer.pushModelMatrix();
            viewer.mulModelMatrix(trans);
         }

         GLSupport.checkAndPrintGLError(viewer.getGL());

         // renderer.drawVertices(myRO,VertexDrawMode.TRIANGLES);
         boolean enableVertexColoring = false;
         if (selected && viewer.isVertexColoringEnabled()) {
            viewer.setVertexColoringEnabled(false);
            enableVertexColoring = true;
         }

         boolean drewTriangles = false;
         if (myRO.hasTriangles()) {
            renderer.drawTriangles(myRO);
            drewTriangles = true;
         }

         GLSupport.checkAndPrintGLError(viewer.getGL());

         if (myRO.hasLines()) {
            renderer.updateMaterial(props, frontMaterial, props.getEdgeOrLineColorArray(), backMaterial, null, selected);
            renderer.setLineWidth(props.getLineWidth());

            if (drewTriangles) {
               viewer.addDepthOffset(-1e-3);
               renderer.drawLines(myRO);
               viewer.addDepthOffset(1e-3);
            } else {
               renderer.drawLines(myRO);
            }
         }

         GLSupport.checkAndPrintGLError(viewer.getGL());

         if (myRO.hasPoints()) {
            renderer.updateMaterial(props, props.getPointMaterial(), selected);
            if (drewTriangles) {
               viewer.addDepthOffset(-2e-3);
               renderer.drawPoints(myRO);
               viewer.addDepthOffset(2e-3);
            } else {
               renderer.drawPoints(myRO);
            }
         }

         GLSupport.checkAndPrintGLError(viewer.getGL());

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

   public static void main(String[] args) {

      MultiViewerTest rot = new MultiViewerTest();
      rot.addGL2Viewer("GL2 Viewer", 10, 10, 640, 480);
      rot.addGL3Viewer("GL3 Viewer", 650, 10, 640, 480);
      rot.syncMouseListeners();

      // read directly from standford bunny
      String bunnyURL = "http://graphics.stanford.edu/~mdfisher/Data/Meshes/bunny.obj";

      // bunny
      PolygonalMesh bunny = null;
      try {
         // read file directly from remote
         FileCacher cacher = new FileCacher();
         cacher.initialize();

         InputStream bunnyStream = cacher.getInputStream(new URIx(bunnyURL));
         InputStreamReader streamReader = new InputStreamReader(bunnyStream);
         WavefrontReader reader = new WavefrontReader(streamReader);

         bunny = new PolygonalMesh();
         reader.readMesh(bunny);
         bunny.computeVertexNormals();
         double r = bunny.computeRadius();
         Vector3d c = new Vector3d();
         bunny.computeCentroid(c);
         c.negate();
         bunny.scale(1.0/r);
         c.z -= 0.5;
         bunny.transform(new RigidTransform3d(c, new AxisAngle(1, 0, 0, Math.PI/2)));
         reader.close();
         cacher.release();
      } catch (IOException e1) {
         e1.printStackTrace();
         System.out.println("Unable to load stanford bunny... requires internet connection");
         bunny = null;
      }


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
         RenderObject bunnyRO;
         RenderObjectWrapper rbunny;
         bunnyRO = RenderObjectFactory.createFromMesh(bunny, rprops.getShading() == Shading.FLAT, rprops.getDrawEdges());
         rbunny = new RenderObjectWrapper(bunnyRO);
         rbunny.setRenderProps(rprops);         
         rot.addRenderable(rbunny);

         bunnyRO = RenderObjectFactory.createFromMesh(bunny, rprops.getShading() == Shading.FLAT, true);
         rbunny = new RenderObjectWrapper(bunnyRO);
         rbunny.setTransform(new RigidTransform3d(new Vector3d(0.707,-0.707,0), AxisAngle.IDENTITY));
         rbunny.setRenderProps(new RenderProps(rprops));         
         rbunny.getRenderProps().setDrawEdges(true);
         //         rbunny.getRenderProps().setFaceColor(new Color(175,153,128));
         //         rbunny.getRenderProps().setEdgeColor(new Color(175,153,128).darker());
         rbunny.getRenderProps().setFaceColor(new Color(154,103,52));
         rbunny.getRenderProps().setEdgeColor(rbunny.getRenderProps().getFaceColor().darker());
         rot.addRenderable(rbunny);

      }

      RenderObject cylinderRO = RenderObjectFactory.createCylinder(32, true);
      RenderObjectWrapper rcylinder = new RenderObjectWrapper(cylinderRO);
      AffineTransform3d cscale = new AffineTransform3d();
      cscale.setTranslation(0, 0.75, 0);
      cscale.setRotation(new AxisAngle(0,1,0,-Math.PI/3));
      cscale.applyScaling(0.05, 0.05, 1.0);
      rcylinder.setTransform(cscale);

      RenderProps rprops2 = rcylinder.getRenderProps();
      rprops2.set(rprops);
      rprops2.setDrawEdges(true);
      rprops2.setFaceColor(Color.CYAN);
      rot.addRenderable(rcylinder);

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

      rot.addRenderable(new RenderObjectWrapper(axes));

   }

}
