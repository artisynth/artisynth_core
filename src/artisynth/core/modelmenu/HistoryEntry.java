package artisynth.core.modelmenu;

import artisynth.core.modelmenu.DemoMenuParser.MenuType;

public class HistoryEntry extends MenuNode {
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

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + compact;
      result = prime * result + size;
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      
      HistoryEntry other = (HistoryEntry)obj;
      if (compact != other.compact) {
         return false;
      }
      if (size != other.size) {
         return false;
      }
      return super.equals(other);
   }
   
   @Override
   public int compareTo (MenuNode o) {
      int cmp =  super.compareTo (o);
      
      if (cmp != 0) {
         return cmp;
      }
      
      HistoryEntry other = (HistoryEntry)o;
      
      cmp = Integer.compare (size, other.size);
      if (cmp != 0) {
         return cmp;
      }
      
      cmp = Integer.compare (compact, other.compact);
      
      return cmp;
   }
   
   
}
