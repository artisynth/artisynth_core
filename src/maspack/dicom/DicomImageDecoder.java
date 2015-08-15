package maspack.dicom;

public interface DicomImageDecoder {

   DicomPixelBuffer decode(DicomHeader header, DicomPixelData data);
   boolean canDecode(DicomHeader header, DicomPixelData data);
   
}
