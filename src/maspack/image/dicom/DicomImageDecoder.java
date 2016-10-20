/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

/**
 * Decodes the image information from DICOM into a pixel buffer that
 * can be used for display
 * @author Antonio
 *
 */
public interface DicomImageDecoder {

   /**
    * Decodes DICOM pixel data into a usable buffer, based on information
    * contained in the DICOM header
    * @param header informs the decoder of the transfer syntax, and any
    *        intensity transforms
    * @param data raw data extracted from the DICOM slice
    * @return a useable pixel buffer with raw pixel data used for display
    */
   DicomPixelBuffer decode(DicomHeader header, DicomPixelData data);
   
   /**
    * Determines whether the current decoder can interpret and decode the supplied
    * pixel data
    * @param header informs the deocer of the transfer syntax and any intensity
    *        transforms
    * @param data raw data extracted from the DICOM slice
    * @return true if the current decoder can interpret and decode the supplied data
    */
   boolean canDecode(DicomHeader header, DicomPixelData data);
   
}
