package com.willpoweru.audiowingsdemo;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by willp on 15/03/2018.
 */

public class DmsClient {
    private static final String LOG_TAG = "<Audiowings>";
    private static final int SOCKET_TIMEOUT_MS = 10000;

    private static final RetryPolicy NETWORK_RETRY_POLICY = new DefaultRetryPolicy(
            SOCKET_TIMEOUT_MS,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
    static final String PROXY_SERVER = "192.168.0.16:3000";
    static final String BASE_URL = "http://" + PROXY_SERVER;
    static final String MAC_ADDRESS = "FF-01-25-79-C7-EC";

    private MySingleton mSingleton;
    OnVolleyResponse mCallback;

    public DmsClient(Fragment fragment) {
        this.mCallback = (OnVolleyResponse) fragment;
        mSingleton = MySingleton.getInstance(fragment.getActivity());
    }

    public interface OnVolleyResponse {
        void onSuccess(String tag, JSONObject response);
    }

    void requestDmsData(final String tag, String url,
                        final HashMap<String, String> headers,
                        final HashMap<String, String> params) {

        Log.d(LOG_TAG, "Params::" + params);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                mCallback.onSuccess(tag, response);
                            }
                        },

                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d(LOG_TAG, "Error: " + error.toString());
                            }
                        }) {

            @Override
            public Map<String, String> getHeaders() {
                return headers;
            }

            @Override
            public Map<String, String> getParams() {
                return params;
            }

        };
        Log.d(LOG_TAG, tag + " Request URL: " + jsonObjectRequest.getUrl());
        jsonObjectRequest.setTag(tag);

        jsonObjectRequest.setRetryPolicy(NETWORK_RETRY_POLICY);
        jsonObjectRequest.setRetryPolicy(NETWORK_RETRY_POLICY);
// Access the RequestQueue through your singleton class.
        mSingleton.addToRequestQueue(jsonObjectRequest);
    }

}
