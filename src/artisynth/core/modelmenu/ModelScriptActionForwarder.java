package artisynth.core.modelmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import artisynth.core.driver.ModelScriptInfo;

public class ModelScriptActionForwarder implements ActionListener {
   
   ModelScriptInfo mi = null;
   String cmd = null;
   ModelScriptActionListener listener = null;
   
   public ModelScriptActionForwarder (
      ModelScriptActionListener listener, String cmd, ModelScriptInfo mi) {
      this.mi = mi;
      this.listener = listener;
      this.cmd = cmd;
   }
   
   @Override
   public void actionPerformed(ActionEvent e) {
      // forward information on
      ModelScriptActionEvent event = new ModelScriptActionEvent(cmd, e, mi);
      if (listener != null) {
         listener.actionPerformed(event);
      }
   }

}
