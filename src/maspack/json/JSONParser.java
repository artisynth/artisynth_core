package maspack.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;

public class JSONParser {

   JSONHandler handler;
   JSONTokenizer jtok;
   
   private static class JSONEOF {
      @Override
      public String toString() {
         return "JSONParser.EOF";
      }
   }
   
   public static Object EOF = new JSONEOF();  // special signal for EOF
   
   public JSONParser(JSONHandler handler) {
      this.handler = handler;
   }
   
   public JSONParser() {
      this(new JSONFactoryHandler<>(new JSONFactoryDefault()));
   }
   
   public void parse(BufferedReader reader) {
      JSONTokenizer jtok = new JSONTokenizer(reader);
      parse(jtok);
   }
   
   public void parse(String str) {
      JSONTokenizer jtok = new JSONTokenizer(new BufferedReader(new StringReader(str)));
      parse(jtok);
   }
   
   public void parse(File file) throws FileNotFoundException {
      parse(new JSONTokenizer(file));
   }
   
   public void parse(JSONTokenizer jtok) {
      this.jtok = jtok;
      parse();
   }
   
   public void parse() {
      
      // next complete object
      while (jtok.next() != JSONTokenizer.EOF && !handler.hasElement()) {
         switch (jtok.token()) {
            case JSONTokenizer.OBJECT_START:
               handler.beginObject();
               break;
            case JSONTokenizer.OBJECT_END:
               handler.yieldObject();
               break;
            case JSONTokenizer.ARRAY_START:
               handler.beginArray();
               break;
            case JSONTokenizer.ARRAY_END:
               handler.yieldArray();
               break;
            case JSONTokenizer.COMMA:
               handler.yieldSeparator();
               break;
            case JSONTokenizer.COLON:
               handler.yieldKey();
               break;
            case JSONTokenizer.STRING:
               handler.yieldString(jtok.lastString());
               break;
            case JSONTokenizer.NUMBER:
               handler.yieldNumber(jtok.lastNumber());
               break;
            case JSONTokenizer.BOOLEAN: {
               boolean b = jtok.lastBoolean();
               if (b) {
                  handler.yieldTrue();
               } else {
                  handler.yieldFalse();
               }
               break;
            }
            case JSONTokenizer.NULL:
               handler.yieldNull();
               break;
            case JSONTokenizer.GARBAGE:
               handler.yieldGarbage(jtok.lastGarbage());
               break;
         }
      }
      
   }
   
   /**
    * Try to retrieve the next JSON object from the handler.  If none exists,
    * tries to parse more content and then retrieve the next element.
    * @return the next JSON element
    */
   public Object next() {
      if (handler.hasElement()) {
         return handler.pop();
      }
      
      if (jtok.token() == JSONTokenizer.EOF) {
         return JSONParser.EOF;
      }
      
      // try to parse
      parse();
      if (handler.hasElement()) {
         return handler.pop();
      }
      if (jtok.token() == JSONTokenizer.EOF) {
         return JSONParser.EOF;
      }
      return null;
   }
   
}
