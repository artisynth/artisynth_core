/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.properties.PropertyList;

public class LineEdgeRenderProps extends RenderProps {
   public static PropertyList myProps =
      new PropertyList (LineEdgeRenderProps.class, RenderProps.class);

   //private static int myDefaultLineSlices = 16;

   static {
      myProps.remove ("alpha");
      myProps.remove ("shading");
      myProps.remove ("shininess");
      myProps.remove ("ambience");
      
      myProps.remove ("faceStyle");
      myProps.remove ("faceColor");
      myProps.remove ("backColor");
      myProps.remove ("drawEdges");
      myProps.remove ("textureProps");

      myProps.remove ("pointStyle");
      myProps.remove ("pointSize");
      myProps.remove ("pointColor");
      myProps.remove ("pointSize");
      myProps.remove ("pointRadius");

//      myProps.get ("lineSlices").setDefaultValue (
//         myDefaultLineSlices);

      // myProps.createGroup("Edges...", "edgeWidth", "edgeColor");
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      //myLineSlices = myDefaultLineSlices;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

}
