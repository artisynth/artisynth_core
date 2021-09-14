package artisynth.core.driver;

import java.awt.*;

import java.awt.event.*;
import javax.swing.*;
import java.beans.*;
import java.util.Random;

import java.io.*;
import maspack.widgets.*;

/**
 * Creates and displays the progress panel for updating ArtiSynth libraries.
 */
public class UpdateLibrariesAgent implements ActionListener {

   JProgressBar myProgressBar;
   JTextArea myTextArea;
   JButton myDoneButton;
   UpdateTask myTask;
   JFrame myFrame;

   class UpdateTask extends SwingWorker<Void, Void> {
      int myRetval = 0;

      /*
       * Main task. Executed in background thread.
       */
      @Override
      public Void doInBackground() {
         PrintStream out = new PrintStream(new TextAreaOutputStream());
         PrintStream savedOut = System.out;
         PrintStream savedErr = System.err;
         System.setOut (out);
         System.setOut (out);
         try {
            myRetval = Launcher.verifyLibraries (/*update=*/true);
         }
         catch (Exception e) {
            // just in case
         }
         finally {
            System.setOut (savedOut);
            System.setOut (savedErr);
         }
         return null;
      }
 
      /*
       * Executed in event dispatching thread
       */
      @Override
      public void done() {
         if (isCancelled()) {
            myFrame.dispose();
         }
         else {
            myProgressBar.setIndeterminate (false);
            myProgressBar.setValue (100);
            if (myRetval == -1) {
               myTextArea.append("\nError encounted\n");
            }
            else if (myRetval == 0) {
               myTextArea.append("\nAll libraries up to date\n");
            }
            else {
               if (myRetval == 1) {
                  myTextArea.append("\nDownloaded 1 library. ");
               }
               else {
                  myTextArea.append("\nDownloaded "+myRetval+" libraries. ");
               }
               myTextArea.append ("You may want to restart ArtiSynth.\n");
            }
            myDoneButton.setText ("OK");
            myDoneButton.setActionCommand ("Done");
         }
      }
    }
 
   class TextAreaOutputStream extends OutputStream {
      /**
       * TextAreaOutputStream which writes to the output text area.
       * 
       * Courtesy of EF5 at Stackoverflow
       */
      public void write (int b) throws IOException {
         myTextArea.append (String.valueOf ((char)b));
         myTextArea.setCaretPosition(myTextArea.getDocument().getLength());
      }
      
      public void write (char[] cbuf, int off, int len) throws IOException {
         myTextArea.append (new String(cbuf, off, len));
         myTextArea.setCaretPosition (myTextArea.getDocument().getLength());
      }
   }

   void createAndShowPanel (Component refcomp) {
      // create the window
      myFrame = new JFrame("Updating libraries");
      myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
      // set up the content pane
      JPanel mainPanel = new JPanel();
      mainPanel.setLayout (new BorderLayout());

      myProgressBar = new JProgressBar();
      myProgressBar.setStringPainted(false);
      myProgressBar.setIndeterminate (true);

      myTextArea = new JTextArea(10, 80);
      myTextArea.setMargin (new Insets(5,5,5,5));
      myTextArea.setEditable(false);
      myTextArea.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));

      JPanel topPanel = new JPanel();
      topPanel.add (myProgressBar);
      mainPanel.add (topPanel, BorderLayout.PAGE_START);

      myDoneButton = new JButton ("Cancel");
      myDoneButton.setActionCommand ("Cancel");
      myDoneButton.addActionListener (this);
      JPanel bottomPanel = new JPanel();
      bottomPanel.add (myDoneButton);
      mainPanel.add (bottomPanel, BorderLayout.PAGE_END);

      mainPanel.add (
         new JScrollPane(myTextArea), BorderLayout.CENTER);
      mainPanel.setBorder(
         BorderFactory.createEmptyBorder(20, 20, 20, 20));

      myFrame.setContentPane (mainPanel);

      // display the window.
      myFrame.pack();
      myFrame.setVisible(true);
      GuiUtils.locateCenter (myFrame, refcomp);

      myTask = new UpdateTask();
      myTask.execute();
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Cancel") && myTask != null) {
         myTask.cancel (/*mayinterrupt=*/true);
      }
      else if (cmd.equals ("Done")) {
         myFrame.dispose();
      }
   }

}
