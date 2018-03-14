/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

import java.io.IOException;
import java.util.List;

import maspack.util.BinaryFileInputStream;

/**
 * Decodes the image information from DICOM into a pixel buffer that
 * can be used for display
 * @author Antonio
 *
 */
public interface DicomImageDecoder {

   /**
    * Tries to decodes DICOM pixel data into a usable buffer, based on information
    * contained in the DICOM header.
    * 
    * @param header informs the decoder of the transfer syntax, and any
    *        intensity transforms
    * @param bin binary input stream of current file, at start of pixel data
    * @param frames decoded pixel buffers (raw format) for frames
    * @return true if decoder can and has successfully decoded frames, false if cannot.
    *           If false is returned, bin should be reset to the start of the pixel data.
    * @throws IOException on File IO error
    */
   boolean decodeFrames(DicomHeader header, BinaryFileInputStream bin, List<DicomPixelBuffer> frames) throws IOException;
   
   
}
