package artisynth.demos.tutorial;

import java.io.IOException;

import maspack.properties.*;
import maspack.render.RenderProps;

import artisynth.core.workspace.RootModel;

public class SimpleMuscleWithProperties extends SimpleMuscleWithPanel {

   public static PropertyList myProps =
      new PropertyList (
         SimpleMuscleWithProperties.class, SimpleMuscleWithPanel.class);

   static {
      myProps.add ("boxVisible", "box is visible", false);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean getBoxVisible() {
      return box.getRenderProps().isVisible();
   }

   public void setBoxVisible (boolean visible) {
      RenderProps.setVisible (box, visible);
   }

   public void build (String[] args) throws IOException {

      super.build (args);

      panel.addWidget (this, "boxVisible");
      panel.pack();
   }

}
