package artisynth.core.gui.widgets;

import java.awt.*;
import javax.swing.*;

import maspack.widgets.*;

/**
 * Frame that displays an indeterminant progress bar
 */
public class ProgressFrame extends JFrame {

   private JProgressBar myProgressBar;

   public ProgressFrame (String title) {
      super ();
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
 
      // set up the content pane
      JPanel mainPanel = new JPanel();
      mainPanel.setLayout (new BorderLayout());

      myProgressBar = new JProgressBar();
      myProgressBar.setIndeterminate (true);
      JPanel centerPanel = new JPanel();
      centerPanel.add (myProgressBar);

      JLabel label = new JLabel (title);
      JPanel topPanel = new JPanel();
      topPanel.add (label);

      mainPanel.add (topPanel, BorderLayout.PAGE_START);
      mainPanel.add (centerPanel, BorderLayout.CENTER);
      mainPanel.setBorder(
         BorderFactory.createEmptyBorder(20, 20, 20, 20));

      setContentPane (mainPanel);

      pack();
   }

   public JProgressBar getProgressBar() {
      return myProgressBar;
   }

   public static void main (String[] args) {
      JFrame frame = new JFrame();

      JPanel mainPanel = new JPanel();
      mainPanel.setLayout (new BorderLayout());

      JPanel centerPanel = new JPanel();
      centerPanel.setPreferredSize (new Dimension (300, 300));
      mainPanel.add (centerPanel, BorderLayout.CENTER);

      frame.setContentPane (mainPanel);
      frame.pack();
      frame.setVisible (true);

      ProgressFrame pframe = new ProgressFrame ("testing progress");
      pframe.setVisible (true);
      GuiUtils.locateCenter (pframe, frame);

   }

}
