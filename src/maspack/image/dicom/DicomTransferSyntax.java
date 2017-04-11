/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.image.dicom;

/**
 * DICOM transfer syntax, determining byte order, whether or not the VR is explicit,
 * and whether or not the image byte stream is encoded
 * @author Antonio
 *
 */
public class DicomTransferSyntax {
   String name;
   String uid;
   boolean littleEndian;
   boolean explicit;
   boolean encoded;

   /**
    * Basic constructor, assumes default of little endian, explicit VR, and
    * encoded image
    */
   public DicomTransferSyntax(String uid) {
      this.name = uid;
      this.uid = uid;
      this.littleEndian = true;
      this.explicit = true;
      this.encoded = true;
   }

   public DicomTransferSyntax(String name, String uid, boolean littleEndian, boolean explicit, boolean encoded) {
      this.name = name;
      this.uid = uid;
      this.littleEndian = littleEndian;
      this.explicit = explicit;
      this.encoded  = encoded;
   }
}
