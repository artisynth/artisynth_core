package artisynth.core.opensim.components;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

public class OpenSimDocumentFactory extends OpenSimObjectFactory<OpenSimDocument> {

   protected OpenSimDocumentFactory (Class<? extends OpenSimDocument> instanceClass) {
      super (instanceClass);
   }
   
   public OpenSimDocumentFactory () {
      super(OpenSimDocument.class);
   }
   
   @Override
   protected boolean parseAttribute (OpenSimDocument comp, Attr attr) {
     
      boolean success = true;
      
      String name = getNodeName(attr);
      
      if ("Version".equals (name)) {
         comp.setVersion (attr.getValue ());
      } else {
         success = super.parseAttribute (comp, attr);
      }
      
      return success;
   }

   @Override
   protected boolean parseChild (OpenSimDocument comp, Element child) {
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("Model".equals (name)) {
         OpenSimObjectFactory<? extends ModelBase> factory = findFactory (ModelBase.class, child);
         if (factory != null) {
            ModelBase model = factory.parse (child);
            comp.setModel (model);
         } else {
            success = false;
         }
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
  
   
}
