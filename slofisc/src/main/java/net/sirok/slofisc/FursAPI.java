package net.sirok.slofisc;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RetryPolicy;

import net.sirok.slofisc.data.BusinessPremise;
import net.sirok.slofisc.data.Invoice;
import net.sirok.slofisc.helpers.Constants;
import net.sirok.slofisc.helpers.Helpers;
import net.sirok.slofisc.network.DataFetchedListener;
import net.sirok.slofisc.network.Network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * Created by martin on 10.2.2016.
 */
public class FursAPI {
    private static FursAPI instance;


    /**
     * Use this method to get an instance of FursAPI.
     * If FursAPI is already instantiated the settings
     * will be set to new values.
     *
     * @param certificate
     * @param certPassword
     * @param production
     * @param requestTimeout
     * @return a singleton instance of FursAPI
     */
    public static FursAPI getInstance(KeyStore certificate, String certPassword, boolean production, int requestTimeout, Context context){
        if(instance == null){
            instance = new FursAPI(certificate, certPassword, production, requestTimeout, context);
        } else {
            instance.setCertificate(certificate);
            instance.setCertPassword(certPassword);
            instance.setProduction(production);
            instance.setRequestTimeout(requestTimeout);
            instance.setContext(context);
        }
        return instance;
    }

    private int requestTimeout = 2000;
    private boolean production = false;
    private String certPassword;
    private KeyStore certificate;
    private Context context;

    private BigDecimal lowTaxRate = BigDecimal.valueOf(9.5);
    private BigDecimal highTaxRate = BigDecimal.valueOf((double)22);

    /**
     * If possible use getInstance() method to instantiate the fiscalization library.
     *
     * @param certificate
     * @param certPassword
     * @param production
     * @param requestTimeout
     */
    public FursAPI(KeyStore certificate, String certPassword, boolean production, int requestTimeout, Context context){
        this.certificate = certificate;
        this.certPassword = certPassword;
        this.production = production;
        this.requestTimeout = requestTimeout;
        this.context = context;
    }

    /**
     * TODO
     *
     * @param taxNumber
     * @param dateIssued
     * @param invoiceNumber
     * @param businessPremiseId
     * @param electronicDeviceId
     * @param amount
     * @return
     * @throws UnrecoverableKeyException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws InvalidKeyException
     * @throws SignatureException
     */
    public String calculateZoi(String taxNumber, Date dateIssued, String invoiceNumber, String businessPremiseId, String electronicDeviceId, BigDecimal amount)
            throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException, SignatureException {
        String issuedString = new SimpleDateFormat( "dd.MM.yyyy HH:mm:ss" ).format(dateIssued);
        String amountString = Helpers.bigDecimalToString(amount);

        String toSign = taxNumber+issuedString+invoiceNumber+businessPremiseId+electronicDeviceId+amountString;

        PrivateKey privateKey = Helpers.getPrivateKey(certificate, certPassword);

        byte[] dataToSign = toSign.getBytes("UTF-8");
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(dataToSign);
        byte[] signedData = signature.sign();
        return Helpers.generateMd5(signedData);
    }

    /**
     * TODO
     *
     * @param taxNumber
     * @param zoi
     * @param dateIssued
     * @return
     */
    public String preparePrintableData(String taxNumber, String zoi, Date dateIssued){
        String zoiBase10 = new BigInteger(zoi, 16).toString();
        String date = new SimpleDateFormat("yyMMddHHmmss", Locale.getDefault()).format(dateIssued.getTime());

        String data = zoiBase10+taxNumber+date;

        int sum = 0;
        for(int i=0;i<data.length();i++){
            sum += Character.getNumericValue(data.charAt(i));
        }
        int control = sum%10;

        String ret = String.format("%s%d", data, control);
        while(ret.length()!=60){
            ret = String.format("0%s", ret);
        }
        return ret;
    }

    /**
     * TODO
     *
     * @param invoice
     * @param callback
     * @return
     */
    public String generateInvoiceEor(Invoice invoice, final EORCallback callback){
        try {
            final JSONObject obj = new JSONObject();

            JSONObject inv = new JSONObject();

            inv.put("TaxNumber", invoice.getTaxNumber());
            inv.put("IssueDateTime", Helpers.dateToString(invoice.getDateIssued()));
            inv.put("NumberingStructure", invoice.getNumberingStructure());

            JSONObject invId = new JSONObject();
            invId.put("BusinessPremiseID", invoice.getBusinessPremiseId());
            invId.put("ElectronicDeviceID", invoice.getElectronicDeviceId());
            invId.put("InvoiceNumber", invoice.getInvoiceNumber());
            inv.put("InvoiceIdentifier", invId);

            inv.put("InvoiceAmount", Helpers.bigDecimalToString(invoice.getInvoiceAmount()));
            inv.put("PaymentAmount", invoice.getPaymentAmount()!=null ?
                    Helpers.bigDecimalToString(invoice.getPaymentAmount()) : Helpers.bigDecimalToString(invoice.getInvoiceAmount()));
            inv.put("ProtectedID", invoice.getZoi());

            JSONArray vat = new JSONArray();
            if(invoice.getLowTaxRateBase() != null) {
                JSONObject taxSpec = new JSONObject();
                taxSpec.put("TaxRate", Helpers.bigDecimalToString(lowTaxRate));
                taxSpec.put("TaxableAmount", Helpers.bigDecimalToString(invoice.getLowTaxRateBase()));
                taxSpec.put("TaxAmount", Helpers.bigDecimalToString(invoice.getLowTaxRateAmount()));
                vat.put(taxSpec);
            }
            if(invoice.getHighTaxRateBase() != null) {
                JSONObject taxSpec = new JSONObject();
                taxSpec.put("TaxRate", Helpers.bigDecimalToString(highTaxRate));
                taxSpec.put("TaxableAmount", Helpers.bigDecimalToString(invoice.getHighTaxRateBase()));
                taxSpec.put("TaxAmount", Helpers.bigDecimalToString(invoice.getHighTaxRateAmount()));
                vat.put(taxSpec);
            }

            JSONObject taxSpec = new JSONObject();
            if(vat.length() > 0) taxSpec.put("VAT", vat);
            if(invoice.getNonTaxableAmount() != null) taxSpec.put("NontaxableAmount", Helpers.bigDecimalToString(invoice.getNonTaxableAmount()));
            if(invoice.getReverseVatTaxableAmount() != null) taxSpec.put("ReverseVATTaxableAmount", Helpers.bigDecimalToString(invoice.getReverseVatTaxableAmount()));
            if(invoice.getExemptVatTaxableAmount() != null) taxSpec.put("ExemptVATTaxableAmount", Helpers.bigDecimalToString(invoice.getExemptVatTaxableAmount()));
            if(invoice.getOtherTaxesAmount() != null) taxSpec.put("OtherTaxesAmount", Helpers.bigDecimalToString(invoice.getOtherTaxesAmount()));

            JSONArray taxPerSeller = new JSONArray();
            taxPerSeller.put(taxSpec);
            inv.put("TaxesPerSeller", taxPerSeller);

            if(invoice.getCustomerVatNumber() != null) inv.put("CustomerVATNumber", invoice.getCustomerVatNumber());
            if(invoice.getReturnsAmount() != null) inv.put("ReturnsAmount", Helpers.bigDecimalToString(invoice.getReturnsAmount()));
            if(invoice.getOperatorTaxNumber() != null) inv.put("OperatorTaxNumber", invoice.getOperatorTaxNumber());
            if(invoice.getForeignOperator()) inv.put("ForeignOperator", invoice.getForeignOperator());
            if(invoice.getSubsequentSubmit()) inv.put("SubsequentSubmit", invoice.getSubsequentSubmit());

            if(invoice.getReferenceInvoiceNumber() != null){
                JSONObject refInvId= new JSONObject();
                refInvId.put("BusinessPremiseID",invoice.getReferenceInvoiceBusinessPremiseId());
                refInvId.put("ElectronicDeviceID",invoice.getReferenceInvoiceElectronicDeviceId());
                refInvId.put("InvoiceNumber",invoice.getReferenceInvoiceNumber());

                JSONObject refInvoice = new JSONObject();
                refInvoice.put("ReferenceInvoiceIdentifier", refInvId);
                refInvoice.put("ReferenceInvoiceIssueDateTime", Helpers.dateToString(invoice.getReferenceInvoiceIssuedDate()));

                // TODO> check if this correct
                JSONArray refInvArr = new JSONArray();
                refInvArr.put(refInvoice);
                inv.put("ReferenceInvoice", refInvArr);
            }

            JSONObject header = Helpers.generateRequestHeader();
            obj.put("Header", header);
            obj.put("Invoice", inv);

            final Key privateKey = Helpers.getPrivateKey(certificate, certPassword);
            final X509Certificate cert = (X509Certificate)Helpers.getCertificate(certificate);

            Network.getInstance(context).makePostRequest(new DataFetchedListener() {
                @Override
                public boolean useProduction() {
                    return production;
                }

                @Override
                public String getTag() {
                    return "INVOICE_FISCALISATION";
                }

                @Override
                public String getUrlPath() {
                    return Constants.INVOICE_ISSUE_PATH;
                }

                @Override
                public String getPostBody() {
                    Map<String, Object> header = new HashMap<>();
                    header.put("alg", "RS256");
                    header.put("subject_name", cert.getSubjectX500Principal().getName());
                    header.put("issuer_name", cert.getIssuerX500Principal().getName());
                    header.put("serial", cert.getSerialNumber());

                    String payload = Jwts.builder()
                            .setHeader(header)
                            .setPayload(obj.toString())
                            .signWith(SignatureAlgorithm.RS256, privateKey).compact();

                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("token", payload);

                        return obj.toString();
                    } catch (Exception e){
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                public void dataFetchedSuccess(String response) {
                    String eor;
                    try {
                        JSONObject obj = new JSONObject(response);

                        // TODO> does this work?
                        String payloadString = Jwts.parser().setSigningKey(privateKey).parse(obj.getString("token")).getBody().toString();
                        JSONObject payload = new JSONObject(payloadString);

                        if(payload.has("Error")){
                            String err = payload.getString("Error");
                            callback.error(Network.ERROR_RESPONSE, err);
                            return;
                        }
                        eor = payload.getString("UniqueInvoiceID");

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.error(Network.ERROR_PARSING, "Error parsing FURS response");
                        return;
                    }
                    callback.success(eor);
                }

                @Override
                public void dataFetchedError(int errorNo, String response) {
                    try {
                        JSONObject obj = new JSONObject(response);

                        // TODO> does this work?
                        String payloadString = Jwts.parser().setSigningKey(privateKey).parse(obj.getString("token")).getBody().toString();
                        JSONObject payload = new JSONObject(payloadString);

                        if (payload.has("Error")) {
                            String err = payload.getString("Error");
                            callback.error(Network.ERROR_RESPONSE, err);
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.error(Network.ERROR_PARSING, "Error parsing FURS response");
                        return;
                    }
                    callback.error(errorNo, response);
                }

                @Override
                public RetryPolicy getRetryPolicy() {
                    return new DefaultRetryPolicy(requestTimeout, 0, 0);
                }
            });

            return header.getString("MessageID");

        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
            callback.error(Network.ERROR_CERTIFICATE, "Error generating payload");
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            callback.error(Network.ERROR_CERTIFICATE, "Error generating payload");
            return null;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            callback.error(Network.ERROR_CERTIFICATE, "Error generating payload");
            return null;
        } catch (Exception e){
            e.printStackTrace();
            callback.error(Network.ERROR_PARSING, "Error generating payload");
            return null;
        }
    }

    /**
     * TODO
     *
     * @param businessPremise
     * @param callback
     * @return
     */
    public String registerImmovableBusinessPremise(BusinessPremise businessPremise, final BusinessPremiseCallback callback){
        try {
            final JSONObject obj = new JSONObject();

            JSONObject bp = new JSONObject();

            bp.put("TaxNumber", businessPremise.getTaxNumber());
            bp.put("BusinessPremiseID", businessPremise.getPremiseId());
            bp.put("ValidityDate", Helpers.dateToShortString(businessPremise.getValidityDate()));
            bp.put("SpecialNotes", businessPremise.getSpecialNotes());

            JSONArray swSupp = new JSONArray();
            JSONObject suppEnt = new JSONObject();
            if(businessPremise.getSoftwareSupplierTaxNumber() != null && !businessPremise.getSoftwareSupplierTaxNumber().isEmpty()){
                suppEnt.put("TaxNumber", businessPremise.getSoftwareSupplierTaxNumber());
            } else {
                suppEnt.put("NameForeign", businessPremise.getForeignSoftwareSupplierName());
            }
            swSupp.put(suppEnt);
            bp.put("SoftwareSupplier",swSupp);

            JSONObject addr = new JSONObject();
            addr.put("Street", businessPremise.getStreet());
            addr.put("HouseNumber", businessPremise.getHouseNumber());
            if(businessPremise.getHouseNumberAdditional() != null && !businessPremise.getHouseNumberAdditional().isEmpty())
                addr.put("HouseNumberAdditional", businessPremise.getHouseNumberAdditional());
            addr.put("Community", businessPremise.getCommunity());
            addr.put("City", businessPremise.getCity());
            addr.put("PostalCode", businessPremise.getPostalCode());

            JSONObject pid = new JSONObject();
            pid.put("CadastralNumber", businessPremise.getStreet());
            pid.put("BuildingNumber", businessPremise.getStreet());
            pid.put("BuildingSectionNumber", businessPremise.getStreet());

            JSONObject realEstate = new JSONObject();
            realEstate.put("Address", addr);
            realEstate.put("PropertyID", pid);

            JSONObject bpi = new JSONObject();
            bpi.put("RealEstateBP",realEstate);

            bp.put("BPIdentifier", bpi);

            JSONObject header = Helpers.generateRequestHeader();
            obj.put("Header", header);
            obj.put("BusinessPremise", bp);

            final Key privateKey = Helpers.getPrivateKey(certificate, certPassword);
            final X509Certificate cert = (X509Certificate)Helpers.getCertificate(certificate);

            Network.getInstance(context).makePostRequest(new DataFetchedListener() {
                @Override
                public boolean useProduction() {
                    return production;
                }

                @Override
                public String getTag() {
                    return "REGISTER_IMMOVABLE_BUSINESS_UNIT";
                }

                @Override
                public String getUrlPath() {
                    return Constants.REGISTER_BUSINESS_UNIT_PATH;
                }

                @Override
                public String getPostBody() {
                    Map<String, Object> header = new HashMap<>();
                    header.put("alg", "RS256");
                    header.put("subject_name", cert.getSubjectX500Principal().getName());
                    header.put("issuer_name", cert.getIssuerX500Principal().getName());
                    header.put("serial", cert.getSerialNumber());

                    String payload = Jwts.builder()
                            .setHeader(header)
                            .setPayload(obj.toString())
                            .signWith(SignatureAlgorithm.RS256, privateKey).compact();

                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("token", payload);

                        return obj.toString();
                    } catch (Exception e){
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                public void dataFetchedSuccess(String response) {

                    callback.success();
                }

                @Override
                public void dataFetchedError(int errorNo, String response) {
                    try {
                        JSONObject obj = new JSONObject(response);

                        // TODO> does this work?
                        String payloadString = Jwts.parser().setSigningKey(privateKey).parse(obj.getString("token")).getBody().toString();
                        JSONObject payload = new JSONObject(payloadString);

                        if (payload.has("Error")) {
                            String err = payload.getString("Error");
                            callback.error(Network.ERROR_RESPONSE, err);
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.error(Network.ERROR_PARSING, "Error parsing FURS response");
                        return;
                    }
                    callback.error(errorNo, response);
                }

                @Override
                public RetryPolicy getRetryPolicy() {
                    return new DefaultRetryPolicy(requestTimeout, 0, 0);
                }
            });

            return header.getString("MessageID");

        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
            callback.error(Network.ERROR_CERTIFICATE, "Error generating payload");
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            callback.error(Network.ERROR_CERTIFICATE, "Error generating payload");
            return null;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            callback.error(Network.ERROR_CERTIFICATE, "Error generating payload");
            return null;
        } catch (Exception e){
            e.printStackTrace();
            callback.error(Network.ERROR_PARSING, "Error generating payload");
            return null;
        }
    }

    /**
     * TODO
     *
     * @param businessPremise
     * @param callback
     * @return
     */
    public String registerMovableBusinessPremise(BusinessPremise businessPremise, final BusinessPremiseCallback callback){
        try {
            final JSONObject obj = new JSONObject();

            JSONObject bp = new JSONObject();

            bp.put("TaxNumber", businessPremise.getTaxNumber());
            bp.put("BusinessPremiseID", businessPremise.getPremiseId());
            bp.put("ValidityDate", Helpers.dateToShortString(businessPremise.getValidityDate()));
            bp.put("SpecialNotes", businessPremise.getSpecialNotes());

            JSONArray swSupp = new JSONArray();
            JSONObject suppEnt = new JSONObject();
            if(businessPremise.getSoftwareSupplierTaxNumber() != null && !businessPremise.getSoftwareSupplierTaxNumber().isEmpty()){
                suppEnt.put("TaxNumber", businessPremise.getSoftwareSupplierTaxNumber());
            } else {
                suppEnt.put("NameForeign", businessPremise.getForeignSoftwareSupplierName());
            }
            swSupp.put(suppEnt);
            bp.put("SoftwareSupplier",swSupp);

            JSONObject bpi = new JSONObject();
            bpi.put("PremiseType", businessPremise.getMovableType());
            bp.put("BPIdentifier", bpi);

            JSONObject header = Helpers.generateRequestHeader();
            obj.put("Header", header);
            obj.put("BusinessPremise", bp);

            final Key privateKey = Helpers.getPrivateKey(certificate, certPassword);
            final X509Certificate cert = (X509Certificate)Helpers.getCertificate(certificate);

            Network.getInstance(context).makePostRequest(new DataFetchedListener() {
                @Override
                public boolean useProduction() {
                    return production;
                }

                @Override
                public String getTag() {
                    return "REGISTER_MOVABLE_BUSINESS_UNIT";
                }

                @Override
                public String getUrlPath() {
                    return Constants.REGISTER_BUSINESS_UNIT_PATH;
                }

                @Override
                public String getPostBody() {
                    Map<String, Object> header = new HashMap<>();
                    header.put("alg", "RS256");
                    header.put("subject_name", cert.getSubjectX500Principal().getName());
                    header.put("issuer_name", cert.getIssuerX500Principal().getName());
                    header.put("serial", cert.getSerialNumber());

                    String payload = Jwts.builder()
                            .setHeader(header)
                            .setPayload(obj.toString())
                            .signWith(SignatureAlgorithm.RS256, privateKey).compact();

                    try {
                        JSONObject obj = new JSONObject();
                        obj.put("token", payload);

                        return obj.toString();
                    } catch (Exception e){
                        e.printStackTrace();
                        return null;
                    }
                }

                @Override
                public void dataFetchedSuccess(String response) {

                    callback.success();
                }

                @Override
                public void dataFetchedError(int errorNo, String response) {
                    try {
                        JSONObject obj = new JSONObject(response);

                        // TODO> does this work?
                        String payloadString = Jwts.parser().setSigningKey(privateKey).parse(obj.getString("token")).getBody().toString();
                        JSONObject payload = new JSONObject(payloadString);

                        if (payload.has("Error")) {
                            String err = payload.getString("Error");
                            callback.error(Network.ERROR_RESPONSE, err);
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.error(Network.ERROR_PARSING, "Error parsing FURS response");
                        return;
                    }
                    callback.error(errorNo, response);
                }

                @Override
                public RetryPolicy getRetryPolicy() {
                    return new DefaultRetryPolicy(requestTimeout, 0, 0);
                }
            });

            return header.getString("MessageID");

        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
            callback.error(Network.ERROR_CERTIFICATE, "Error generating payload");
            return null;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            callback.error(Network.ERROR_CERTIFICATE, "Error generating payload");
            return null;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            callback.error(Network.ERROR_CERTIFICATE, "Error generating payload");
            return null;
        } catch (Exception e){
            e.printStackTrace();
            callback.error(Network.ERROR_PARSING, "Error generating payload");
            return null;
        }
    }

    /**
     * Returns the current request timeout set on initialisation or
     * with the method setRequestTimeout.
     * This value is used as timeout time for all network requests
     * to the FURS fiscalization endpoints.
     *
     * @return the current request timeout time in milliseconds
     */
    public long getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Sets the request timeout time to a value.
     * This value will be used as timeout time for all network requests
     * to the FURS fiscalization endpoint.
     *
     * @param requestTimeout    request timeout time in milliseconds
     */
    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Returns the current boolean value which determines if the library uses
     * FURS' production or test servers.
     *
     * @return the boolean value determines if the lib uses production or test servers
     */
    public boolean isProduction() {
        return production;
    }

    /**
     * Sets the boolean value which determines if the library uses
     * FURS' production or test servers.
     *
     * @param production    True if you want to use it in production, else set False
     */
    public void setProduction(boolean production) {
        this.production = production;
    }

    /**
     * Returns the current certificate password.
     *
     * @return the current certificate password
     */
    public String getCertPassword() {
        return certPassword;
    }

    /**
     * TODO
     *
     * @param certPassword
     */
    public void setCertPassword(String certPassword) {
        this.certPassword = certPassword;
    }

    /**
     * TODO
     *
     * @return
     */
    public KeyStore getCertificate() {
        return certificate;
    }

    /**
     * TODO
     *
     * @param certificate
     */
    public void setCertificate(KeyStore certificate) {
        this.certificate = certificate;
    }

    /**
     * TODO
     *
     * @return
     */
    public Context getContext() {
        return context;
    }

    /**
     * TODO
     *
     * @param context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * TODO
     *
     * @return
     */
    public BigDecimal getLowTaxRate() {
        return lowTaxRate;
    }

    /**
     * TODO
     *
     * @param lowTaxRate
     */
    public void setLowTaxRate(BigDecimal lowTaxRate) {
        this.lowTaxRate = lowTaxRate;
    }

    /**
     * TODO
     *
     * @return
     */
    public BigDecimal getHighTaxRate() {
        return highTaxRate;
    }

    /**
     * TODO
     *
     * @param highTaxRate
     */
    public void setHighTaxRate(BigDecimal highTaxRate) {
        this.highTaxRate = highTaxRate;
    }
}
