package artisynth.core.gui.jythonconsole;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.TextAction;

import maspack.util.InternalErrorException;

public class ReadlinePanel extends JTextArea {
   private static final long serialVersionUID = 255772702202415635L;

   String myPrompt = ">>> ";

   LinkedList<String> myHistory = new LinkedList<String>();
   int myMaxHistory = 500;
   int myMaxLines = 500;
   int myHistoryIdx = 0;
   int myLineStart;
   boolean myInputRequested = false;

   // I/O streams for connecting the console to an external thread
   PipedWriter myInputWriter = null;
   InterruptHandler myInterruptHandler = null;

   public static interface InterruptHandler {
      public void interrupt();
   }

   public static class OutputWriter extends Writer {

      StringBuilder builder = new StringBuilder();
      ReadlinePanel myConsole;

      public OutputWriter (ReadlinePanel console) {
         myConsole = console;
      }

      public void close() {
         flush();
      }

      public void flush() {
         if (builder.length() > 0) {
            myConsole.appendText (builder.toString());
            builder.setLength (0);
         }
      }

      public void write (char[] cbuf, int off, int len) {
         String str = new String (cbuf, off, len);
         for (int i=0; i<len; i++) {
            char c = cbuf[off+i];
            builder.append (c);
            if (c == '\n') {
               myConsole.appendText (builder.toString());
               builder.setLength (0);
            }
         }
      }
   }

   public void setInterruptHandler (InterruptHandler h) {
      myInterruptHandler = h;
   }

   public InterruptHandler getInterruptHandler() {
      return myInterruptHandler;
   }       

   /**
    * Create a reader for returning input from this console. Used
    * when the console is connected to an external thread.
    */   
   public BufferedReader createReader () {
      myInputWriter = new PipedWriter();
      try {
         return new BufferedReader (new PipedReader (myInputWriter));
      }
      catch (Exception e) {
         throw new InternalErrorException (
            "Can't create BufferedReader: " + e);
      }      
   }

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

   public ReadlinePanel() {
      super (12, 80);
      setEditable (true);

      initializeKeyMap();
      initializeFont();
      addHistory ("");
      addComponentListener (new MyComponentListener());
      addPopupMenu();
      //initializeNewLine();
   }
   
   private void addPopupMenu() {
      
      JPopupMenu menu = new JPopupMenu();
      
      Action copy = new DefaultEditorKit.CopyAction();
      copy.putValue(Action.NAME, "Copy");
      menu.add( copy );

      /**
       * Insert clipboard text into caret position
       */
      Action n = new TextAction("Paste") {
         private static final long serialVersionUID = 12452542145L;

         @Override
         public void actionPerformed (ActionEvent e) {
            try {
               String data = (String) Toolkit.getDefaultToolkit().
                  getSystemClipboard().getData(DataFlavor.stringFlavor);
               adjustCaretIfNecessary ();
               doInsert (data, getCaretPosition());
               
            }
            catch (HeadlessException | UnsupportedFlavorException
            | IOException e1) {
               e1.printStackTrace();
            } 
            
         }
      };
      menu.add (n);

      setComponentPopupMenu( menu );
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
      if (myInterruptHandler != null) {
         myInterruptHandler.interrupt();
      }
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
            insert (text, pos);
         }
      }
      catch (Exception e) {
         throw new InternalErrorException ("bad location");
      }
   }

   private void doDelete (int start, int end) {
      try {
         replaceRange (null, start, end);
         if (start < myLineStart) {
            if (end < myLineStart) {
               myLineStart -= end-start;
            }
            else {
               myLineStart = start;
            }
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
      myLineStart = getCaretPosition();
   }

   public void setLineText (String str) {
      replaceLine (str);
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

   String getLineText () {
      int begOff = getLineStart();
      int endOff = getLineEnd();
      try {
         return getText (begOff, endOff - begOff);
      }
      catch (BadLocationException e) {
         throw new InternalErrorException ("bad location");
      }
   }

   private int getLineStart () {
      return myLineStart;
   }

   private int getLineEnd () {
      try {
         return getLineEndOffset (getLineCount()-1);
      }
      catch (BadLocationException e) {
         throw new InternalErrorException ("bad location");
      }      
   }

   protected void endOfLine (boolean select) {
      int endOffs = getLineEnd();
      if (select) {
         moveCaretPosition (endOffs);
      }
      else {
         setCaretPosition (endOffs);
      }
   }

   protected void beginningOfLine (boolean select) {
      int offs = getCaretPosition();
      int begOffs = getLineStart();
      if (select) {
         moveCaretPosition (begOffs);
      }
      else {
         setCaretPosition (begOffs);
      }
   }

   protected void forwardChar (boolean select) {
      int offs = getCaretPosition();
      if (offs < getLineEnd()) {
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
      int begOffs = getLineStart();
      int endOffs = getLineEnd();
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
      String line = getLineText (offs, getLineEnd());
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
      String line = getLineText (getLineStart(), offs);
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
      
      doDelete (0, getLineStart());
   }

   protected void backwardChar (boolean select) {
      int offs = getCaretPosition();
      if (offs > getLineStart()) {
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
      if (offs < getLineEnd()) {
         doDelete (offs, offs + 1);
      }
   }

   protected void backwardDeleteChar() {
      int offs = getCaretPosition();
      if (offs > getLineStart()) {
         doDelete (offs - 1, offs);
      }
   }

   protected void killLine() {
      int offs = getCaretPosition();
      int endOffs = getLineEnd();
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
      int begOffs = getLineStart();
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

   private void replaceLine (String str) {
      int begOffs = getLineStart();
      int endOffs = getLineEnd();
      doDelete (begOffs, endOffs);
      insertText (str, begOffs);
   }

   protected void previousHistory() {
      if (myHistoryIdx < myHistory.size() - 1) {
         setHistory (myHistoryIdx, getLineText());
         replaceLine (myHistory.get (++myHistoryIdx));
      }
   }

   protected void beginningOfHistory() {
      if (myHistoryIdx < myHistory.size() - 1) {
         setHistory (myHistoryIdx, getLineText());
         myHistoryIdx = myHistory.size() - 1;
         replaceLine (myHistory.get (myHistoryIdx));
      }
   }

   protected void nextHistory() {
      if (myHistoryIdx > 0) {
         setHistory (myHistoryIdx, getLineText());
         replaceLine (myHistory.get (--myHistoryIdx));
      }
   }

   protected void endOfHistory() {
      if (myHistoryIdx > 0) {
         setHistory (myHistoryIdx, getLineText());
         myHistoryIdx = 0;
         replaceLine (myHistory.get (myHistoryIdx));
      }
   }

   public void handleInput (String line) {
      String result = processInput (line);
      if (result != null && !result.endsWith ("\n")) {
         result = result + "\n";
      }
      appendText (result);
      requestInput (myPrompt);
   }

   public String processInput (String str) {
      return null;
   }

   public void requestInput (String prompt) {
      appendText (prompt);
      myInputRequested = true;
   }      

   public void requestInput () {
      requestInput (myPrompt);
   }      

   // we synchronize acceptLine so that we can ensure that scripts
   // don't start running while we are still in this method
   protected synchronized void acceptLine() {
      String line = getLineText();
      appendText ("\n");
      setHistory (0, line);
      addHistory ("");
      myHistoryIdx = 0;
      myInputRequested = false;

      if (myInputWriter == null) {
         String result = processInput (line);
         if (result != null && !result.endsWith ("\n")) {
            result = result + "\n";
         }
         appendText (result);
         requestInput (myPrompt);        
      }
      else {
         try {
            myInputWriter.write (line + "\n");
            myInputWriter.flush();
         }
         catch (Exception e) {
            throw new InternalErrorException (
               "Error writing to commandWriter: " + e);
         }
      }
   }

   private void initializeFont() {
      setFont (new Font ("Monospaced", Font.PLAIN, /* size= */14));
   }

   protected void selfInsert (char c) {
      doInsert (Character.toString (c), getCaretPosition());
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
      try {
         if (getLineCount()-1 != getLineOfOffset(off)) {
            setCaretPosition (getLineEndOffset (getLineCount()-1));
         }
         else {
            int begOff = getLineStart();
            if (off < begOff) {
               setCaretPosition (begOff);
            }
         }
      }
      catch (Exception e) {
         System.out.println (e);
      }
   }

   public class MyKeyListener extends KeyAdapter {

      public void keyPressed (KeyEvent evt) {
         if (!myInputRequested) {
            return;
         }
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
            if (!myInputRequested) {
               if (ch == CTRL_C) {
                  interrupt ();
               }
               return;
            }
         
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
                  //interrupt ();
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
            if (!myInputRequested) {
               return;
            }
            if (ch >= 0x20) {
               selfInsert (ch);
            }
            metaState = NORMAL;
         }
         else if (metaState == ESCAPED) {
            if (!myInputRequested) {
               return;
            }
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
      for (int i = 0; i < keys.length; i++) {
         // disable this key binding
         map.put (keys[i], "none");
      }
   }

   public static void main (String[] args) {
      JFrame frame = new JFrame ("console");

      ReadlinePanel console = new ReadlinePanel();
      console.setLineWrap (true);
      console.requestInput ();

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
