package artisynth.core.modelmenu;

public class MenuEntry extends MenuNode {

   protected static final int DEFAULT_MAX_ROWS = 20;
   
   private int maxRows;
   private boolean scrolling;
   
   public MenuEntry() {
      super();
      scrolling = false;
      maxRows = DEFAULT_MAX_ROWS;
   }
   
   public MenuEntry(String title) {
      super(title);
      scrolling = false;
      maxRows = DEFAULT_MAX_ROWS;
   }
   
   public void setMaxRows(int max) {
      maxRows = max;
   }
   
   public int getMaxRows() {
      return maxRows;
   }
   
   public void setScrolling(boolean set) {
      this.scrolling = set;
   }
   
   public boolean isScrolling() {
      return scrolling;
   }
   
   @Override
   public int hashCode () {
      return 31*(31*super.hashCode () + maxRows) + (scrolling ? 13 : 0);
   }
   
   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (obj == this) {
         return true;
      }
      
      if (!(obj.getClass() == getClass())) { 
         return false; 
      }

      MenuEntry other = (MenuEntry) obj;
      
      return equals(other);
      
   }
   
   public boolean equals(MenuEntry other) {
      
      if (scrolling != other.scrolling) {
         return false;
      }
      
      if (maxRows != other.maxRows) {
         return false;
      }
      
      return super.equals (other);
   }
   
   @Override
   public int compareTo (MenuNode o) {
      int cmp =  super.compareTo (o);
      
      if (cmp != 0) {
         return cmp;
      }
      
      MenuEntry other = (MenuEntry)o;
      
      cmp = Integer.compare (maxRows, other.maxRows);
      if (cmp != 0) {
         return cmp;
      }
      
      cmp = Boolean.compare (scrolling, other.scrolling);
      return cmp;
   }
}
