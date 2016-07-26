package maspack.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {

   public void assertEquals(Object o1, Object o2) throws Exception {
      if (o1 == null || o2 == null) {
         if (o1 != o2) {
            throw new Exception("Assertion failed!\n\t" + o1 + "\n\t" + o2);
         }
      }
      if (!o1.equals(o2)) {
         throw new Exception("Assertion failed!\n\t" + o1.toString() + "\n\t" + o2.toString());
      }
   }

   public void testArrayDecode() throws Exception {

      String str = "[0,{\"1\":{\"2\":{\"3\":{\"4\":[5,{\"6\":7}]}}}}]";

      HashMap<String,Object> sixseven = new HashMap<>();
      sixseven.put("6", 7.0);
      ArrayList<Object> five = new ArrayList<Object>();
      five.add(5.0);
      five.add(sixseven);
      HashMap<String,Object> four = new HashMap<>();
      four.put("4", five);
      HashMap<String,Object> three = new HashMap<>();
      three.put("3", four);
      HashMap<String,Object> two = new HashMap<>();
      two.put("2", three);
      HashMap<String,Object> one = new HashMap<>();
      one.put("1", two);
      ArrayList<Object> zero = new ArrayList<Object>();
      zero.add(0.0);
      zero.add(one);

      JSONReader jparse = new JSONReader();
      Object l = jparse.read(str);
      assertEquals(l, zero);

      List<?> ll = (List<?>)l;
      assertEquals(ll.get(1), one);

      Map<?,?> l1 = (Map<?,?>)ll.get(1);
      assertEquals(two,l1.get("1"));

   }

   public void testObjectDecode() throws Exception {
      JSONReader jr = new JSONReader();
      Object r = jr.read("{\"first\": 123, \"second\": [4, 5, 6], \"third\": 789}");

      HashMap<String,Object> out = new HashMap<>();
      out.put("first", 123.0);
      List<Object> fourfivesix = Arrays.asList((Object)4.0, 5.0, 6.0);
      out.put("second", fourfivesix);
      out.put("third", 789.0);

      assertEquals(out, r);


   }

   public void testUnicode() throws Exception {
      String str = "[\"hello\\bworld\\\"abc\\tdef\\\\ghi\\rjkl\\n123\\u4e2d\"]";
      JSONReader jparse = new JSONReader();
      Object j = jparse.read(str);

      assertEquals("hello\bworld\"abc\tdef\\ghi\rjkl\n123中",((List<?>)j).get(0).toString());
   }

   public void testBoundaries() throws Exception {
      // empty
      String str = "{}";
      JSONReader jr = new JSONReader();
      Object empty = jr.read(str);
      assertEquals(empty, new HashMap<>());

      // extra comma
      str="[5,]";
      Object r = jr.read(str);
      ArrayList<Object> five = new ArrayList<Object>();
      five.add(5.0);
      assertEquals(r, five);

      // comma in middle
      r = jr.read("[5,,2]");
      five.add(2.0);
      assertEquals(r, five);

   }

   public void testErrors() throws Exception {

      JSONReader jr = new JSONReader();
      Object r = jr.read("{\"name\":");
      assertEquals(r, JSONReader.EOF);
      jr.clear();

      r = jr.read("{\"name");
      assertEquals(r, JSONReader.EOF);
      jr.clear();

      r = jr.read("[[null, 123.45, \"a\\\tb c\"}, true]");
      assertEquals(r, JSONReader.EOF);
      jr.clear();

   }

   public void testValues() throws Exception {
      String str = "{\"string\":\"Hello world\", \"integer\": 4, \"double\":-12e-5, \"true\": true,"
      + "\"false\": false, \"null\": null, \"array\":[\"I\",\"am\",\"legend\"]}";

      HashMap<String,Object> sol = new HashMap<String,Object>();
      sol.put("string", "Hello world");
      sol.put("integer", 4.0);
      sol.put("double", -12e-5);
      sol.put("true", true);
      sol.put("false", false);
      sol.put("null", null);
      sol.put("array", Arrays.asList("I", "am", "legend"));

      JSONReader jr = new JSONReader();
      Object r = jr.read(str);
      assertEquals(r, sol);
      jr.clear();

   }

   public void testConfig() throws Exception {
      String config = "{\n"
      + "\t\"uri\":\"http://www.artisynth.org/files/lib/vfs2.jar\",\n"
      + "\t\"username\":\"artisynth\",\n"
      + "\t\"password\":\"abcdefghijklmnopqrstuvwxyz\",\n"
      + "\t\"cipher\":\"AES/CBC/PKCS5Padding\",\n"
      + "\t\"key\":\"xplakjal3kj5lajksdf\"\n"
      + "}";
      // System.out.println(config);

      HashMap<String,Object> sol = new HashMap<String,Object>();
      sol.put("uri", "http://www.artisynth.org/files/lib/vfs2.jar");
      sol.put("username", "artisynth");
      sol.put("password", "abcdefghijklmnopqrstuvwxyz");
      sol.put("cipher", "AES/CBC/PKCS5Padding");
      sol.put("key", "xplakjal3kj5lajksdf");

      JSONReader jr = new JSONReader();
      Object r = jr.read(config);
      assertEquals(r, sol);
      jr.clear();
   }

   public static void main(String[] args) {
      Test test = new Test();
      boolean success = true;
      try {
         test.testArrayDecode();
         test.testUnicode();
         test.testBoundaries();
         test.testErrors();
         test.testObjectDecode();
         test.testValues();
         test.testConfig();
      } catch (Exception e) {
         e.printStackTrace();
         success = false;
      }
      if (success) {
         System.out.println("Great success!");
      }
   }

}
