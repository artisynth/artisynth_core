package maspack.dicom;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


public class DicomDateTime implements Comparable<DicomDateTime> {

   GregorianCalendar calendar;
   int micros;
   long _val;
   
   public DicomDateTime(int hours, int minutes, int seconds, int micros) {
      
      calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
      calendar.clear();
      calendar.set(1970, 0, 1, hours, minutes, seconds);
      this.micros = micros;
      
      _val = calendar.getTimeInMillis();
      _val = _val * 1000 + micros; 
   }
   
   public DicomDateTime(int year, int month, int date, 
      int hours, int minutes, int seconds, int micros) {
      
      this.micros = micros;
      
      calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
      calendar.clear();
      calendar.set(year, month-1, date, hours, minutes, seconds);
      _val = calendar.getTimeInMillis();
      _val = _val * 1000 + micros;  
   }
   
   public void addTimeMicros(int micros) {
      int seconds = 0;
      this.micros += micros;
      while (this.micros > 1000000) {
         seconds++;
         this.micros -= 1000000;
      }
      calendar.add(Calendar.SECOND, seconds);
      _val = calendar.getTimeInMillis();
      _val = _val*1000 + micros;
   }
   
   public void addTimeSeconds(int seconds) {
      calendar.add(Calendar.SECOND, seconds);
      _val = calendar.getTimeInMillis();
      _val = _val*1000 + micros;
   }
   
   public void addTimeMinutes(int minutes) {
      calendar.add(Calendar.MINUTE, minutes);
      _val = calendar.getTimeInMillis();
      _val = _val*1000 + micros;
   }
   
   public void addTimeHours(int hours) {
      calendar.add(Calendar.HOUR, hours);
      _val = calendar.getTimeInMillis();
      _val = _val*1000 + micros;
   }
   
   @Override
   public int compareTo(DicomDateTime o) {
      
      if (_val < o._val) {
         return -1;
      } else if (_val > o._val) {
         return 1;
      }
      return 0;
   }
   
   /**
    * Integer number of MICROseconds since January 1, 1970 UTC
    */
   public long microseconds() {
      return _val;
   }
   
   /**
    * Integer number of seconds since January 1, 1970 UTC
    * @return
    */
   public long epoch() {
      return (long)_val/1000000; 
   }
   
   public GregorianCalendar getCalendar() {
      return calendar;
   }

}
