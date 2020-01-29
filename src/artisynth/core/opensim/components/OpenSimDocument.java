package artisynth.core.opensim.components;

public class OpenSimDocument extends OpenSimObject {

   String version;
   ModelBase model;
   
   public OpenSimDocument() {
   }
   
   public String getVersion() {
      return version;
   }
   
   public void setVersion(String v) {
      version = v;
   }
   
   public void setModel(ModelBase model) {
      this.model = model;
      model.setParent (this);
   }
   
   public ModelBase getModel() {
      return model;
   }
   
   @Override
   public OpenSimDocument clone () {
      OpenSimDocument osim = (OpenSimDocument)super.clone ();
      if (model != null) {
         osim.setModel (model.clone ());
      }
      return osim;
   }
}
