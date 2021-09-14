package artisynth.core.modelmenu;

import artisynth.core.driver.ModelScriptInfo;

public class ModelScriptActionEvent {

   String cmd;
   Object source;
   ModelScriptInfo info;
   
   public ModelScriptActionEvent(String cmd, Object source, ModelScriptInfo info) {
      this.cmd = cmd;
      this.source = source;
      this.info = info;
   }
   
   public ModelScriptInfo getModelInfo() {
      return info;
   }
   
   public String getCommand() {
      return cmd;
   }
   
   public Object getSource() {
      return source;
   }
   
}
