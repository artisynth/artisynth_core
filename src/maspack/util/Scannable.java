/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.IOException;
import java.io.PrintWriter;

public interface Scannable {
   /**
    * Scans this element from a ReaderTokenizer. The expected text format is
    * assumed to be compatible with that produced by {@link #write write}.
    * 
    * @param rtok
    * Tokenizer from which to scan the element
    * @param ref
    * optional reference object which can be used for resolving references to
    * other objects
    * @throws IOException
    * if an I/O or formatting error occured
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException;

   /**
    * Writes a text description of this element to a PrintWriter. The text
    * description should be compatable with {@link #scan scan} and complete
    * enough to allow full reconstruction of the element.
    * 
    * @param writer
    * stream for writing the element
    * @param fmt
    * numeric formating information
    * @param ref
    * optional reference object which can be used for producing references to
    * other objects
    * @throws IOException
    * if an I/O error occured
    */
   public void write (PrintWriter writer, NumberFormat fmt, Object ref)
      throws IOException;

   /**
    * Returns <code>true</code> if this component should in fact be written to
    * secondary storage. This gives subclasses control over whether or
    * not they are actually written out.
    *
    * @return <code>true</code> if this component should be written to
    * secondary storage.
    */
   public boolean isWritable();

}
