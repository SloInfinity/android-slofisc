package net.sirok.slofisc.helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by martin on 11.2.2016.
 */
public class Helpers {
    private static final SimpleDateFormat dateTimeformatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat dateTimeShortformatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static final DecimalFormat decimalFormat;
    static{
        decimalFormat = (DecimalFormat) DecimalFormat.getInstance(Locale.US);
        decimalFormat.setMaximumFractionDigits(2);
        decimalFormat.setMinimumFractionDigits(2);
        decimalFormat.setRoundingMode(RoundingMode.HALF_EVEN);
        decimalFormat.setGroupingUsed(false);
        decimalFormat.setParseBigDecimal(true);
    }

    public static String bigDecimalToString(BigDecimal bigDecimal){
        if(bigDecimal == null) return null;
        return decimalFormat.format(bigDecimal);
    }

    public static String dateToString(Date date){
        return dateTimeformatter.format(date);
    }

    public static String dateToShortString(Date date){
        return dateTimeShortformatter.format(date);
    }

    public static String generateMd5(final byte[] s) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance(MD5);
            digest.update(s);
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static JSONObject generateRequestHeader() throws JSONException {
        JSONObject obj = new JSONObject();

        obj.put("MessageID", UUID.randomUUID());
        obj.put("DateTime", dateTimeformatter.format(new Date()));

        return obj;
    }

    public static PrivateKey getPrivateKey(KeyStore keyStore, String password) throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException {
        Enumeration<String> aliases = keyStore.aliases();

        PrivateKey privateKey = null;
        while(privateKey==null && aliases.hasMoreElements()){
            String alias = aliases.nextElement();
            if(alias.equals("TaxCA"))
                continue;
            privateKey = (PrivateKey)keyStore.getKey(alias, password.toCharArray());
        }
        if(privateKey==null)
            throw new UnrecoverableKeyException("Fiscal cert error: Cannot get private key for any of aliases.");

        return privateKey;
    }

    public static Certificate getCertificate(KeyStore keyStore) throws KeyStoreException, UnrecoverableKeyException {
        Enumeration<String> aliases = keyStore.aliases();

        Certificate certificate = null;
        while(certificate==null && aliases.hasMoreElements()){
            String alias = aliases.nextElement();
            if(alias.equals("TaxCA"))
                continue;
            certificate = (Certificate)keyStore.getCertificate(alias);
        }
        if(certificate==null)
            throw new UnrecoverableKeyException("Fiscal cert error: Cannot get certificate for any of aliases.");

        return certificate;
    }
}
