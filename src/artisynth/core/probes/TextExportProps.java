package artisynth.core.probes;

import maspack.properties.*;
import maspack.util.*;
import maspack.widgets.*;

public class TextExportProps implements Probe.ExportProps {

   public static PropertyList myProps =
      new PropertyList (TextExportProps.class);

   protected static final boolean DEFAULT_INCLUDE_TIME = true;
   protected boolean myIncludeTime = DEFAULT_INCLUDE_TIME;

   protected static final String DEFAULT_FORMAT_STR = "%g";
   protected String myFormatStr = DEFAULT_FORMAT_STR;

   static {
      myProps.add ("formatStr", "numeric format", DEFAULT_FORMAT_STR);
      myProps.add ("includeTime", "include time data", DEFAULT_INCLUDE_TIME);
   }

   public TextExportProps() {
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public String getFormatStr() {
      return myFormatStr;
   }

   public void setFormatStr (String fmtStr) {
      FormatRange range = getFormatStrRange();
      StringHolder errMsg = new StringHolder();
      if (!range.isValid (fmtStr, errMsg)) {
         throw new IllegalArgumentException (errMsg.value);
      }
      myFormatStr = fmtStr;
   }

   public FormatRange getFormatStrRange() {
      return new FormatRange ("eEfgaA");
   }

   public boolean getIncludeTime() {
      return myIncludeTime;
   }

   public void setIncludeTime (boolean includeTime) {
      myIncludeTime = includeTime;
   }
}
