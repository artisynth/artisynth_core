package artisynth.core.gui.jythonconsole;

import javax.swing.*;
import javax.swing.text.*;
import java.util.*;
import java.awt.event.*;
import java.awt.*;

import maspack.util.*;

public class ReadlineConsole extends JTextArea {
   private static final long serialVersionUID = 255772702202415635L;

   String myPrompt = ">>> ";

   LinkedList<String> myHistory = new LinkedList<String>();
   int myMaxHistory = 500;
   int myMaxLines = 500;
   int myHistoryIdx = 0;
   PromptLengths myPromptLengths;

   /** 
    * A prompt length is associated with every line, to indicate that
    * part of the line (associated with the prompt) that cannot be editting.
    * Lines without prompts have a prompt length of 0.
    */
   protected class PromptLengths extends ArrayList<Integer> {
      private static final long serialVersionUID = -7271464798363177301L;

      PromptLengths (int size) {
         super (size);
      }

      public void removeRange (int from, int to) {
         super.removeRange (from, to);
      }
   }

   public ReadlineConsole() {
      super (12, 80);
      setEditable (true);

      myPromptLengths = new PromptLengths (64);
      initializeKeyMap();
      initializeFont();
      initializeNewLine (null);
      addComponentListener (new MyComponentListener());

   }

   public void setRows (int nrows) {
      super.setRows (nrows);
      removeExcessLines();
   }

   protected boolean isWordChar (char c) {
      return Character.isLetterOrDigit (c);
   }

   public String getPrompt() {
      return myPrompt;
   }

   public void setPrompt (String prompt) {
      myPrompt = prompt;
   }

   /** 
    * Can be overridden by subclasses to implement an interrupt function.
    */
   protected void interrupt() {
   }

   private static final int CTRL_A = 0x01;
   private static final int CTRL_B = 0x02;
   private static final int CTRL_C = 0x03;
   private static final int CTRL_D = 0x04;
   private static final int CTRL_E = 0x05;
   private static final int CTRL_F = 0x06;
   private static final int CTRL_G = 0x07;
   private static final int CTRL_H = 0x08;
   private static final int CTRL_I = 0x09;
   private static final int CTRL_J = 0x0A;
   private static final int CTRL_K = 0x0B;
   private static final int CTRL_L = 0x0C;
   private static final int CTRL_M = 0x0D;
   private static final int CTRL_N = 0x0E;
   private static final int CTRL_O = 0x0F;
   private static final int CTRL_P = 0x10;
   private static final int CTRL_Q = 0x11;
   private static final int CTRL_R = 0x12;
   private static final int CTRL_S = 0x13;
   private static final int CTRL_T = 0x14;
   private static final int CTRL_U = 0x15;
   private static final int CTRL_V = 0x16;
   private static final int CTRL_W = 0x17;
   private static final int CTRL_X = 0x18;
   private static final int CTRL_Y = 0x19;
   private static final int CTRL_Z = 0x1A;
   private static final int ESC = 0x1B;
   private static final int DEL = 0x7F;

   private static int NORMAL = 0;
   private static int QUOTING = 1;
   private static int ESCAPED = 2;

   String myKillBuffer;

   private void addHistory (String str) {
      // System.out.println ("adding '"+str+"'");
      myHistory.addFirst (str);
      if (myHistory.size() == myMaxHistory) {
         myHistory.removeLast();
      }
   }

   private void setHistory (int idx, String str) {
      // System.out.println ("setting '"+str+"'");
      myHistory.set (idx, str);
   }

   protected void removeExcessLines() {
      int linesOverLimit = getLineCount() - myMaxLines;

      try {
         if (linesOverLimit > 0) {
            doDelete (0, getLineStartOffset (linesOverLimit));
         }
      }
      catch (BadLocationException ble) {
         throw new InternalErrorException ("bad location");
      }
   }

   private void doInsert (String text, int pos) {
      try {
         if (text.length() > 0) {
            int lineno = getLineOfOffset (pos);
            for (int idx=0; idx < text.length(); idx++) {
               if (text.charAt(idx) == '\n') {
                  myPromptLengths.add (++lineno, 0);
               }
            }
            insert (text, pos);
         }
      }
      catch (Exception e) {
         throw new InternalErrorException ("bad location");
      }
   }

   private void doDelete (int start, int end) {
      try {
         int startLine = getLineOfOffset (start);
         int endLine = getLineOfOffset (end);
         replaceRange (null, start, end);
         if (endLine > startLine) {
            myPromptLengths.removeRange (startLine+1, endLine+1);
         }
      }
      catch (Exception e) {
         throw new InternalErrorException ("bad location");
      }
   }

   protected void appendText (String str) {
      if (str != null) {
         doInsert (str, getDocument().getLength());
         removeExcessLines();
      }      
      setCaretPosition (getDocument().getLength());
   }

   protected void initializeNewLine (String oldLine) {
      if (oldLine == null) {
         addHistory ("");
         myPromptLengths.add (0);
      }
      else if (oldLine.length() > 0) {
         setHistory (0, oldLine);
         addHistory ("");
      }
      myHistoryIdx = 0;
      try {
         int off = getCaretPosition();
         int lineno = getLineOfOffset (off);
         if (getLineStartOffset(lineno) != off) {
            // we are not at the start of a newline, so add at newline
            appendText ("\n");
            lineno++;
         }
         appendText (myPrompt);
         // set prefixe length for the start of this line
         myPromptLengths.set (lineno, myPrompt.length());
      }
      catch (BadLocationException e) {
         throw new InternalErrorException ("bad location");
      }
   }

   private void insertText (String text, int loc) {
      doInsert (text, loc);
      setCaretPosition (loc + text.length());
   }

   String getLineText (int startOff, int endOff) {
      try {
         return getText (startOff, endOff - startOff);
      }
      catch (BadLocationException e) {
         throw new InternalErrorException ("bad location");
      }
   }

   String getLineText (int off) {
      int begOff = getRowStart (off);
      int endOff = getRowEnd (off);
      try {
         return getText (begOff, endOff - begOff);
      }
      catch (BadLocationException e) {
         throw new InternalErrorException ("bad location");
      }
   }

   private int getRowStart (int off) {
      try {
         int lineno = getLineOfOffset (off);
         return getLineStartOffset (lineno) + myPromptLengths.get (lineno);
      }
      catch (BadLocationException ble) {
         throw new InternalErrorException ("bad location");
      }
   }

   private int getRowEnd (int off) {
      try {
         int lineno = getLineOfOffset (off);
         int endOff = getLineEndOffset (lineno);
         if (lineno < getLineCount() - 1) {
            endOff--;
         }
         return endOff;
      }
      catch (BadLocationException e) {
         throw new InternalErrorException ("bad location");
      }
   }

   protected void endOfLine (boolean select) {
      int endOffs = getRowEnd (getCaretPosition());
      if (select) {
         moveCaretPosition (endOffs);
      }
      else {
         setCaretPosition (endOffs);
      }
   }

   protected void beginningOfLine (boolean select) {
      int offs = getCaretPosition();
      int begOffs = getRowStart (offs);
      if (select) {
         moveCaretPosition (begOffs);
      }
      else {
         setCaretPosition (begOffs);
      }
   }

   protected void forwardChar (boolean select) {
      int offs = getCaretPosition();
      if (offs < getRowEnd (offs)) {
         if (select) {
            moveCaretPosition (offs + 1);
         }
         else {
            setCaretPosition (offs + 1);
         }
      }
   }

   protected void transposeChars() {
      int offs = getCaretPosition();
      int begOffs = getRowStart (offs);
      int endOffs = getRowEnd (offs);
      if (offs > begOffs) {
         String text;
         int off0, off1;
         if (offs < endOffs) {
            off0 = offs - 1;
            off1 = offs + 1;
         }
         else if (endOffs - begOffs >= 2) {
            off0 = offs - 2;
            off1 = offs;
         }
         else {
            off0 = off1 = -1;
         }
         if (off0 != -1) {
            text = getLineText (off0, off1);
            text = new String (new char[] { text.charAt (1), text.charAt (0) });
            replaceRange (text, off0, off1);
         }
      }
   }

   protected int nextWordEndOffset (int offs) {
      String line = getLineText (offs, getRowEnd (offs));
      int idx = 0;
      while (idx < line.length() && !isWordChar (line.charAt (idx))) {
         idx++;
      }
      while (idx < line.length() && isWordChar (line.charAt (idx))) {
         idx++;
      }
      return offs + idx;
   }

   protected void forwardWord (boolean select) {
      int offs = nextWordEndOffset (getCaretPosition());
      if (select) {
         moveCaretPosition (offs);
      }
      else {
         setCaretPosition (offs);
      }
   }

   protected void killWord() {
      int offs = getCaretPosition();
      int endOffs = nextWordEndOffset (offs);
      doDelete (offs, endOffs);
   }

   protected int prevWordStartOffset (int offs) {
      String line = getLineText (getRowStart (offs), offs);
      int idx = line.length() - 1;
      while (idx >= 0 && !isWordChar (line.charAt (idx))) {
         idx--;
      }
      while (idx >= 0 && isWordChar (line.charAt (idx))) {
         idx--;
      }
      return offs - (line.length() - 1 - idx);
   }

   protected void backwardWord (boolean select) {
      int offs = prevWordStartOffset (getCaretPosition());
      if (select) {
         moveCaretPosition (offs);
      }
      else {
         setCaretPosition (offs);
      }
   }

   protected void backwardKillWord() {
      int offs = getCaretPosition();
      int begOffs = prevWordStartOffset (offs);
      doDelete (begOffs, offs);
   }

   protected void clearScreen() {
      doDelete (0, getRowStart (getCaretPosition()) - myPrompt.length());
   }

   protected void backwardChar (boolean select) {
      int offs = getCaretPosition();
      if (offs > getRowStart (offs)) {
         if (select) {
            moveCaretPosition (offs - 1);
         }
         else {
            setCaretPosition (offs - 1);
         }
      }
   }

   protected void deleteChar() {
      int offs = getCaretPosition();
      if (offs < getRowEnd (offs)) {
         doDelete (offs, offs + 1);
      }
   }

   protected void backwardDeleteChar() {
      int offs = getCaretPosition();
      if (offs > getRowStart (offs)) {
         doDelete (offs - 1, offs);
      }
   }

   protected void killLine() {
      int offs = getCaretPosition();
      int endOffs = getRowEnd (offs);
      if (offs < endOffs) {
         try {
            myKillBuffer = getText (offs, endOffs - offs);
            doDelete (offs, endOffs);
         }
         catch (BadLocationException ble) {
            throw new InternalErrorException ("bad location");
         }
      }
   }

   protected void yank() {
      if (myKillBuffer != null) {
         insertText (myKillBuffer, getCaretPosition());
      }
   }

   protected void backwardKillLine() {
      int begOffs = getRowStart (getCaretPosition());
      int offs = getCaretPosition();
      if (begOffs < offs) {
         try {
            myKillBuffer = getText (begOffs, offs - begOffs);
            doDelete (begOffs, offs);
         }
         catch (BadLocationException ble) {
            throw new InternalErrorException ("bad location");
         }
      }
   }

   private void replaceLine (String str, int off) {
      int begOffs = getRowStart (off);
      int endOffs = getRowEnd (off);
      doDelete (begOffs, endOffs);
      insertText (str, begOffs);
   }

   protected void previousHistory() {
      if (myHistoryIdx < myHistory.size() - 1) {
         int dot = getCaretPosition();
         setHistory (myHistoryIdx, getLineText (dot));
         replaceLine (myHistory.get (++myHistoryIdx), dot);
      }
   }

   protected void beginningOfHistory() {
      if (myHistoryIdx < myHistory.size() - 1) {
         int dot = getCaretPosition();
         setHistory (myHistoryIdx, getLineText (dot));
         myHistoryIdx = myHistory.size() - 1;
         replaceLine (myHistory.get (myHistoryIdx), dot);
      }
   }

   protected void nextHistory() {
      if (myHistoryIdx > 0) {
         int dot = getCaretPosition();
         setHistory (myHistoryIdx, getLineText (dot));
         replaceLine (myHistory.get (--myHistoryIdx), dot);
      }
   }

   protected void endOfHistory() {
      if (myHistoryIdx > 0) {
         int dot = getCaretPosition();
         setHistory (myHistoryIdx, getLineText (dot));
         myHistoryIdx = 0;
         replaceLine (myHistory.get (myHistoryIdx), dot);
      }
   }

   public String processLine (String str) {
      return null;
   }

   // we synchronize acceptLine so that we can ensure that scripts
   // don't start running while we are still in this method
   protected synchronized void acceptLine() {
      String line = getLineText (getCaretPosition());
      appendText ("\n");
      String result = processLine (line);
      appendText (result);
      initializeNewLine (line);
   }

   private void initializeFont() {
      setFont (new Font ("Monospaced", Font.PLAIN, /* size= */14));
   }

   protected void selfInsert (char c) {
      doInsert (Character.toString (c), getCaretPosition());
//       try {
//          getDocument().insertString (
//             getCaretPosition(), Character.toString (c), null);
//       }
//       catch (BadLocationException ble) {
//          throw new InternalErrorException ("bad location");
//       }
   }

   protected int metaState = NORMAL;
   protected boolean selecting = false;

   public class MyComponentListener extends ComponentAdapter {
      public void componentResized (ComponentEvent e) {
         Dimension size = getSize();
         setRows (size.height / getRowHeight());
      }
   }

   protected void adjustCaretIfNecessary() {
      int off = getCaretPosition();
      int begOff = getRowStart (off);
      if (off < begOff) {
         setCaretPosition (begOff);
      }
   }

   public class MyKeyListener extends KeyAdapter {

      public void keyPressed (KeyEvent evt) {
         adjustCaretIfNecessary();

         int code = evt.getKeyCode();
         if (metaState == NORMAL) {
            switch (code) {
               case KeyEvent.VK_INSERT: {
                  yank();
                  break;
               }
               case KeyEvent.VK_RIGHT: {
                  forwardChar (selecting);
                  break;
               }
               case KeyEvent.VK_LEFT: {
                  backwardChar (selecting);
                  break;
               }
               case KeyEvent.VK_UP: {
                  previousHistory();
                  break;
               }
               case KeyEvent.VK_DOWN: {
                  nextHistory();
                  break;
               }
               case KeyEvent.VK_HOME: {
                  beginningOfLine (selecting);
                  break;
               }
               case KeyEvent.VK_END: {
                  endOfLine (selecting);
                  break;
               }
            }
         }
      }

      public void keyTyped (KeyEvent evt) {
         char ch = evt.getKeyChar();

         if (metaState == NORMAL) {
            switch (ch) {
               case CTRL_A: {
                  beginningOfLine (selecting);
                  break;
               }
               case CTRL_B: {
                  backwardChar (selecting);
                  break;
               }
               case CTRL_C: {
                  interrupt ();
                  break;
               }
               case CTRL_D: {
                  deleteChar();
                  break;
               }
               case CTRL_E: {
                  endOfLine (selecting);
                  break;
               }
               case CTRL_F: {
                  forwardChar (selecting);
                  break;
               }
               case CTRL_H: {
                  backwardDeleteChar();
                  break;
               }
               case CTRL_K: {
                  killLine();
                  break;
               }
               case CTRL_L: {
                  clearScreen();
                  break;
               }
               case CTRL_J:
               case CTRL_M: {
                  acceptLine();
                  break;
               }
               case CTRL_N: {
                  nextHistory();
                  break;
               }
               case CTRL_O: {
                  break;
               }
               case CTRL_P: {
                  previousHistory();
                  break;
               }
               case CTRL_Q: {
                  metaState = QUOTING;
                  break;
               }
               case CTRL_T: {
                  transposeChars();
                  break;
               }
               case CTRL_U: {
                  backwardKillLine();
                  break;
               }
               case CTRL_Y: {
                  yank();
                  break;
               }
               case DEL: {
                  deleteChar();
                  break;
               }
               case ESC: {
                  metaState = ESCAPED;
                  break;
               }
               default: {
                  if (ch >= 0x20) {
                     selfInsert (ch);
                  }
               }
            }
         }
         else if (metaState == QUOTING) {
            if (ch >= 0x20) {
               selfInsert (ch);
            }
            metaState = NORMAL;
         }
         else if (metaState == ESCAPED) {
            switch (ch) {
               case 'B':
               case 'b': {
                  backwardWord (selecting);
                  break;
               }
               case 'D':
               case 'd': {
                  killWord();
                  break;
               }
               case 'F':
               case 'f': {
                  forwardWord (selecting);
                  break;
               }
               case '>': {
                  endOfHistory();
                  break;
               }
               case '<': {
                  beginningOfHistory();
                  break;
               }
               case DEL: {
                  backwardKillWord();
                  break;
               }
               default: {
               }
            }
            metaState = NORMAL;
         }
         else {
            throw new InternalErrorException ("unknown meta state " + metaState);
         }
         evt.consume();
      }
   }

   private void initializeKeyMap() {
      addKeyListener (new MyKeyListener());

      InputMap map = getInputMap();
      KeyStroke[] keys = map.allKeys();
      for (int i = 0; i < keys.length; i++) { // System.out.println (" " +
                                                // keys[i] + " " +
                                                // map.get(keys[i]));
         // disable this key binding
         map.put (keys[i], "none");
      }
      // ActionMap amap = getActionMap();
      // Object[] akeys = amap.allKeys();
      // for (int i=0; i<akeys.length; i++)
      // { System.out.println (
      // " " + akeys[i] + " " + amap.get(akeys[i]));

      // }
   }

   public static void main (String[] args) {
      JFrame frame = new JFrame ("console");

      ReadlineConsole console = new ReadlineConsole();
      console.setLineWrap (true);
      JScrollPane pane = new JScrollPane(console);
      pane.setVerticalScrollBarPolicy (
         ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
      pane.setHorizontalScrollBarPolicy (
         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      frame.getContentPane().add (pane);
      frame.pack();
      frame.setVisible (true);
   }
}
