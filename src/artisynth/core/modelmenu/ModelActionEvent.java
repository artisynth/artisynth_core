package artisynth.core.modelmenu;

import artisynth.core.driver.ModelInfo;

public class ModelActionEvent {

   String cmd;
   Object source;
   ModelInfo info;
   
   public ModelActionEvent(String cmd, Object source, ModelInfo info) {
      this.cmd = cmd;
      this.source = source;
      this.info = info;
   }
   
   public ModelInfo getModelInfo() {
      return info;
   }
   
   public String getCommand() {
      return cmd;
   }
   
   public Object getSource() {
      return source;
   }
   
}
