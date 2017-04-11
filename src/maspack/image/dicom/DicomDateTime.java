/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 * Stores date and time information from DICOM header information,
 * storing up to microseconds
 * @author Antonio
 *
 */
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
   
   /**
    * Adds time in microseconds to the current date/time
    * @param micros microseconds to add
    */
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
   
   /**
    * Adds time in seconds to the current date/time
    */
   public void addTimeSeconds(int seconds) {
      calendar.add(Calendar.SECOND, seconds);
      _val = calendar.getTimeInMillis();
      _val = _val*1000 + micros;
   }
   
   /**
    * Adds time in minutes to the current date/time
    */
   public void addTimeMinutes(int minutes) {
      calendar.add(Calendar.MINUTE, minutes);
      _val = calendar.getTimeInMillis();
      _val = _val*1000 + micros;
   }
   
   /**
    * Adds time in hours to the current date/time
    */
   public void addTimeHours(int hours) {
      calendar.add(Calendar.HOUR, hours);
      _val = calendar.getTimeInMillis();
      _val = _val*1000 + micros;
   }
   
   @Override
   /**
    * Compares time based on microseconds since January 1, 1970 UTC
    */
   public int compareTo(DicomDateTime o) {
      
      if (_val < o._val) {
         return -1;
      } else if (_val > o._val) {
         return 1;
      }
      return 0;
   }
   
   /**
    * Returns the number of MICROseconds since January 1, 1970 UTC
    */
   public long microseconds() {
      return _val;
   }
   
   /**
    * Returns the number of seconds since January 1, 1970 UTC
    */
   public long epoch() {
      return (long)_val/1000000; 
   }
   
   /**
    * Returns a calender representation of the date/time 
    * (only includes up to floor(seconds)).
    */
   public GregorianCalendar getCalendar() {
      return calendar;
   }

}
