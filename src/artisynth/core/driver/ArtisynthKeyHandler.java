package artisynth.core.driver;

import java.awt.event.KeyListener;


public interface ArtisynthKeyHandler extends KeyListener {

   /**
    * Sets the main frame for which to send action commands e.g. play/pause
    * @param main
    */
   public void setMainFrame(MainFrame main);
   public MainFrame getMainFrame();
   
}
