package maspack.dicom;

public class DicomTransferSyntax {
   String name;
   String uid;
   boolean littleEndian;
   boolean explicit;
   boolean encoded;

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
