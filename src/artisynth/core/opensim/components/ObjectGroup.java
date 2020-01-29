package artisynth.core.opensim.components;

import java.util.ArrayList;

public class ObjectGroup extends OpenSimObject {
   
   ArrayList<String> members;
   
   public ObjectGroup() {
      members = new ArrayList<>();
   }
   
   public void addMember(String member) {
      members.add (member);
   }
   
   public ArrayList<String> members() {
      return members;
   }
   
   public void clear() {
      members.clear ();
   }
   
   @Override
   public ObjectGroup clone () {
      ObjectGroup og = (ObjectGroup)super.clone ();
      // duplicate member array
      og.members = new ArrayList<String>();
      og.members.addAll (members);
      return og;
   }

}
