package maspack.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;

public class JSONReader {

   JSONHandler handler;
   JSONTokenizer jtok;
   
   private static class JSONEOF {
      @Override
      public String toString() {
         return "JSONParser.EOF";
      }
   }
   
   //   private static class JSONERR {
   //      @Override
   //      public String toString() {
   //         return "JSONParser.ERROR";
   //      }
   //   }
   
   public static Object EOF = new JSONEOF();  // special signal for EOF
   //   public static Object ERROR = new JSONERR();  // special signal for ERROR
   
   public JSONReader(JSONHandler handler, JSONTokenizer jtok) {
      this.handler = handler;
      this.jtok = jtok;
   }
   
   public JSONReader(JSONTokenizer jtok) {
      this(new JSONFactoryHandler<>(new JSONFactoryDefault()), jtok);
   }
   
   public JSONReader() {
      this(new JSONFactoryHandler<>(new JSONFactoryDefault()), null);
   }
   
   public JSONTokenizer getTokenizer() {
      return jtok;
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
      int lastToken = 0;
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
               if (lastToken != JSONTokenizer.COMMA) {
                  handler.yieldSeparator();
               }
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
         lastToken = jtok.token();
      }
      
   }
   
   /**
    * Parses and returns next object
    * @return next JSON object
    */
   public Object read(BufferedReader reader) {
      this.jtok = new JSONTokenizer(reader);
      return read();
   }
   
   public Object read(String str) {
      this.jtok = new JSONTokenizer(new BufferedReader(new StringReader(str)));
      return read();
   }
   
   public Object read(File file) throws FileNotFoundException {
      this.jtok = new JSONTokenizer(file);
      return read();
   }
   
   public Object read(JSONTokenizer jtok) {
      this.jtok = jtok;
      return read();
   }
   
   /**
    * Clear saved info in the underlying handler
    */
   public void clear() {
      handler.clear();
   }
   
   
   /**
    * Try to retrieve the next JSON object from the handler.  If none exists,
    * tries to parse more content and then retrieve the next element.
    * @return the next JSON element
    */
   public Object read() {
      
      if (handler.hasElement()) {
         return handler.pop();
      }
      if (jtok == null) {
         return EOF;
      }
      
      if (jtok.token() == JSONTokenizer.EOF) {
         return JSONReader.EOF;
      }
      
      // try to parse
      parse();
      if (handler.hasElement()) {
         return handler.pop();
      }
      if (jtok.token() == JSONTokenizer.EOF) {
         return JSONReader.EOF;
      }
      return null;
   }
   
   public void close() {
      if (jtok != null) {
         jtok.close();
      }
   }
   
}
