package artisynth.core.modelmenu;

import java.util.ArrayList;

import javax.swing.JMenuItem;

public class HistoryMenuInfo {
   
   HistoryEntry hist;
   ArrayList<JMenuItem> items;
   
   public HistoryMenuInfo(HistoryEntry hist, ArrayList<JMenuItem> items) {
      this.hist = hist;
      this.items = items;
   }

}
