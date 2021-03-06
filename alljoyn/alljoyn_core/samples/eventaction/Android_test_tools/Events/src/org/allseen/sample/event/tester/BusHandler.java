/******************************************************************************
 * Copyright (c) 2014, AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 ******************************************************************************/

package org.allseen.sample.event.tester;

import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class BusHandler extends Handler {
	public static final String TAG = "BusHandler";
		
	private Activity mContext;
	private EventActionListener listener;
	private SharedPreferences mPrefs; 
	private static final String PREFS_KEY = "engine_rules";

    /* These are the messages sent to the BusHandler from the UI/Application thread. */
    public static final int INITIALIZE = 0;
    public static final int INTROSPECT = 2;
    public static final int ENABLE_EVENT = 4;
    public static final int SHUTDOWN = 100;
    
    
    private HashMap<String,Device> remoteInfo = new HashMap<String, Device>();
    
    /*
     * Native JNI methods
     */
    static {
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("MyAllJoynCode");
    }

	 /** Initialize AllJoyn in JNI(C++) */
    private native void initialize(String packageName);
    
    /** Perform introspection with description in JNI(C++) */
    private native String doIntrospection(String sessionName, String path, int sessionId);
    
    private native void introspectionDone(int sessionId);

    /** Enable listening for an event */
    private native void enableEvent(//event
								  String jEUniqueName, String jEPath,
								  String jEIface, String jEMember, String jESig);
 
    /** clear out the saved rules in local memory and persistent memory */
    private native void deleteAllRules();
    
    /** Clean up and shutdown AllJoyn in JNI(C++) */
    private native void shutdown();

    /**
     * Constructor
     */
    public BusHandler(Looper looper, Activity context, EventActionListener listener) {
        super(looper);
        this.mContext = context;
        this.listener = listener;
        this.mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }
    
    /**
     * Callback from jni code invoked via an About Annouce callback
     * @param sessionName	name of the remote application
     * @param sessionId		sessionId that was set
     * @param friendlyName	name to display in the UI
     */
	public void foundActionApplication(String sessionName, int sessionId, String friendlyName) {
		Log.d(TAG, "Found device with friendly name: "+friendlyName);
		Message msg = this.obtainMessage(INTROSPECT);
		Bundle data = new Bundle();
		data.putString("name", sessionName);
		data.putString("friendly", friendlyName);
		msg.setData(data);
		msg.arg1 = sessionId;
		this.sendMessage(msg);
	}

    /**
     * Callback from jni code invoked when a device goes away
     * @param sessionId		sessionId that was lost
     */
	public void lostActionApplication(int sessionId) {
		Log.d(TAG, "Lost applicatoin with session id: "+sessionId);
		listener.onEventLost(sessionId);
	}
	
	public void onEventReceived(final String from, final String path,
			  final String iface, final String member, final String sig) {
		Log.d(TAG, "Received an event from "+from+" - "+path+" "+iface+"::"+member+"("+sig+")");
		try{
			mContext.runOnUiThread(new Runnable() {
				public void run() {
					Toast.makeText(mContext, "Received ("+iface+"::"+member+"("+sig+")", Toast.LENGTH_SHORT).show();
				}
			});
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Helper method to send an Add Rule request to the message Handler
	 */
	public void enableEvent(Bundle b) {
		Message msg = this.obtainMessage(ENABLE_EVENT);
		msg.setData(b);
		this.sendMessage(msg);
	}
    

	/**
	 * Helper method to start the introspection process on a remote AllJoyn Appliction
	 */
    public void introspect(String sessionName, int sessionId, String path, DescriptionParser node) {
    	try{
        	String xml = doIntrospection(sessionName, path, sessionId);
        	if(xml == null) {
        		/** The device must not support the new org.allseen.Introspectable interface */
        		return;
        	}
        	Log.d(TAG, "path: "+path+"\n"+xml);
	    	node.parse(xml);

	    	/*
	    	 * Parsing complete, collect the actions and assing them into the remoteInfo object
	    	 */
	    	Device info = remoteInfo.get(sessionName);
	    	for(Description eai : node.getEvents()) {
		    	info.addEvent((EventDescription)eai);
	    	}
	    	/*
	    	 * Introspect then parse the children paths
	    	 */
	    	for(DescriptionParser in : node.getChidren()) {
	    		/*
	    		 * Recursively call and process the child nodes looking for actions and events
	    		 */
	    		introspect(sessionName, sessionId, in.getPath(), in);
	    	}
	    	
	    	/*
	    	 * If the root node then parsing complete, inform listener of events/actions found
	    	 */
	    	if(node.getPath().equals("/")) { //root node, recursive parsing complete 
	    		if(info.getEvents().size() > 0) { //We have actions so inform as such to the UI
	    			listener.onEventFound(info);
	    			introspectionDone(sessionId);
	    		}
	    	}
    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    

	/**
	 * Helper method used by Handler to call into JNI to save the rule.
	 */
    private void doEnableEvent(Bundle b) {
    	enableEvent(
				//action
				b.getString("eUniqueName"),
				b.getString("ePath"),
				b.getString("eIface"),
				b.getString("eMember"),
				b.getString("eSig"));
    }
    
	/**
	 * Handles the messages so that the UI thread is not blocked
	 */
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
        /* Connect to the bus and start our service. */
        case INITIALIZE:
        { 
        	initialize(mContext.getPackageName());
        	break;
        }
        case INTROSPECT:
        {
        	try{
	        	String sessionName = msg.getData().getString("name");
	        	Device device = new Device(sessionName, msg.arg1, msg.getData().getString("friendly"), "/");
	        	remoteInfo.put(sessionName, device);
	        	introspect(device.getSessionName(),
	        			device.getSessionId(),
	        			device.getPath(),
	        			new DescriptionParser(device.getPath())
	        			);
        	} catch( Exception e ) {
        		e.printStackTrace();
        	}
        	break;
        }
        case ENABLE_EVENT:
        {
        	try{
        		doEnableEvent(msg.getData());
        	} catch( Exception e ) {
        		e.printStackTrace();
        	}
        	break;
        }
        /* Release all resources acquired in connect. */
        case SHUTDOWN: {
            shutdown();
            break;   
        }

        default:
            break;
        }
    }
    
    
}
