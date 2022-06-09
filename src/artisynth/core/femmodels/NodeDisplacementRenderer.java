package artisynth.core.femmodels;

import java.util.*;
import artisynth.core.modelbase.*;

import maspack.render.*;
import maspack.properties.*;
import maspack.matrix.*;
import maspack.render.Renderer.*;


public class NodeDisplacementRenderer extends RenderableComponentBase {

   private ArrayList<FemNode3d> myNodes = new ArrayList<>();
   private RenderObject myRob;

   public static PropertyList myProps =
      new PropertyList (
         NodeDisplacementRenderer.class, RenderableComponentBase.class);

   static {
      myProps.add ("renderProps", "render properties", null);
   }

   protected static RenderProps defaultRenderProps (HasProperties host) {
      return RenderProps.createLineProps (host);
   }
   public NodeDisplacementRenderer () {
      myRenderProps = createRenderProps();
   }

   public NodeDisplacementRenderer (Collection<FemNode3d> nodes) {
      this();
      addNodes (nodes);
   }

   public void addNode (FemNode3d n) {
      myNodes.add (n);
   }

   public void addNodes (Collection<FemNode3d> nodes) {
      myNodes.addAll (nodes);
   }

   public boolean removeNode (FemNode3d n) {
      return myNodes.remove (n);
   }

   public void clearNodes () {
      myNodes.clear();
   }

   public int numNodes() {
      return myNodes.size();
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void updateBounds (Point3d min, Point3d max) {
      for (FemNode3d n : myNodes) {
         n.getPosition().updateBounds(min, max);
         n.getRestPosition().updateBounds(min, max);
      }
   }

   private void buildRenderObject() {
      RenderObject rob = new RenderObject();
      rob.createLineGroup();
      int vidx = 0;
      for (FemNode3d n : myNodes) {
         rob.addPosition (n.getRestPosition());
         rob.addVertex (vidx);
         rob.addPosition (n.getPosition());
         rob.addVertex (vidx+1);
         rob.addLine (vidx, vidx+1);
         vidx += 2;
      }
      myRob = rob;
   }

   public void prerender (RenderList rlist) {
      buildRenderObject();
   }

   public void render (Renderer r, int flags) {
      RenderObject rob = myRob;
      if (rob != null) {
         drawLines (r, rob, myRenderProps, isSelected());
      }
   }

   private void drawLines (
      Renderer renderer, RenderObject rob, RenderProps props, boolean selected) {
   
      LineStyle style = props.getLineStyle();
      Shading savedShading = renderer.setLineShading(props);
      renderer.setLineColoring (props, selected);
      switch (style) {
         case LINE: {
            int width = props.getLineWidth();
            if (width > 0) {
               //renderer.setLightingEnabled (false);
               //renderer.setColor (props.getLineColorArray(), selected);
               renderer.drawLines (rob, LineStyle.LINE, width);
               //renderer.setLightingEnabled (true);
            }
            break;
         }
         case SPINDLE:
         case SOLID_ARROW:
         case CYLINDER: {
            double rad = props.getLineRadius();
            if (rad > 0) {
               //Shading savedShading = renderer.getShadeModel();
               //renderer.setLineLighting (props, selected);
               renderer.drawLines (rob, style, rad);
               //renderer.setShadeModel(savedShading);
            }
            break;
         }
      }
      renderer.setShading(savedShading);
   }


}
