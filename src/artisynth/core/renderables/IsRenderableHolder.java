/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Deque;

import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.IsRenderable;
import maspack.render.IsSelectable;
import maspack.render.Renderer;
import maspack.render.HasRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.util.Scannable;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;

public class IsRenderableHolder extends RenderableComponentBase {

   IsRenderable myRenderable;
   
   // methods to get/set render props info, stored for speed
   HasRenderProps myHasRenderProps;
   Method getRenderPropsMethod;
   Method setRenderPropsMethod;
   Method createRenderPropsMethod;

   // method to write/scan
   Scannable myScannable;
   Method writeMethod;
   Method scanMethod;
   
   // properties
   public PropertyList myProps;
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   @Override
   public RenderProps getRenderProps() {
      if (myHasRenderProps != null) {
         return myHasRenderProps.getRenderProps();
      } else if (getRenderPropsMethod != null) {
         try {
            return (RenderProps)getRenderPropsMethod.invoke(myRenderable);
         } catch (IllegalAccessException e) {
            System.err.println(e.toString() + ": " + e.getMessage());
         } catch (IllegalArgumentException e) {
            System.err.println(e.toString() + ": " + e.getMessage());
         } catch (InvocationTargetException e) {
            System.err.println(e.toString() + ": " + e.getMessage());
         }
         
      }
      return super.getRenderProps();
   }
   
   @Override
   public void setRenderProps(RenderProps props) {
      
      if (myHasRenderProps != null) {
         myHasRenderProps.setRenderProps(props);
      } else if (setRenderPropsMethod != null) {
         try {
           setRenderPropsMethod.invoke(myRenderable, props);
         } catch (IllegalAccessException e) {
            System.err.println(e.toString() + ": " + e.getMessage());
         } catch (IllegalArgumentException e) {
            System.err.println(e.toString() + ": " + e.getMessage());
         } catch (InvocationTargetException e) {
            System.err.println(e.toString() + ": " + e.getMessage());
         }
      } else {
         super.setRenderProps(props);
      }
   }
   
   @Override
   public RenderProps createRenderProps() {
      if (myHasRenderProps != null) {
         return myHasRenderProps.createRenderProps();
      } else if (createRenderPropsMethod != null) {
         try {
            return (RenderProps)createRenderPropsMethod.invoke(myRenderable);
         } catch (IllegalAccessException e) {
            System.err.println(e.toString() + ": " + e.getMessage());
         } catch (IllegalArgumentException e) {
            System.err.println(e.toString() + ": " + e.getMessage());
         } catch (InvocationTargetException e) {
            System.err.println(e.toString() + ": " + e.getMessage());
         }
      }
      
      return super.createRenderProps();
   }
   
   @Override
   public int getRenderHints() {
      return myRenderable.getRenderHints() | 
         super.getRenderHints();
   }
   
   private void init(IsRenderable renderable) {
      if (renderable == null) {
         throw new IllegalArgumentException("Cannot hold a null object");
      }
      myRenderable = renderable;
      myProps = new PropertyList (IsRenderableHolder.class, RenderableComponentBase.class);
      myProps.add ("renderProps * *", "render properties", null);
      if (myRenderable instanceof HasProperties) {
         myProps.addReadOnly("renderable", "the held renderable object");
      }
      
      if (myRenderable instanceof HasRenderProps) {
         myHasRenderProps = (HasRenderProps)myRenderable;
         getRenderPropsMethod = null;
         setRenderPropsMethod = null;
         createRenderPropsMethod = null;
      } else {
         myHasRenderProps = null;
         
         // try to find methods
         Class<?> clazz = myRenderable.getClass();
         getRenderPropsMethod = getMethod(clazz, "getRenderProps");
         setRenderPropsMethod = getMethod(clazz, "setRenderProps", RenderProps.class);
         createRenderPropsMethod = getMethod(clazz, "createRenderProps");
      }
      
      if (myRenderable instanceof Scannable) {
         myScannable = (Scannable)myRenderable;
         writeMethod = null;
         scanMethod = null;
      } else {
         myScannable = null;
         Class<?> clazz = myRenderable.getClass();
         writeMethod = getMethod(clazz, "write", PrintWriter.class, NumberFormat.class, Object.class);
         scanMethod = getMethod(clazz, "scan", ReaderTokenizer.class, CompositeComponent.class);
      }
       
   }
   
   public IsRenderableHolder(IsRenderable renderable, String name) {
      super();
      init(renderable);
      setName(name);
   }
   
   public IsRenderableHolder(IsRenderable renderable) {
      super();
      init(renderable);
      
      // copy over name
      String name = null;
      
      try {
         name = (String)reflect(myRenderable.getClass(), "getName", renderable);
      } catch (Exception e) {
      }
      
      if (name != null) {
         setName(ModelComponentBase.makeValidName(name));
      }
   }
   
   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      myRenderable.updateBounds(pmin, pmax);
   }
   
   private static Method getMethod(Class<?> clazz, String method, 
      Class<?>... parameterTypes) {
      Method m = null;
      try {
         m = clazz.getMethod (method, parameterTypes);
      } catch(Exception e) {}
      return m;
   }
   
   private static Object reflect(Class<?> clazz, String method, Object obj, Object... args) 
      throws Exception {
      Method m = null;
      try {
         m = clazz.getMethod (method);
      }
      catch (Exception e) {
         throw e;
      }
      if (m != null) {
        
         Object out = null;
         try {
            out = m.invoke (obj, args);
         }
         catch (Exception e) {
            throw e;
         }
         return out;
      }
      return null;
   }
   
   @Override
   public void prerender(RenderList list) {
      myRenderable.prerender(list);
   }
   
   @Override
   public void render(Renderer renderer, int flags) {
      myRenderable.render(renderer, flags);
   }
   
   @Override
   public boolean isSelectable() {
      if (myRenderable instanceof IsSelectable) {
         return ((IsSelectable)myRenderable).isSelectable();
      } 
      // default to false
      return false;
   }

   public int numSelectionQueriesNeeded() {
      if (myRenderable instanceof IsSelectable) {
         return ((IsSelectable)myRenderable).numSelectionQueriesNeeded();
      }
      // default to -1
      return -1;
   }

   public IsRenderable getRenderable() {
      return myRenderable;
   }
   
   @Override
   public void write (PrintWriter writer, NumberFormat fmt, Object ref)
      throws IOException {
      dowrite(writer, fmt, ref);
   }
   
   @Override
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
      
      // write renderable if possible
      try {
         pw.write("renderable=");
         if (myScannable != null) {
            if (myScannable.isWritable()) {
               myScannable.write(pw, fmt, ancestor);
            }
         } else if (writeMethod != null) {
            writeMethod.invoke(myRenderable, pw, fmt, ancestor);
         } else {
            pw.write("null\n");
         }
      } catch (Exception e) {
         System.err.println("Cannot write renderable item");
         pw.write("null\n");
      }
      
   }
   
   
   @Override
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   throws IOException {
      
      rtok.nextToken();
      if (scanAttributeName (rtok, "renderable")) {
         try {
            if (myScannable != null) {
               if (myScannable.isWritable()) {
                  if (myScannable instanceof ModelComponent) {
                     tokens.offer (
                        new StringToken ("renderable"));
                     tokens.offer (
                        new ObjectToken (myScannable, rtok.lineno()));
                  }
                  myScannable.scan(rtok, tokens);
                  return true;
               }
            } else if (scanMethod != null) {
               Object out = scanMethod.invoke(myRenderable, rtok, tokens);
               if (out instanceof Boolean) {
                  return ((Boolean)out).booleanValue();
               } else {
                  return false;
               }
            } else {
               myRenderable = null;
               return false;
            }
         } catch (Exception e) {
            System.err.println("Failed to scan renderable");
            return false;
         }
      }
      return super.scanItem(rtok, tokens);
   }
   
   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "renderable")) {
         ModelComponent comp = 
            (ModelComponent)((ObjectToken)tokens.poll()).value();
         comp.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }  
}
