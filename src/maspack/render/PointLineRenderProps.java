/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.properties.PropertyList;

public class PointLineRenderProps extends RenderProps {
   public static PropertyList myProps =
      new PropertyList (PointLineRenderProps.class, RenderProps.class);

   static {
      myProps.remove ("faceStyle");
      myProps.remove ("faceColor");
      myProps.remove ("backColor");
      myProps.remove ("drawEdges");
      myProps.remove ("textureProps");

      myProps.remove ("edgeWidth");
      myProps.remove ("edgeColor");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

}
