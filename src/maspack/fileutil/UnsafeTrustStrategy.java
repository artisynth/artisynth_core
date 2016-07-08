package maspack.fileutil;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.ssl.TrustStrategy;

public class UnsafeTrustStrategy implements TrustStrategy {

	@Override
	public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
		return true;
	}	

}
