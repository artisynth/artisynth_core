/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.io.*;
import maspack.util.*;

public class Rectangle2d implements Scannable {

   public double x;
   public double y;
   public double width;
   public double height;
   
   public Rectangle2d() {
      set(0,0,0,0);
   }
   
   public Rectangle2d(Rectangle2d rect) {
      set(rect.x, rect.y, rect.width, rect.height);
   }
   
   public Rectangle2d(double x, double y, double width, double height) {
      set(x,y,width,height);
   }
   
   public void set(Rectangle2d r) {
      set(r.x, r.y, r.width, r.height);
   }
   
   public void translate(double dx, double dy) {
      this.x += dx;
      this.y += dy;
   }
   
   public void set(double x, double y, double width, double height) {
      this.height = height;
      this.width = width;
      this.y = y;
      this.x = x;
   }
   
   public double getX() {
      return x;
   }
   
   public double getY() {
      return y;
   }
   
   public double getWidth() {
      return width;
   }
   
   public double getHeight() {
      return height;
   }

   double getArea() {
      return width*height;
   }
   
   public void scan (String str) {
      str = str.replaceAll("[\\[,\\]]", "");
      String[] strArray = str.split(" ");
      if (strArray.length != 4) {
         throw new IllegalArgumentException("Cannot parse " + str);
      }
      x = Double.parseDouble(strArray[0]);
      y = Double.parseDouble(strArray[1]);
      width = Double.parseDouble(strArray[2]);
      height = Double.parseDouble(strArray[3]);
      
   }
   
   public String toString() {
      return String.format("[ %f %f %f %f ]", x, y, width, height  );
   }

   public boolean isWritable() {
      return true;
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      pw.print (toString());
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      rtok.scanToken ('['); 
      x = rtok.scanNumber();
      y = rtok.scanNumber();
      width = rtok.scanNumber();
      height = rtok.scanNumber();
      rtok.scanToken (']');
   }
   
   public String toString(NumberFormat fmt) {
      
      StringBuilder out = new StringBuilder();
      out.append("[ ");
      out.append(fmt.format(x));
      out.append(' ');
      out.append(fmt.format(y));
      out.append(' ');
      out.append(fmt.format(width));
      out.append(' ');
      out.append(fmt.format(height));
      out.append(' ');
      out.append("]");
      
      return out.toString();
   }
   
   @Override
   protected Object clone() throws CloneNotSupportedException {
      return new Rectangle2d(this);
   }

   public boolean equals (Object obj) {
      if (obj instanceof Rectangle2d) {
         Rectangle2d rect = (Rectangle2d)obj;
         return (rect.x == x && rect.y == y &&
                 rect.width == width && rect.height == height);
      }
      else {
         return false;
      }
   }
}
