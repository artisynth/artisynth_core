package artisynth.demos.fem;

import artisynth.demos.fem.SheetDemo.ElementType;

public class HexSheet extends SheetDemo {
   public HexSheet (String name) {
      super (name, ElementType.Hex, 5, 5, 1);
   }
   
   public HexSheet() {
      super();
   }
}
