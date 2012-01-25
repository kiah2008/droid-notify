package apps.droidnotify.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

import apps.droidnotify.common.Common;
import apps.droidnotify.common.Constants;
import apps.droidnotify.log.Log;
import apps.droidnotify.receivers.RescheduleReceiver;

public class RescheduleBroadcastReceiverService extends WakefulIntentService {
	
	//================================================================================
    // Properties
    //================================================================================
	
	boolean _debug = false;

	//================================================================================
	// Public Methods
	//================================================================================
	
	/**
	 * Class Constructor.
	 */
	public RescheduleBroadcastReceiverService() {
		super("RescheduleBroadcastReceiverService");
		_debug = Log.getDebug();
		if (_debug) Log.v("RescheduleBroadcastReceiverService.RescheduleBroadcastReceiverService()");
	}

	//================================================================================
	// Protected Methods
	//================================================================================
	
	/**
	 * Do the work for the service inside this function.
	 * 
	 * @param intent - Intent object that we are working with.
	 */
	@Override
	protected void doWakefulWork(Intent intent) {
		if (_debug) Log.v("RescheduleBroadcastReceiverService.doWakefulWork()");
		try{
			Context context = getApplicationContext();
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
			//Read preferences and exit if app is disabled.
		    if(!preferences.getBoolean(Constants.APP_ENABLED_KEY, true)){
				if (_debug) Log.v("RescheduleBroadcastReceiverService.doWakefulWork() App Disabled. Exiting...");
				return;
			}
			//Block the notification if it's quiet time.
			if(Common.isQuietTime(context)){
				if (_debug) Log.v("RescheduleBroadcastReceiverService.doWakefulWork() Quiet Time. Exiting...");
				return;
			}
		    Bundle bundle = intent.getExtras();
		    int notificationType = bundle.getInt(Constants.BUNDLE_NOTIFICATION_TYPE) - 100;
		    //Check the state of the users phone.
		    TelephonyManager telemanager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		    boolean notificationIsBlocked = false;
		    boolean rescheduleNotification = true;
		    boolean callStateIdle = telemanager.getCallState() == TelephonyManager.CALL_STATE_IDLE;
		    String blockingAppRuningAction = null;
		    boolean showBlockedNotificationStatusBarNotification = false;
		    switch(notificationType){
			    case Constants.NOTIFICATION_TYPE_RESCHEDULE_PHONE:{
			    	blockingAppRuningAction = preferences.getString(Constants.PHONE_BLOCKING_APP_RUNNING_ACTION_KEY, Constants.BLOCKING_APP_RUNNING_ACTION_SHOW);
			    	showBlockedNotificationStatusBarNotification = preferences.getBoolean(Constants.PHONE_STATUS_BAR_NOTIFICATIONS_SHOW_WHEN_BLOCKED_ENABLED_KEY, true);
			    	break;
			    }
			    case Constants.NOTIFICATION_TYPE_RESCHEDULE_SMS:{
			    	blockingAppRuningAction = preferences.getString(Constants.SMS_BLOCKING_APP_RUNNING_ACTION_KEY, Constants.BLOCKING_APP_RUNNING_ACTION_SHOW);
			    	showBlockedNotificationStatusBarNotification = preferences.getBoolean(Constants.SMS_STATUS_BAR_NOTIFICATIONS_SHOW_WHEN_BLOCKED_ENABLED_KEY, true);
			    	break;
			    }
			    case Constants.NOTIFICATION_TYPE_RESCHEDULE_MMS:{
			    	blockingAppRuningAction = preferences.getString(Constants.MMS_BLOCKING_APP_RUNNING_ACTION_KEY, Constants.BLOCKING_APP_RUNNING_ACTION_SHOW);
			    	showBlockedNotificationStatusBarNotification = preferences.getBoolean(Constants.MMS_STATUS_BAR_NOTIFICATIONS_SHOW_WHEN_BLOCKED_ENABLED_KEY, true);
			    	break;
			    }
			    case Constants.NOTIFICATION_TYPE_RESCHEDULE_CALENDAR:{
			    	blockingAppRuningAction = preferences.getString(Constants.CALENDAR_BLOCKING_APP_RUNNING_ACTION_KEY, Constants.BLOCKING_APP_RUNNING_ACTION_SHOW);
			    	showBlockedNotificationStatusBarNotification = preferences.getBoolean(Constants.CALENDAR_STATUS_BAR_NOTIFICATIONS_SHOW_WHEN_BLOCKED_ENABLED_KEY, true);
			    	break;
			    }
			    case Constants.NOTIFICATION_TYPE_RESCHEDULE_GMAIL:{
			    	break;
			    }
			    case Constants.NOTIFICATION_TYPE_RESCHEDULE_TWITTER:{
			    	blockingAppRuningAction = preferences.getString(Constants.TWITTER_BLOCKING_APP_RUNNING_ACTION_KEY, Constants.BLOCKING_APP_RUNNING_ACTION_SHOW);
			    	showBlockedNotificationStatusBarNotification = preferences.getBoolean(Constants.TWITTER_STATUS_BAR_NOTIFICATIONS_SHOW_WHEN_BLOCKED_ENABLED_KEY, true);
			    	break;
			    }
			    case Constants.NOTIFICATION_TYPE_RESCHEDULE_FACEBOOK:{
			    	blockingAppRuningAction = preferences.getString(Constants.FACEBOOK_BLOCKING_APP_RUNNING_ACTION_KEY, Constants.BLOCKING_APP_RUNNING_ACTION_SHOW);
			    	showBlockedNotificationStatusBarNotification = preferences.getBoolean(Constants.FACEBOOK_STATUS_BAR_NOTIFICATIONS_SHOW_WHEN_BLOCKED_ENABLED_KEY, true);
			    	break;
			    }
			    case Constants.NOTIFICATION_TYPE_RESCHEDULE_K9:{
			    	blockingAppRuningAction = preferences.getString(Constants.K9_BLOCKING_APP_RUNNING_ACTION_KEY, Constants.BLOCKING_APP_RUNNING_ACTION_SHOW);
			    	showBlockedNotificationStatusBarNotification = preferences.getBoolean(Constants.K9_STATUS_BAR_NOTIFICATIONS_SHOW_WHEN_BLOCKED_ENABLED_KEY, true);
			    	break;
			    }
		    }
		    //Reschedule notification based on the users preferences.
		    if(!callStateIdle){
		    	notificationIsBlocked = true;		    	
		    	rescheduleNotification = preferences.getBoolean(Constants.IN_CALL_RESCHEDULING_ENABLED_KEY, false);
		    }else{		    	
		    	notificationIsBlocked = Common.isNotificationBlocked(context, blockingAppRuningAction);
		    }
		    if(!notificationIsBlocked){
				Intent rescheduleIntent = new Intent(context, RescheduleService.class);
				rescheduleIntent.putExtras(intent.getExtras());
				WakefulIntentService.sendWakefulWork(context, rescheduleIntent);
		    }else{
		    	//Display the Status Bar Notification even though the popup is blocked based on the user preferences.
		    	if(showBlockedNotificationStatusBarNotification){
		    		//Get the notification info.
		    		Bundle rescheduleBundle = intent.getExtras();
					Bundle rescheduleNotificationBundle = rescheduleBundle.getBundle(Constants.BUNDLE_NOTIFICATION_BUNDLE_NAME);
				    if(rescheduleNotificationBundle != null){
						//Loop through all the bundles that were sent through.
						int bundleCount = rescheduleNotificationBundle.getInt(Constants.BUNDLE_NOTIFICATION_BUNDLE_COUNT);
						for(int i=1;i<=bundleCount;i++){
							Bundle rescheduleNotificationBundleSingle = rescheduleNotificationBundle.getBundle(Constants.BUNDLE_NOTIFICATION_BUNDLE_NAME + "_" + String.valueOf(i));
			    			if(rescheduleNotificationBundleSingle != null){
								//Display Status Bar Notification
							    Common.setStatusBarNotification(context, rescheduleNotificationBundleSingle.getInt(Constants.BUNDLE_NOTIFICATION_TYPE), rescheduleNotificationBundleSingle.getInt(Constants.BUNDLE_NOTIFICATION_SUB_TYPE), callStateIdle, rescheduleNotificationBundleSingle.getString(Constants.BUNDLE_CONTACT_NAME), rescheduleNotificationBundleSingle.getString(Constants.BUNDLE_SENT_FROM_ADDRESS), rescheduleNotificationBundleSingle.getString(Constants.BUNDLE_MESSAGE_BODY), rescheduleNotificationBundleSingle.getString(Constants.BUNDLE_K9_EMAIL_URI), rescheduleNotificationBundleSingle.getString(Constants.BUNDLE_LINK_URL));
			    			}
						}			    			
					}
		    	}		    	
		    	//Ignore notification based on the users preferences.
		    	if(blockingAppRuningAction.equals(Constants.BLOCKING_APP_RUNNING_ACTION_IGNORE)){
		    		rescheduleNotification = false;
		    		return;
		    	}
		    	if(rescheduleNotification){
			    	//Set alarm to go off x minutes from the current time as defined by the user preferences.
			    	long rescheduleInterval = Long.parseLong(preferences.getString(Constants.RESCHEDULE_BLOCKED_NOTIFICATION_TIMEOUT_KEY, Constants.RESCHEDULE_BLOCKED_NOTIFICATION_TIMEOUT_DEFAULT)) * 60 * 1000;
		    		if (_debug) Log.v("RescheduleBroadcastReceiverService.doWakefulWork() Rescheduling notification. Rechedule in " + rescheduleInterval + "minutes.");					
					String intentActionText = "apps.droidnotify.alarm/RescheduleReceiverAlarm/" + String.valueOf(notificationType) + "/" + String.valueOf(System.currentTimeMillis());
					long rescheduleTime = System.currentTimeMillis() + rescheduleInterval;
					Common.startAlarm(context, RescheduleReceiver.class, intent.getExtras(), intentActionText, rescheduleTime);
		    	}
		    }
		}catch(Exception ex){
			Log.e("RescheduleBroadcastReceiverService.doWakefulWork() ERROR: " + ex.toString());
		}
	}
		
}