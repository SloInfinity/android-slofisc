package net.sirok.slofisc.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import net.sirok.slofisc.R;
import net.sirok.slofisc.helpers.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by martin on 11.2.2016.
 */
public class Network {
    public static final int ERROR_NOT_FOUND = 404;
    public static final int ERROR_BAD_REQUEST = 400;
    public static final int ERROR_AUTH_PROBLEM = 401;
    public static final int ERROR_NOT_ACCEPTABLE = 406;
    public static final int ERROR_FORBIDDEN = 403;
    public static final int ERROR_SERVER = 500;

    public static final int ERROR_PARSING = -1;
    public static final int ERROR_CONNECTION = -2;
    public static final int ERROR_UNKNOWN = -3;
    public static final int ERROR_ACTIVATED = -4;
    public static final int ERROR_CERTIFICATE = -5;
    public static final int ERROR_RESPONSE = -6;

    private static final RetryPolicy DEFAULT_VOLLEY_RETRY_POLICY = new DefaultRetryPolicy(3000, 0, 0);

    private static Network instance;
    public static Network getInstance(Context context){
        if(instance == null){
            instance = new Network(context);
        }
        return instance;
    }

    protected Context context;
    protected RequestQueue requestQueue;

    public Network(Context context) {
        this.context = context;

        this.requestQueue = Volley.newRequestQueue(context);
    }

    public void makeGetRequest(DataFetchedListener dataFetchedListener) {
        makeRequest(dataFetchedListener, Request.Method.GET);
    }

    public void makePostRequest(DataFetchedListener dataFetchedListener) {
        makeRequest(dataFetchedListener, Request.Method.POST);
    }

    private void makeRequest(final DataFetchedListener dataFetchedListener, final int requestMethod){
        try {
            if (!hasNetworkConnectivity()) {
                dataFetchedListener.dataFetchedError(ERROR_CONNECTION, "No connection");
                return;
            }

            String url = (dataFetchedListener.useProduction()? Constants.PRODUCTION_URL: Constants.TEST_URL) + dataFetchedListener.getUrlPath();

            final Response.ErrorListener errorListener = new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    VolleyLog.d(dataFetchedListener.getTag(), "Error: " + error.getMessage());
                    Log.w(Network.class.getName(), "" + error.getClass().getName() + " " + requestMethod + " " + dataFetchedListener.getUrlPath());
                    error.printStackTrace();

                    int cause = ERROR_CONNECTION;
                    if (error.networkResponse != null)
                        cause = error.networkResponse.statusCode;

                    try {
                        String response = new String(error.networkResponse.data, "UTF-8");
                        dataFetchedListener.dataFetchedError(cause, response);
                    } catch (Exception e) {
                        e.printStackTrace();
                        dataFetchedListener.dataFetchedError(cause, e.getMessage());
                    }
                }
            };


            Request request = new StringRequest(requestMethod, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String s) {

                }
            }, errorListener){
                @Override
                public byte[] getBody() throws AuthFailureError {
                    if ((requestMethod == Method.POST) &&
                            dataFetchedListener.getPostBody() != null &&
                            !dataFetchedListener.getPostBody().isEmpty()) {
                        return dataFetchedListener.getPostBody().getBytes();
                    }
                    return super.getBody();
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> params = new HashMap<>();

                    if(dataFetchedListener.getExtraHeaderParams()!=null)
                        params.putAll(dataFetchedListener.getExtraHeaderParams());

                    if ((requestMethod == Request.Method.POST) &&
                            dataFetchedListener.getPostBody() != null &&
                            !dataFetchedListener.getPostBody().isEmpty()) {
                        params.put("Content-Length", Integer.toString(dataFetchedListener.getPostBody().getBytes().length));
                    }

                    return (params.size() == 0) ? super.getHeaders() : params;
                }

                @Override
                public String getBodyContentType() {
                    return "application/json; charset=UTF-8";
                }
            };

            request.setTag(dataFetchedListener.getTag());

            RetryPolicy rp =dataFetchedListener.getRetryPolicy();
            if(rp == null) request.setRetryPolicy(DEFAULT_VOLLEY_RETRY_POLICY);
            else request.setRetryPolicy(rp);

            requestQueue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            dataFetchedListener.dataFetchedError(ERROR_UNKNOWN, null);
        }
    }

    private boolean hasNetworkConnectivity() {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return networkInfo != null && networkInfo.isConnected();
    }
}
