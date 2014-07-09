/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import maspack.properties.PropertyList;

public class MeshRenderProps extends RenderProps {
   public static PropertyList myProps =
      new PropertyList (MeshRenderProps.class, RenderProps.class);

   protected static TextureProps defaultTextureProps = new TextureProps();

   static {
      myProps.remove ("lineStyle");
      myProps.remove ("lineRadius");
      myProps.remove ("lineSlices");

      myProps.remove ("pointStyle");
      myProps.remove ("pointRadius");
      myProps.remove ("pointSlices");

      myProps.get ("textureProps").setDefaultValue (defaultTextureProps);
   }

   protected void setDefaultValues() {
      super.setDefaultValues();
      setTextureProps (new TextureProps());
      //setTextureProps (null);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

}
