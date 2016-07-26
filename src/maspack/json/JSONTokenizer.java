package maspack.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

public class JSONTokenizer {

   public static final int EOF = -1;
   public static final int OBJECT_START = (int)('{');
   public static final int OBJECT_END = (int)('}');
   public static final int COLON = (int)(':');
   public static final int COMMA = (int)(',');
   public static final int ARRAY_START = (int)('[');
   public static final int ARRAY_END = (int)(']');
   public static final int STRING = -6;
   public static final int NUMBER = -7;
   public static final int BOOLEAN = -8;
   public static final int NULL = -9;
   public static final int GARBAGE = -10;   // any non-standard JSON content (maybe a comment?)
   public static final int EMPTY = -11;
   
   private BufferedReader reader;
   
   private int token;
   private String sval;
   private boolean bval;
   private double nval;
   private String gval;
   
   private int currentCode;  // a number between 0x0000-0xFFFF, or -1 for EOF, or -11 for empty
   private long pos;
   
   public JSONTokenizer(String fileName) throws FileNotFoundException {
      this(new BufferedReader(new FileReader(fileName)));
   }
   
   public JSONTokenizer(File file) throws FileNotFoundException {
      this(new BufferedReader(new FileReader(file)));
   }
   
   public JSONTokenizer(InputStream stream) throws UnsupportedEncodingException {
      this(new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset())));
   }
   
   public JSONTokenizer(InputStream stream, String charset) throws UnsupportedEncodingException {
      this(new BufferedReader(new InputStreamReader(stream, charset)));
   }
   
   public JSONTokenizer(InputStream stream, Charset charset) throws UnsupportedEncodingException {
      this(new BufferedReader(new InputStreamReader(stream, charset)));
   }
   
   public JSONTokenizer(BufferedReader reader) {
      this.reader = reader;
      token = EMPTY;
      sval = null;
      bval = false;
      nval = 0;
      gval = null;
      pos = -1;
      currentCode = EMPTY;
   }
   
   public int token() {
      return token;
   }
   
   public String lastString() {
      return sval;
   }
   
   public boolean lastBoolean() {
      return bval;
   }
   
   public double lastNumber() {
      return nval;
   }
   
   public String lastGarbage() {
      return gval;
   }
   
   private char getChar() {
      if (currentCode < 0) {
         throw new RuntimeException("EOF");
      }
      return (char)currentCode;
   }
   
   private int getCode() {
      return currentCode;
   }
   
   private boolean isEOF() {
      return currentCode == EOF;
   }
   
   private int nextCode() {
      try {
         currentCode = reader.read();
         ++pos;
      } catch (IOException e) {
         e.printStackTrace();
         currentCode = EOF;
      }
      return currentCode;
   }
   
   private int getHex(int ch) {
      if (ch >= '0' && ch <= '9') {
         return ch-'0';
      } else if (ch >= 'a' && ch <= 'f') {
         return ch+10-'a';
      } else if (ch >= 'A' && ch <= 'F') {
         return ch+10-'A';
      }
      return -1;
   }
   
   private String parseString() {
      // current character should be a quote, continue to next quote
      StringBuilder sb = new StringBuilder();
      if (nextCode() < 0) {
         return sb.toString(); // empty
      }
      
      int nc = getCode();
      while (nc >= 0 && nc != '"') {
         if (nc == '\\') {
            // escaped character
            nc = nextCode();
            if (nc < 0) {
               sb.append('\\');
               break;
            }
            switch (nc) {
               case '"':
                  sb.append('"');
                  break;
               case '\\':
                  sb.append('\\');
                  break;
               case '/':
                  sb.append('/');
                  break;
               case 'b':
                  sb.append('\b');
                  break;
               case 'f':
                  sb.append('\f');
                  break;
               case 'n':
                  sb.append('\n');
                  break;
               case 'r':
                  sb.append('\r');
                  break;
               case 't':
                  sb.append('\t');
                  break;
               case 'u':
                  // unicode character, 4 hex digits
                  StringBuilder backup = new StringBuilder("\\u");
                  int uc = 0;
                  boolean valid = true;
                  for (int i=0; i<4; ++i) {
                     nc = nextCode();
                     if (nc >= 0) {
                        backup.append((char)nc);
                     }
                     int h = getHex(nc);
                     if (h >= 0) {
                        uc = (uc << 4) | h;
                     } else {
                        valid = false;
                     }
                  }
                  if (valid) {
                     sb.append((char)uc);
                  } else {
                     sb.append(backup);
                  }
                  break;
            }
         } else {
            sb.append((char)nc);
         }
         nc = nextCode(); // advance
      }
      
      // skip over ending quote
      if (nc >= 0) {
         nextCode();
      }
      
      return sb.toString();
   }
   
   private double parseNumber() {
      
      // rely on Java's parsing
      StringBuilder sb = new StringBuilder();
      
      char c = getChar();
      sb.append(c);
      int nc = nextCode();
      // if (d > 0) { // allow preceding zeroes
         while (nc >= '0' && nc <= '9') {
            sb.append((char)nc);
            nc = nextCode();
         }
      // }
         
      if (nc == '.') {
         sb.append((char)nc);
         // fractional part
         nc = nextCode();
         while (nc >= '0' && nc <= '9') {
            sb.append((char)nc);
            nc = nextCode();
         }
      }
      if (nc == 'e' || nc == 'E') {
         sb.append((char)nc);
         nc = nextCode();
         if (nc == '+') {
            nc = nextCode();
         }
         if (nc == '-') {
            sb.append((char)nc);
            nc = nextCode();
         }
         while (nc >= '0' && nc <= '9') {
            sb.append((char)nc);
            nc = nextCode();
         }
      }
      double v = Double.parseDouble(sb.toString());
      
      return v;
   }
   
   private void parseGarbageLine(StringBuilder sb) {
      // to end of line or file
      int c;
      if (isEOF()) {
         return;
      } else {
         c = getChar();
      }
      while (c != '\n' && c != '\r') {
         sb.append(c);
         int nc = nextCode();
         if (nc < 0) {
            return;
         }
         c = (char)nc;
      }
      
      // eat remaining newlines 
      while (c == '\n' || c == '\r') {
         int nc = nextCode();
         if (nc < 0) {
            break;
         }
         c = (char)nc;
      }
   }
   
   private String parseGarbageWord(StringBuilder sb) {
      // to end of line or file
      char c;
      if (isEOF()) {
         return null;
      } else {
         c = getChar();
      }
      while (!isWhitespace(c) && c != COLON && c != COMMA && c != OBJECT_START && c != OBJECT_END
         && c != ARRAY_START && c != ARRAY_END) {
         sb.append(c);
         int nc = nextCode();
         if (nc < 0) {
            break;
         }
         c = (char)nc;
      }
      return sb.toString();
   }
   
   private String  parseGarbageInlineComment(StringBuilder sb) {
      char c1 = 0;
      char c2;
      if (isEOF()) {
         return null;
      } else {
         c2 = getChar();
      }
      while (c1 != '*' || c2 != '/') {
         sb.append(c2);
         int nc = nextCode();
         if (nc < 0) {
            return sb.toString();
         }
         c1 = c2;
         c2 = (char)nc;
      }
      // append final slash
      sb.append(c2);
      nextCode();
      return sb.toString();
   }
   
   // makes some assumptions about garbage
   // i.e. if starts with // or #, then ends on a line feed or carriage return
   // if starts with a /* then ends on a */
   // otherwise, ends on whitespace or colon or comma or start/end of object or array
   private String parseGarbage() {
      
      if (isEOF()) {
         return null;
      }
      
      StringBuilder sb = new StringBuilder();
      char c = getChar();
      
      if (c == '/') {
         int nc = nextCode();
         if (nc < 0) {
            return Character.toString(c);
         }
         
         char c2 = (char)nc;
         sb.append(c);
         if (c2 == '/') {
            sb.append(c2);
            nextCode();
            parseGarbageLine(sb);
         } else if (c2 == '*') {
            sb.append(c2);
            nextCode();
            parseGarbageInlineComment(sb);
         } else {
            // to white space or token
            parseGarbageWord(sb);
         }
      } else if (c == '#') { 
         // to end of line or file
         parseGarbageLine(sb);
      } else {
         // to white space or token or end of file
         parseGarbageWord(sb);
      }
      
      return sb.toString();
   }
   
   private boolean isWhitespace(char b) {
      if (b == ' ' || b == '\t' || b == '\n' || b == '\r' || b == '\b' || b == '\f') {
         return true;
      }
      return false;
   }
   
   public int next() {
      
      // process characters
      
      // initial character
      if (currentCode == EMPTY) {
         nextCode();
      }
      
      if (isEOF()) {
         token = EOF;
         return token;
      }
      
      // skip white-space
      char c = getChar();
      while (isWhitespace(c)) {
         int nc = nextCode();
         if (nc < 0) {
            token = EOF;
            return token;
         }
         c = (char)nc;
      }
      
      // look for { , [, ", number, true, false, null
      switch (c) {
         case ':':
            token = COLON;
            nextCode();
            break;
         case ',':
            token = COMMA;
            nextCode();
            break;
         case '{':
            token = OBJECT_START;
            nextCode();
            break;
         case '}':
            token = OBJECT_END;
            nextCode();
            break;
         case '[':
            token = ARRAY_START;
            nextCode();
            break;
         case ']':
            token = ARRAY_END;
            nextCode();
            break;
         case '"': {
            // parse string
            String str = parseString();
            token = STRING;
            sval = str;
            break;
         }
         case 't': {
            // parse true
            final String TRUE = "true";
            StringBuilder sb = new StringBuilder(TRUE.length());
            int cc = c;
            for (int i=0; i<TRUE.length() && cc == TRUE.charAt(i); ++i) {
               sb.append((char)cc);
               cc = nextCode();
            }
            if (sb.length() == TRUE.length()) {
               token = BOOLEAN;
               bval = true;
            } else {
               token = GARBAGE;
               gval = parseGarbageWord(sb);
            }
            break;
         }
         case 'f': {
            // parse false
            final String FALSE = "false";
            StringBuilder sb = new StringBuilder(FALSE.length());
            int cc = c;
            for (int i=0; i<FALSE.length() && cc == FALSE.charAt(i); ++i) {
               sb.append((char)cc);
               cc = nextCode();
            }
            if (sb.length() == FALSE.length()) {
               token = BOOLEAN;
               bval = false;
            } else {
               token = GARBAGE;
               gval = parseGarbageWord(sb);
            }
            break;
         }
         case 'n': {
            // parse null
            final String NULL_STR= "null";
            StringBuilder sb = new StringBuilder(NULL_STR.length());
            int cc = c;
            for (int i=0; i<NULL_STR.length() && cc == NULL_STR.charAt(i); ++i) {
               sb.append((char)cc);
               cc = nextCode();
            }
            if (sb.length() == NULL_STR.length()) {
               token = NULL;
            } else {
               token = GARBAGE;
               gval = parseGarbageWord(sb);
            }
            break;
         }
         default: {
            // number?
            if (c >= '0' && c <= '9') {
               nval = parseNumber();
               token = NUMBER;
            } else if (c == '-' || c == '+') {
               // check next byte is a digit
               if (nextCode() >= 0) {
                  char d = getChar();
                  if ( d >= '0' && d <= '9') {
                     nval = parseNumber();
                     token = NUMBER;
                     if (c == '-') {
                        nval = -nval;
                     }
                  } else {
                     StringBuilder sb = new StringBuilder();
                     sb.append(c);
                     token = GARBAGE;
                     gval = parseGarbageWord(sb);
                  }
               } else {
                  gval = Character.toString(c);
                  token = GARBAGE;
               }
               
            } else {
               gval = parseGarbage();
               token = GARBAGE;
            }
         }
      }
       
      return token;
   }
   
   public long getPosition() {
      return pos;
   }
   
   public int getCharacter() {
      return currentCode;
   }
   
   public void close() {
      if (reader != null) {
         // close quietly
         try {
            reader.close();
         } catch (IOException e) {
            // e.printStackTrace();
         }
      }
   }
   
}
