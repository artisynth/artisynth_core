package artisynth.core.gui.widgets;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import maspack.widgets.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.interpolation.*;

/**
 * Dialog used to data smoothing parameters and initiate smoothing.
 */
public class SmoothingDialog extends PropertyDialog {

   EnumSelector myMethodWidget;
   IntegerField myWinSizeWidget;
   IntegerField myDegreeWidget;
   ArrayList<ActionListener> myApplyListeners = new ArrayList<>();

   JButton myApplyButton;

   public SmoothingDialog (String title) {
      super (title, new PropertyPanel(), /*options=*/null);
      myMethodWidget = new EnumSelector ("method", SmoothingMethod.values());
      myMethodWidget.addValueChangeListener (
         new ValueChangeListener() {
            public void valueChange (ValueChangeEvent e) {
               if (getMethod() == SmoothingMethod.SAVITZKY_GOLAY) {
                  myDegreeWidget.setEnabledAll (true);
                  int deg = getPolynomialDegree();
                  if (getWindowSize() < deg+1) {
                     setWindowSize (deg+1);
                  }
               }
               else {
                  myDegreeWidget.setEnabledAll (false);
               }
            }
         });

      myWinSizeWidget = new IntegerField ("window size", 3);
      myWinSizeWidget.addValueCheckListener (
         new ValueCheckListener() {
           public Object validateValue (
               ValueChangeEvent e, StringHolder errMsg) {
               int wsize = ((Integer)e.getValue()).intValue();
               if (wsize < 3) {
                  wsize = 3;
               }
               if (getMethod() == SmoothingMethod.SAVITZKY_GOLAY) {
                  int deg = getPolynomialDegree();                  
                  if (wsize < deg+1) {
                     wsize = deg+1;
                  }
               }
               if (wsize%2 == 0) {
                  wsize++;
               }
               return PropertyUtils.validValue (wsize, errMsg);
            }
         });

      myDegreeWidget = new IntegerField ("polynomial degree", 2);
      myDegreeWidget.addValueCheckListener (
         new ValueCheckListener() {
           public Object validateValue (
               ValueChangeEvent e, StringHolder errMsg) {
               int deg = ((Integer)e.getValue()).intValue();
               if (deg < 2) {
                  deg = 2;
               }
               return PropertyUtils.validValue (deg, errMsg);
            }
         });
      myDegreeWidget.addValueChangeListener (
         new ValueChangeListener() {
           public void valueChange (ValueChangeEvent e) {
               int deg = ((Integer)e.getValue()).intValue();
               if (getWindowSize() < deg+1) {
                  setWindowSize (deg+1);
               }
            }
         });

      setDefaultCloseOperation (HIDE_ON_CLOSE);
         
      myDegreeWidget.setEnabledAll (false);
      myPanel.addWidget (myMethodWidget);
      myPanel.addWidget (myWinSizeWidget);
      myPanel.addWidget (myDegreeWidget);
      initOptionPanel();
      pack();
   }

    protected void initOptionPanel() {

      JSeparator sep = new JSeparator();
      sep.setAlignmentX (Component.CENTER_ALIGNMENT);
      getContentPane().add (GuiUtils.createBoxFiller());
      getContentPane().add (sep);

      JPanel optionPanel = new JPanel();
      optionPanel.setBorder (BorderFactory.createEmptyBorder (5, 5, 5, 5));
      optionPanel.setLayout (new BoxLayout(optionPanel, BoxLayout.X_AXIS));
      
      optionPanel.add (Box.createHorizontalGlue());
      myApplyButton = GuiUtils.addHorizontalButton (
         optionPanel, "Apply", this, "Apply smoothing");
      optionPanel.add (Box.createRigidArea (new Dimension (10, 10)));
      GuiUtils.addHorizontalButton (
         optionPanel, "Done", this, "Close dialog");
      optionPanel.add (Box.createHorizontalGlue());

      getContentPane().add (optionPanel);
      pack();
   }

    public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("Apply")) {
         fireApplyListeners();
      }
      else {
         super.actionPerformed (e);
      }
   }  

   public int getWindowSize() {
      return myWinSizeWidget.getIntValue();
   }
  
   public void setWindowSize(int w) {
      if (w%2 == 0) {
         w++;
      }
      myWinSizeWidget.maskValueChangeListeners (true);
      myWinSizeWidget.setValue (w);
      myWinSizeWidget.maskValueChangeListeners (false);
   }
 
   public SmoothingMethod getMethod() {
      return (SmoothingMethod)myMethodWidget.getValue();
   }
  
   public int getPolynomialDegree() {
      return myDegreeWidget.getIntValue();
   }
 
   public void setPolynomialDegree(int deg) {
      myDegreeWidget.maskValueChangeListeners (true);
      myDegreeWidget.setValue (deg);
      myDegreeWidget.maskValueChangeListeners (false);
   }
 
   protected void fireApplyListeners() {
      ActionEvent e = new ActionEvent (this, 0, "Apply");
      for (ActionListener l : myApplyListeners) {
         l.actionPerformed (e);
      }
   }

   public void addApplyListener (ActionListener l) {
      myApplyListeners.add (l);
   }

   public void removeApplyListener (ActionListener l) {
      myApplyListeners.remove (l);
   }

   public void removeAllApplyListeners() {
      myApplyListeners.clear();
   }

   public ActionListener[] getApplyListeners () {
      return myApplyListeners.toArray(new ActionListener[0]);
   }

   public void dispose() {
      myPanel.dispose();
      removeAllApplyListeners();
   }
}
