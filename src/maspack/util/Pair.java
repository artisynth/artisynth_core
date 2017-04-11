package maspack.util;

/**
 * Basic "Pair"
 */
public class Pair<S, T> {
   public S first;
   public T second;
 
   public Pair(S first, T second) {
      this.first = first;
      this.second = second;
   }
   
   public S first() {
      return first;
   }
   
   public T second() {
      return second;
   }
   
}
