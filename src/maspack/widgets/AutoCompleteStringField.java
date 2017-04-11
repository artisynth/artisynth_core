/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Auto-completing version of the StringField
 * Tab to make a suggestion, continue tabbing to cycle through suggestions
 * or use Up/Down keys
 * @author antonio
 *
 */
public class AutoCompleteStringField extends StringField {

   private static final long serialVersionUID = 1L;
   private ArrayList<String> data;
   private ArrayList<String> suggestions;

   private int suggestionIdx = -1;	// so that starts at 1
   private String lastSearch = "";
   private static int myCompletionKey = KeyEvent.VK_TAB;
   private static int myNextCompletionKey = KeyEvent.VK_DOWN;
   private static int myPreviousCompletionKey = KeyEvent.VK_UP;

   /**
    * Creates a StringField with an empty label text and a default number of
    * columns.
    */
   public AutoCompleteStringField() {
      this ("", 20);
   }

   /**
    * Creates a StringField with specified label text.
    * 
    * @param labelText
    * text for the control label
    * @param ncols
    * approximate width of the text field in columns
    */
   public AutoCompleteStringField (String labelText, int ncols) {
      this(labelText,"", ncols);
   }

   /**
    * Creates a StringField with specified label text and initial value.
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the string
    * @param ncols
    * approximate width of the text field in columns
    */
   public AutoCompleteStringField (String labelText, String initialValue, int ncols) {
      this (labelText, initialValue, ncols, new ArrayList<String>());
   }

   
   /**
    * Creates an ACStringField with specified label text, initial value,
    * 		and list of suggested words
    * 
    * @param labelText
    * text for the control label
    * @param initialValue
    * initial value for the string
    * @param ncols
    * approximate width of the text field in columns
    * @param list
    * values to search for auto-complete functionality
    */
   public AutoCompleteStringField (String labelText, String initialValue, int ncols, ArrayList<String> list) {
      super(labelText, initialValue, ncols);
      setDataList(list);
      suggestions = new ArrayList<String>(data);	// copy suggestions over for now

      // check if have to disable focus traversal 
      updateFocusTraversalSetting();
      
      // register auto-completion
      myTextField.addKeyListener(new KeyListener() {

	 public void keyTyped(KeyEvent e) {}
	 public void keyReleased(KeyEvent e) {}
	 public void keyPressed(KeyEvent e) {    
	    
	    int caretPos =myTextField.getCaretPosition();

	    // get either up to caret, or entire text box, and update suggestions
	    String txt = "";
	    try {
	       txt = myTextField.getText(0,caretPos);
	       updateSuggestions(txt);
	    } catch (Exception err) {
	       err.printStackTrace();
	       txt = myTextField.getText();
	    }
	    
	    // if no suggestions, exit
	    if (suggestions.size()==0)
	       return;
	       
	    // autocomplete via tab key
	    if (e.getKeyCode() == myCompletionKey) {
	       
	       // if current item is accepted and matches the current suggestion
	       // transfer focus to next control (load)
	       if (!myTextBackgroundReversedP && (suggestionIdx >= 0 ) && (suggestions.size() > 0)){
		  if (suggestions.get(suggestionIdx).equals(myTextField.getText())) {
		     myTextField.transferFocus();
		     return;
		  }
	       } 
	       
	       // check prefixes
	       String pre = getPrefix(suggestions);

	       // if we can advance without missing suggestions, do so
	       if (pre.length() > txt.length()) {

		  // manually change text and fire action event
		  myTextField.setText(pre);
		  //fireUpdate();
		  
		// if we're at the point where it could be one of several, start cycling through suggestions
	       } else if (pre.equals(txt)) {

		  // shift to next suggestion
		  suggestionIdx++;
		  suggestionIdx %= suggestions.size();

		  // manually set text 
		  myTextField.setText(suggestions.get(suggestionIdx));
		  myTextField.setCaretPosition(caretPos);		// reset caret position

	       // otherwise, our suggestions are not up to date, and we need to force an update (or there are no suggestions)
	       } else {
		  suggestions = getSuggestions(txt, data);
		  lastSearch = txt;
		  
	       } // end comparing prefix to current text (txt)

	    // down key to move forward in suggestions list
	    } else if (e.getKeyCode() == myNextCompletionKey) {
	       suggestionIdx++;
	       suggestionIdx %= suggestions.size();
	       
	       // manually set text 
	       myTextField.setText(suggestions.get(suggestionIdx));
	       myTextField.setCaretPosition(caretPos);		// reset caret position
		  
	    // up key move up in suggestions list
	    } else if (e.getKeyCode() == myPreviousCompletionKey) {
	       
	       suggestionIdx--;
	       if (suggestionIdx >= suggestions.size()) {
		  suggestionIdx = 0;
	       } else if (suggestionIdx < 0) {
		  suggestionIdx = suggestions.size()-1;
	       }
	       
	       // manually set 
	       myTextField.setText(suggestions.get(suggestionIdx));
	       myTextField.setCaretPosition(caretPos);		// reset caret position
	    }
	 }
      });
   }

   private void updateFocusTraversalSetting() {
      if ( (myCompletionKey == KeyEvent.VK_TAB) || (myNextCompletionKey==KeyEvent.VK_TAB)
	    	|| (myPreviousCompletionKey==KeyEvent.VK_TAB)) {
	 myTextField.setFocusTraversalKeysEnabled(false);
      } else {
	 myTextField.setFocusTraversalKeysEnabled(true);
      }
   }
   

   /**
    * Sets the list of words from which to autocomplete
    * 
    * @param list
    * list of words to add to the dictionary
    */
   public void setDataList(ArrayList<String> list) {
      data = list;
      Collections.sort(data);
   }

   /**
    * Adds to the list of words from which to autocomplete
    * 
    * @param list
    * list of words to add to the dictionary
    */
   public void addDataList(ArrayList<String> list) {
      data.addAll(list);
      Collections.sort(data);
   }

   /**
    * Adds a word to the dictionary
    * 
    * @param item
    * word to add to the dictionary
    */
   public void addDataItem(String item) {
      data.add(item);
      Collections.sort(data);
   }

   /**
    * Removes a word from the dictionary
    * 
    * @param item
    * word to remove from the dictionary
    */
   public void removeDataItem(String item) {
      data.remove(item);
   }

   /**
    * Clears the dictionary
    */
   public void clearDataList() {
      data.clear();
   }


   /**
    * Returns the list of words in the dictionary
    */
   public ArrayList<String> getDataList() {
      return data;
   }

   /**
    * Sets the keyboard key to be used for triggering a completion
    */
   public void setCompletionKey(int keyEventCode) {
      myCompletionKey = keyEventCode;
      updateFocusTraversalSetting();	// turn on/off tab traversal if necessary
   }
   
   /**
    * Sets the keyboard key to be used for triggering the next completion option
    */
   public void setNextCompletionKey(int keyEventCode) {
      myNextCompletionKey = keyEventCode;
      updateFocusTraversalSetting();	// turn on/off tab traversal if necessary
   }
   
   /**
    * Sets the keyboard key to be used for triggering the next completion option
    */
   public void setPreviousCompletionKey(int keyEventCode) {
      myPreviousCompletionKey = keyEventCode;
      updateFocusTraversalSetting();	// turn on/off tab traversal if necessary
   }
   
   /**
    * Finds the set of words in dict that begin with 'word'
    */
   public static ArrayList<String> getSuggestions(String word, ArrayList<String> dict) {
      ArrayList<String> suggs = new ArrayList<String>();
      for (String str : dict) {
	 if (str.startsWith(word)) {
	    suggs.add(str);
	 }
      }
      return suggs;
   }


   private boolean updateSuggestions(String word) {

      String lastSuggestion = "";	// keep track of last suggestion
      if ((suggestionIdx >=0) && (suggestionIdx < suggestions.size())) {
	 lastSuggestion = suggestions.get(suggestionIdx); 
      }
      
      // use shorter list if one is contained in the other
      if (word.equals(lastSearch)) {
	 return false;		// no update required
      } else if (word.startsWith(lastSearch)) {
	 suggestions = getSuggestions(word, suggestions);	
      } else {
	 suggestions = getSuggestions(word, data);
      }
      lastSearch = word;
      
      // try to recover suggestion
      suggestionIdx = suggestions.indexOf(lastSuggestion);
      // it's okay if this is -1, since will be corrected later

      return true;
   }

   /**
    * Given a list of strings, finds the greatest common prefix
    * @param array
    * input array of strings
    * @return
    * the greatest common prefix
    */
   public static String getPrefix(ArrayList<String> array) {
      String pre = "";
      int preLength = 0;

      if (array.size() == 0) {
	 return "";
      }

      int maxLength = array.get(0).length();

      char c;

      // loop through each character to see if it matches in all supplied words
      for (int i=0; i<maxLength; i++) {
	 boolean diff = false;
	 c = array.get(0).charAt(i);
	 for (int j=1; j<array.size(); j++) {
	    if (array.get(j).charAt(i) != c) {
	       diff = true;
	       break;
	    }
	 }
	 if (diff) {
	    break;
	 }
	 preLength++;
      }
      pre = array.get(0).substring(0, preLength);
      return pre;
   }

}
