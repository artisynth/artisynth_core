package artisynth.core.modelmenu;

import artisynth.core.modelmenu.DemoMenuParser.MenuType;

public class HistoryEntry extends MenuEntry {
   private int size;
   private int compact;
   
   public HistoryEntry(int size, int compact) {
      this.size = size;
      this.compact = compact;
   }
   
   public int getSize() {
      return size;
   }
   
   public void setSize(int s) {
      size = s;
   }
   
   public int getCompact() {
      return compact;
   }
   
   public void setCompact(int c) {
      compact = c;
   }
   
   @Override
   public MenuType getType() {
      return MenuType.HISTORY;
   }
}
