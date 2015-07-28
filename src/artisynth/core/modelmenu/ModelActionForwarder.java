package artisynth.core.modelmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import artisynth.core.driver.ModelInfo;

public class ModelActionForwarder implements ActionListener {
   
   ModelInfo mi = null;
   String cmd = null;
   ModelActionListener listener = null;
   
   public ModelActionForwarder(ModelActionListener listener, String cmd, ModelInfo mi) {
      this.mi = mi;
      this.listener = listener;
      this.cmd = cmd;
   }
   
   @Override
   public void actionPerformed(ActionEvent e) {
      // forward information on
      ModelActionEvent event = new ModelActionEvent(cmd, e, mi);
      if (listener != null) {
         listener.actionPerformed(event);
      }
   }

}
