/*
 * Copyright (c) Microsoft Corporation.  All rights reserved.
 * Licensed under the MIT license. See License.txt in the project root for license information.
 */

package com.microsoft.azure.engagement.cordova;

import java.util.Iterator;
import android.content.Context;
import android.content.Intent;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.microsoft.azure.engagement.EngagementConfiguration;
import com.microsoft.azure.engagement.EngagementAgent;
import com.microsoft.azure.engagement.EngagementAgentUtils;

public class AZME extends CordovaPlugin {
    private CordovaInterface cordova;
    private String previousActivityName = null;
    private String lastRedirect = null;
    private final String pluginVersion = "1.0.0";

    public void initialize(CordovaInterface _cordova, CordovaWebView webView) {
        CordovaActivity activity =  (CordovaActivity) _cordova.getActivity();
        final String invokeString = activity.getIntent().getDataString();

        if (invokeString != "" && invokeString != null) {
            lastRedirect = invokeString;
            System.out.println("Preparing Redirect to " + lastRedirect);
        }
        super.initialize(_cordova, webView);
        cordova = _cordova;

        try {
            ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            String appId = bundle.getString("engagement:appId");
            String sdkKey = bundle.getString("engagement:sdkKey");
            String collection = bundle.getString("engagement:collection");

            EngagementConfiguration engagementConfiguration = new EngagementConfiguration();
            engagementConfiguration.setConnectionString("Endpoint=" + collection + ";AppId=" + appId + ";SdkKey=" + sdkKey);
            EngagementAgent.getInstance(activity).init(engagementConfiguration);

            Bundle b = new Bundle();
            b.putString("CDVAZMEVersion", pluginVersion);
            EngagementAgent.getInstance(activity).sendAppInfo( b);

        } catch (PackageManager.NameNotFoundException e) {
            System.err.println("Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println("Failed to load meta-data, NullPointer: " + e.getMessage());
        }
    }

    private Bundle stringToBundle(String _param) {
        JSONObject jObj;

        try {
            jObj = new JSONObject(_param);
            Bundle b = new Bundle();

            @SuppressWarnings("unchecked")
            Iterator<String> keys = jObj.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String val = jObj.getString(key);
                b.putString(key, val);
            }
            return b;

        } catch (JSONException e) {
            return null;
        }
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext)   {
        if (action.equals("checkRedirect")) {
            callbackContext.success(lastRedirect);
            lastRedirect = null;
            return true;
        }
        if (action.equals("getStatus")) {

            final CallbackContext cb = callbackContext;
            EngagementAgent.getInstance(cordova.getActivity()).getDeviceId(new EngagementAgent.Callback<String>() {
                @Override
                public void onResult(String deviceId) {
                    System.out.println("Got my device id:" + deviceId);
                    JSONObject j;
                    String response = "{";
                    response = "{" +
                               "\"pluginVersion\": \"" + pluginVersion + "\"," +
                               "\"AZMEVersion\": \"3.0.0\"," +
                               "\"deviceId\": \"" + deviceId + "\"" +
                               "}";
                    try {

                        cb.success(new JSONObject(response));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        cb.error("could not retrieve status");
                    }
                }
            });
            return true;
        } else if (action.equals("startActivity")) {
            String activityName;
            try {
                activityName = args.getString(0);
                String param = args.getString(1);
                Bundle b = stringToBundle(param);
                if (b == null) {
                    callbackContext.error("invalid param for startActivity");
                    return true;
                }
                previousActivityName = activityName;
                EngagementAgent.getInstance(cordova.getActivity()).startActivity(cordova.getActivity(), activityName, b);
                callbackContext.success();
            } catch (JSONException e) {
                callbackContext.error("invalid args for startActivity");
            }
            return true;
        } else if (action.equals("endActivity")) {
            EngagementAgent.getInstance(cordova.getActivity()).endActivity();
            previousActivityName = null;
            callbackContext.success();
            return true;
        } else if (action.equals("sendEvent")) {

            String eventName;
            try {
                eventName = args.getString(0);
                String param = args.getString(1);
                Bundle b = stringToBundle(param);
                if (b == null) {
                    callbackContext.error("invalid param for sendEvent");
                    return true;
                }

                EngagementAgent.getInstance(cordova.getActivity()).sendEvent(eventName, b);
                callbackContext.success();
            } catch (JSONException e) {
                callbackContext.error("invalid args for sendEvent");
            }
            return true;
        } else if (action.equals("startJob")) {

            String jobName;
            try {
                jobName = args.getString(0);
                String param = args.getString(1);
                Bundle b = stringToBundle(param);
                if (b == null) {
                    callbackContext.error("invalid param for start Job");
                    return true;
                }
                EngagementAgent.getInstance(cordova.getActivity()).startJob(jobName, b);
            } catch (JSONException e) {
                callbackContext.error("invalid args for start Job");
            }
            return true;
        } else if (action.equals("endJob")) {

            String jobName;
            try {
                jobName = args.getString(0);
                EngagementAgent.getInstance(cordova.getActivity()).endJob(jobName );
                callbackContext.success();
            } catch (JSONException e) {
                callbackContext.error("invalid args for end Job");
            }
            return true;
        } else if (action.equals("sendAppInfo")) {
            String param;
            try {
                param = args.getString(0);
                Bundle b = stringToBundle(param);
                if (b == null) {
                    callbackContext.error("invalid param for sendAppInfo");
                    return true;
                }
                EngagementAgent.getInstance(cordova.getActivity()).sendAppInfo( b);
                callbackContext.success();

            } catch (JSONException e) {
                callbackContext.error("invalid args for sendAppInfo");
            }
            return true;
        } else if (action.equals("registerForPushNotification")) {
            // does nothing on Android
            callbackContext.success();
            return true;
        }
        callbackContext.error("unrecognized command :" + action);
        return false;
    }

    public void onPause(boolean multitasking) {
        EngagementAgent.getInstance(cordova.getActivity()).endActivity();
    }

    public void onResume(boolean multitasking) {
        if (previousActivityName != null)
            EngagementAgent.getInstance(cordova.getActivity()).startActivity(cordova.getActivity(), previousActivityName, null);
    }


}
