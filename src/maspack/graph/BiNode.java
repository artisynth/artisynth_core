/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.graph;


/**
* Represents a node of the {@code Tree<A>} class. Has an extra "value" element
* of type B.
* 
*/
public class BiNode<A,B> extends Node<A> {
   
   private B value;      
   
   /**
    * Default constructor.
    */
   public BiNode() {
       super();
   }
   
   /**
    * Convenience constructor.
    * @param data the regular node data
    * @param value the extra value entity
    */
   public BiNode(A data, B value) {
       this();
       setData(data, value);
   }
    
   public B getValue() {
      return this.value;
  }

   public void setData(A data, B value) {
       this.data = data;
       this.value = value;
   }
    
   public String toString() {
       StringBuilder sb = new StringBuilder();
       sb.append("{").append(getData().toString()).append ("(").append (getValue().toString()).append(")").append(",[");
       int i = 0;
       for (Node<A> e : getChildren()) {
           if (i > 0) {
               sb.append(",");
           }
           sb.append(e.getData().toString());
           i++;
       }
       sb.append("]").append("}");
       return sb.toString();
   }

}
