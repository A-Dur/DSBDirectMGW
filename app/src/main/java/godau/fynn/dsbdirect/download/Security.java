package godau.fynn.dsbdirect.download;

import org.conscrypt.Conscrypt;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

class Security {

    private static SSLSocketFactory socketFactory;

    static void setUp() throws NoSuchProviderException, NoSuchAlgorithmException, KeyManagementException {
        Provider conscryptProvider = Conscrypt.newProvider();
        java.security.Security.addProvider(conscryptProvider);

        SSLContext conscryptContext = SSLContext.getInstance("TLSv1.2", conscryptProvider.getName());

        conscryptContext.init(null, null, null);

        socketFactory = conscryptContext.getSocketFactory();
    }

    static void setSocketFactory(HttpsURLConnection connection) {
        connection.setSSLSocketFactory(socketFactory);
    }
}
