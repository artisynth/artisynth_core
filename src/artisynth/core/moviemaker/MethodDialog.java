package artisynth.core.moviemaker;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.*;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.*;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import maspack.render.GL.FrameBufferObject;
import maspack.util.IntegerInterval;
import maspack.util.InternalErrorException;
import maspack.widgets.DoubleField;
import maspack.widgets.FileNameField;
import maspack.widgets.GuiUtils;
import maspack.widgets.IntegerField;
import maspack.widgets.LabeledComponentPanel;
import maspack.widgets.OptionPanel;
import maspack.widgets.StringField;
import maspack.widgets.StringSelector;
import maspack.widgets.EnumSelector;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.driver.Main;
import artisynth.core.driver.MainFrame;
import artisynth.core.driver.Scheduler;
import artisynth.core.driver.ViewerManager;
import artisynth.core.modelbase.HasAudio;
import artisynth.core.moviemaker.MovieMaker.MethodInfo;
import artisynth.core.moviemaker.MovieMaker.Method;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.ConvertRawToWav;
import artisynth.core.workspace.RootModel;

/**
 * Dialog which allows the command to be set for a particular movue
 * making method.
 */
public class MethodDialog extends JDialog implements ActionListener {
   private static final long serialVersionUID = 1L;
   private JTextField myTextField;
   private OptionPanel myOptionPanel;
   private MovieMaker.MethodInfo myMethod;

   public MethodDialog (Window owner, MovieMaker.MethodInfo methodInfo) {
      /* explicit Dialog cast for compatibility with Java 1.5 */
      super (owner);
      myTextField = new JTextField();
      JPanel panel = new JPanel();
      panel.setLayout (new BoxLayout (panel, BoxLayout.Y_AXIS));
      setContentPane (panel);

      panel.setBorder (new EmptyBorder (4, 4, 4, 4));
      myTextField.setText (methodInfo.command);
      Dimension size = myTextField.getPreferredSize();
      size.width = 10000;
      myTextField.setMaximumSize (size);
      myOptionPanel = new OptionPanel ("OK Cancel", this);
      myMethod = methodInfo;
      panel.add (myTextField);
      panel.add (Box.createRigidArea (new Dimension (0,4)));
      System.out.println (myTextField.getPreferredSize());
      panel.add (myOptionPanel);
      setModal (true);
      pack();
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("OK")) {
         myMethod.command = myTextField.getText();
      }
      else if (cmd.equals ("Cancel")) {
      }
      dispose();
   }
}
