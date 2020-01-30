package artisynth.core.opensim.components;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public abstract class ModelFactoryBase<E extends ModelBase> extends OpenSimObjectFactory<E> {

   protected ModelFactoryBase (Class<? extends E> instanceClass) {
      super (instanceClass);
   }
   
   protected int getModelVersion(Element modelElem) {
      int version = -1;
      Element parent = (Element)(modelElem.getParentNode ());
      if (parent != null && "OpenSimDocument".equals(getNodeName(parent))) {
         NamedNodeMap attributes = parent.getAttributes ();
         if (attributes != null) {
            String versionStr = parent.getAttribute ("Version");
            if (versionStr != null) {
               version = Integer.parseInt (versionStr);
            }
         }
      }
      return version;
   }
   
   @Override
   protected boolean parseChild (E comp, Element child) {
      
      boolean success = true;
      
      String name = getNodeName (child);
      
      if ("defaults".equals(name)) {
         // process defaults
         success = DefaultsParser.parse (getFactoryStore(), child);
      } else if ("credits".equals(name)) {
         comp.setCredits (parseTextValue (child));
      } else if ("publications".equals (name)) {
         comp.setPublications (parseTextValue (child));
      } else if ("length_units".equals(name)) {
         comp.setLengthUnits (parseTextValue (child));
      } else if ("force_units".equals (name)) {
         comp.setForceUnits (parseTextValue (child));
      } else if ("gravity".equals(name)) {
         comp.setGravity (parseVector3dValue (child));
      } else if ("BodySet".equals(name)) {
         
         OpenSimObjectFactory<? extends BodySet> factory = getFactory (BodySet.class);
         if (factory != null) {
            comp.setBodySet (factory.parse (child));
         } else {
            success = false;
         }
         
      } else if ("ForceSet".equals(name)) {
       
         OpenSimObjectFactory<? extends ForceSet> factory = getFactory (ForceSet.class);
         if (factory != null) {
            comp.setForceSet (factory.parse (child));
         } else {
            success = false;
         }
         
      } else if ("MarkerSet".equals(name)) {
       
         OpenSimObjectFactory<? extends MarkerSet> factory = getFactory (MarkerSet.class);
         if (factory != null) {
            comp.setMarkerSet (factory.parse (child));
         } else {
            success = false;
         }
         
      } else {
         success = super.parseChild (comp, child);
      }
      
      return success;
   }
   
   @Override
   public E parse (Element elem) {
      E model = super.parse (elem);
      
      if (model != null) {
         // get version from parent
         int version = getModelVersion (elem);
         model.setVersion (version);
      }
      return model;
   }

}
