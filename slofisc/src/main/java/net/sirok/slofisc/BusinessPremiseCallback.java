package net.sirok.slofisc;

/**
 * Created by martin on 11.2.2016.
 */
public interface BusinessPremiseCallback {
    /**
     * Method is called when the business premise was successfully registered.
     */
    void success();

    /**
     * Method is called when the FURS server returns an error.
     *
     * @param errorNumber network response error number
     * @param message FURS business premise error
     */
    void error(int errorNumber, String message);
}
