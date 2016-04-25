package net.sirok.slofisc.network;

import com.android.volley.RetryPolicy;

import java.util.Map;

public abstract class DataFetchedListener {
    public Map<String, String> getExtraHeaderParams(){return null;}
	public RetryPolicy getRetryPolicy(){return null;}

	public abstract boolean useProduction();
	public abstract String getTag();
	public abstract String getUrlPath();
	public abstract String getPostBody();
	public abstract void dataFetchedSuccess(String response);
	public abstract void dataFetchedError(int errorNo, String response);
}
