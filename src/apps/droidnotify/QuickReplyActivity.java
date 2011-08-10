package apps.droidnotify;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.telephony.SmsManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import apps.droidnotify.common.Common;
import apps.droidnotify.log.Log;

/**
 * This is the quick reply activity that is used to send sms messages.
 * 
 * @author Camille S�vigny
 */
public class QuickReplyActivity extends Activity {

	//================================================================================
    // Constants
    //================================================================================
	
	private static final int SEND_BUTTON = R.id.quick_reply_send_button;
	private static final int CANCEL_BUTTON = R.id.quick_reply_cancel_button;
	private static final int SEND_TO_TEXT_VIEW = R.id.send_to_text_view;
	private static final int MESSAGE_EDIT_TEXT = R.id.message_edit_text;
	private static final int CHARACTERS_REMAINING_TEXT_TEXT = R.id.characters_remaining_text_view;

	private static final String HAPTIC_FEEDBACK_ENABLED_KEY = "haptic_feedback_enabled";
	private static final String LANDSCAPE_SCREEN_ENABLED_KEY = "landscape_screen_enabled";
	private static final String SAVE_MESSAGE_DRAFT_KEY = "quick_reply_save_draft_enabled";
	private static final String HIDE_CANCEL_BUTTON_KEY = "quick_reply_hide_cancel_button_enabled";
	
	private static final String QUICK_REPLY_BLUR_SCREEN_ENABLED_KEY = "quick_reply_blur_screen_background_enabled";
	private static final String QUICK_REPLY_DIM_SCREEN_ENABLED_KEY = "quick_reply_dim_screen_background_enabled";
	private static final String QUICK_REPLY_DIM_SCREEN_AMOUNT_KEY = "quick_reply_dim_screen_background_amount";
	
	private static final String APP_THEME_KEY = "app_theme";
	private static final String ANDROID_FROYO_THEME = "android";
	private static final String ANDROID_GINGERBREAD_THEME = "android_dark";
	private static final String IPHONE_THEME = "iphone";
	private static final String DARK_TRANSLUCENT_THEME = "dark_translucent";
	private static final String DARK_TRANSLUCENT_V2_THEME = "dark_translucent_v2";
	private static final String DARK_TRANSLUCENT_V3_THEME = "dark_translucent_v3";
	private static final String HTC_SENSE_UI_THEME = "theme_htc";
	private static final String XPERIA_THEME = "theme_xperia";
	
	private static final String SMS_GATEWAY_KEY = "quick_reply_sms_gateway_settings";
	private static final int SMS_EMAIL_GATEWAY_KEY_1 = 1;
	private static final int SMS_EMAIL_GATEWAY_KEY_2 = 2;
	private static final int SMS_EMAIL_GATEWAY_KEY_3 = 3;
	private static final int SMS_EMAIL_GATEWAY_KEY_4 = 4;
	private static final int SMS_EMAIL_GATEWAY_KEY_5 = 5;
	private static final int SMS_EMAIL_GATEWAY_KEY_6 = 6;
	private static final int SMS_EMAIL_GATEWAY_KEY_7 = 7;
	private static final int SMS_EMAIL_GATEWAY_KEY_8 = 8;
	
	//================================================================================
    // Properties
    //================================================================================
	
	private boolean _debug;
	private Context _context = null;
	private Button _sendButton = null;
	private Button _cancelButton = null;
	private TextView _sendToTextView  = null;
	private TextView _charactersRemaining = null;
	private EditText _messageEditText = null;
	private String _phoneNumber = null;
	private String _name = null;
	private SharedPreferences _preferences = null;
	private boolean _messageSent = false;

	//================================================================================
	// Public Methods
	//================================================================================

	/**
	 * Handles the activity when the configuration changes (e.g. The phone switches from portrait view to landscape view).
	 */
	public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (_debug) Log.v("QuickReplyActivity.onConfigurationChanged()");
        //Do Nothing.
	}
	
	//================================================================================
	// Protected Methods
	//================================================================================
	
	/**
	 * Called when the activity is created. Set up views and notifications.
	 * 
	 * @param bundle - The bundle passed into this Activity.
	 */
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		_debug = Log.getDebug();
	    if (_debug) Log.v("QuickReplyActivity.onCreate()");
	    _context = getApplicationContext();
	    _preferences = PreferenceManager.getDefaultSharedPreferences(_context);
	    //Don't rotate the Activity when the screen rotates based on the user preferences.
	    if(!_preferences.getBoolean(LANDSCAPE_SCREEN_ENABLED_KEY, false)){
	    	this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	    }
	    //Get main window for this Activity.
	    Window mainWindow = getWindow(); 
	    //Set Blur 
	    if(_preferences.getBoolean(QUICK_REPLY_BLUR_SCREEN_ENABLED_KEY, false)){
	    	mainWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
	    }
	    //Set Dim
	    if(_preferences.getBoolean(QUICK_REPLY_DIM_SCREEN_ENABLED_KEY, true)){
	    	mainWindow.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND); 
		    WindowManager.LayoutParams params = mainWindow.getAttributes(); 
		    int dimAmt = Integer.parseInt(_preferences.getString(QUICK_REPLY_DIM_SCREEN_AMOUNT_KEY, "50"));
		    params.dimAmount = dimAmt / 100f; 
		    mainWindow.setAttributes(params); 
	    }
	    //Set based on the theme. This is set in the user preferences.
		String applicationThemeSetting = _preferences.getString(APP_THEME_KEY, ANDROID_FROYO_THEME);
		int themeResource = R.layout.android_froyo_theme_notification;
		if(applicationThemeSetting.equals(ANDROID_FROYO_THEME)) themeResource = R.layout.android_froyo_theme_smsreply;
		if(applicationThemeSetting.equals(ANDROID_GINGERBREAD_THEME)) themeResource = R.layout.android_gingerbread_theme_smsreply;
		if(applicationThemeSetting.equals(IPHONE_THEME)) themeResource = R.layout.iphone_theme_smsreply;
		if(applicationThemeSetting.equals(DARK_TRANSLUCENT_THEME)) themeResource = R.layout.dark_translucent_theme_smsreply;
		if(applicationThemeSetting.equals(DARK_TRANSLUCENT_V2_THEME)) themeResource = R.layout.dark_translucent_v2_theme_smsreply;
		if(applicationThemeSetting.equals(DARK_TRANSLUCENT_V3_THEME)) themeResource = R.layout.dark_translucent_v3_theme_smsreply;		
		if(applicationThemeSetting.equals(HTC_SENSE_UI_THEME)) themeResource = R.layout.htc_theme_smsreply;	
		if(applicationThemeSetting.equals(XPERIA_THEME)) themeResource = R.layout.xperia_theme_smsreply;	
	    setContentView(themeResource);  
	    _sendButton = (Button)findViewById(SEND_BUTTON);
	    //Disable the Send button initially.
	    _sendButton.setEnabled(false);
	    _cancelButton = (Button)findViewById(CANCEL_BUTTON);
	    _sendToTextView = (TextView)findViewById(SEND_TO_TEXT_VIEW);
	    _messageEditText = (EditText)findViewById(MESSAGE_EDIT_TEXT);
	    _charactersRemaining = (TextView)findViewById(CHARACTERS_REMAINING_TEXT_TEXT);
	    //Add a TextWatcher.
	    _messageEditText.addTextChangedListener(new TextWatcher() {
	    	public void afterTextChanged(Editable s){
	    		//Do Nothing.
	    	}
	    	public void beforeTextChanged(CharSequence s, int start, int count, int after){
	    		//Do Nothing.
	    	}
	    	public void onTextChanged(CharSequence s, int start, int before, int count){
	    		//Enable the Send button if there is text in the EditText layout.
	    		if(s.length() > 0){
	    			_sendButton.setEnabled(true);
	    		}else{
	    			_sendButton.setEnabled(false);
	    		}
	    		int charactersRemaining = 160 - s.length();
	    		_charactersRemaining.setText(String.valueOf(charactersRemaining));
	    	}
	    });
	    //Get name and phone number from the Bundle.
	    Bundle extrasBundle = getIntent().getExtras();
	    parseQuickReplyParameters(extrasBundle);
	    //Setup Activities buttons.
	    setupButtons();
	    //Set focus to appropriate field.
	    setFocus();
	}
	
	/**
	 * Activity was started after it stopped or for the first time.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		_debug = Log.getDebug();
	    if (_debug) Log.v("QuickReplyActivity.onStart()");
	    setFocus();
	}
	  
	/**
	 * Activity was resumed after it was stopped or paused.
	 */
	@Override
	protected void onResume() {
	    super.onResume();
	    _debug = Log.getDebug();
	    if (_debug) Log.v("QuickReplyActivity.onResume()");
	    setFocus();
	}
	  
	/**
	 * Activity was paused due to a new Activity being started or other reason.
	 */
	@Override
	protected void onPause() {
	    super.onPause();
	    if (_debug) Log.v("QuickReplyActivity.onPause()");
	    showSoftKeyboard(false, (EditText) findViewById(R.id.message_edit_text));
	}
	  
	/**
	 * Activity was stopped due to a new Activity being started or other reason.
	 */
	@Override
	protected void onStop() {
	    super.onStop();
	    if (_debug) Log.v("QuickReplyActivity.onStop()");
	}
	  
	/**
	 * Activity was stopped and closed out completely.
	 */
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    if (_debug) Log.v("QuickReplyActivity.onDestroy()");
	    showSoftKeyboard(false, (EditText) findViewById(R.id.message_edit_text));
	    saveMessageDraft();
	}
	
	//================================================================================
	// Private Methods
	//================================================================================
	
	/**
	 * Gets the passed in parameters for this Activity and loads them into the text fields.
	 * 
	 * @param bundle - The bundle passed into this Activity.
	 */
	private void parseQuickReplyParameters(Bundle bundle){
		if (_debug) Log.v("QuickReplyActivity.parseQuickReplyParameters()");
		_phoneNumber = bundle.getString("smsPhoneNumber");
		_name = bundle.getString("smsName");
		String message = bundle.getString("smsMessage");
		if(_phoneNumber == null){
			if (_debug) Log.v("QuickReplyActivity.parseQuickReplyParameters() Send To number is null. Exiting...");
			return;
		}
		if(!_name.equals("")){
			_sendToTextView.setText("To: " + _name + " (" + _phoneNumber + ")");
		}else{
			_sendToTextView.setText("To: " + _phoneNumber);
		}		
		if(message != null){
			_messageEditText.setText(message);
		}
	}
	
	/**
	 * Setup the Quick Reply buttons.
	 */
	private void setupButtons(){
		if (_debug) Log.v("QuickReplyActivity.setupButtons()");
	    _sendButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
            	customPerformHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            	if(sendSMSMessage()){
	                //Set the result for this activity.
	                setResult(RESULT_OK);
	                //Finish Activity.
	                finish();
            	}
            }
        });
	    if(_preferences.getBoolean(HIDE_CANCEL_BUTTON_KEY, true)){
	    	_cancelButton.setVisibility(View.GONE);
    	}else{
		    _cancelButton.setOnClickListener(new View.OnClickListener(){
	            public void onClick(View view) {
	            	customPerformHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
	            	//Set the result for this activity.
	            	setResult(RESULT_CANCELED);
	            	//Finish Activity.
	            	finish();                
	            }
	        });
    	}
	}
	
	/**
	 * Send simple SMS message.
	 * 
	 * @return boolean - Returns true if the message was sent.
	 */
	private boolean sendSMSMessage(){
		if (_debug) Log.v("QuickReplyActivity.sendSMSMessage()");
        String message = _messageEditText.getText().toString();                 
        if(_phoneNumber.length()>0 && message.length()>0){                
            sendSMS(_phoneNumber, message);  
            return true;
        }else{
        	if(_phoneNumber.length()<= 0){
        		Toast.makeText(getBaseContext(), getString(R.string.phone_number_error_text), Toast.LENGTH_LONG).show();
        	}else if(message.length()<= 0){
        		Toast.makeText(getBaseContext(), getString(R.string.message_error_text), Toast.LENGTH_LONG).show();
        	}
        	return false;
        }
	}
	
	/**
	 * Send SMS message.
	 * 
	 * @param phoneNumber - The phone number we are sending the message to.
	 * @param message - The message we are sending.
	 */
	private void sendSMS(String smsAddress, String message){   
		if (_debug) Log.v("QuickReplyActivity.sendSMS()");
//      final String SMS_SENT = "SMS_SENT";
//      final String SMS_DELIVERED = "SMS_DELIVERED";
        //PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_SENT), 0);
        //PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0, new Intent(SMS_DELIVERED), 0);
        PendingIntent sentPI = null;
        PendingIntent deliveredPI = null;
//        //When the SMS has been sent.
//        registerReceiver(new BroadcastReceiver(){
//            @Override
//            public void onReceive(Context arg0, Intent arg1) {
//                switch (getResultCode())
//                {
//                    case Activity.RESULT_OK:
//                        Toast.makeText(getBaseContext(), getString(R.string.message_sent_text), Toast.LENGTH_LONG).show();
//                        break;
//                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
//                        Toast.makeText(getBaseContext(), getString(R.string.message_sent_error_generic_failure_text), Toast.LENGTH_LONG).show();
//                        break;
//                    case SmsManager.RESULT_ERROR_NO_SERVICE:
//                        Toast.makeText(getBaseContext(), getString(R.string.message_sent_error_no_service_text), Toast.LENGTH_LONG).show();
//                        break;
//                    case SmsManager.RESULT_ERROR_NULL_PDU:
//                        Toast.makeText(getBaseContext(), getString(R.string.message_sent_error_null_pdu_text), Toast.LENGTH_LONG).show();
//                        break;
//                    case SmsManager.RESULT_ERROR_RADIO_OFF:
//                        Toast.makeText(getBaseContext(), getString(R.string.message_sent_error_radio_off_text), Toast.LENGTH_LONG).show();
//                        break;
//                }
//            }
//        }, new IntentFilter(SMS_SENT));
//        //When the SMS has been delivered.
//        registerReceiver(new BroadcastReceiver(){
//            @Override
//            public void onReceive(Context arg0, Intent arg1) {
//                switch (getResultCode())
//                {
//                    case Activity.RESULT_OK:
//                        Toast.makeText(getBaseContext(), getString(R.string.message_delivered_text), Toast.LENGTH_LONG).show();
//                        break;
//                    case Activity.RESULT_CANCELED:
//                        Toast.makeText(getBaseContext(), getString(R.string.message_not_delivered_text), Toast.LENGTH_LONG).show();
//                        break;                        
//                }
//            }
//        }, new IntentFilter(SMS_DELIVERED));  
		SmsManager sms = SmsManager.getDefault();
		if(smsAddress.contains("@")){
			//Send to email address
			//Need to set the SMS-to-Email Gateway number for this to work.
			// (USA) Sprint PCS - 6245 [address message]
			// (USA) T-Mobile - 500 [address text | address/subject/text | address#subject#text]
			// (USA) AT&T - 121 [address text | address (subject) text]
			// (USA) AT&T - 111 [address text | address (subject) text]
			// (UK) AQL - 447766 [address text]
			// (UK) AQL - 404142 [address text]
			// (Croatia) T-Mobile - 100 [address#subject#text]
			// (Costa Rica) ICS - 1001 [address : (subject) text]
			//This value can be set in the Advanced Settings preferences.
			int smsToEmailGatewayKey = Integer.parseInt(_preferences.getString(SMS_GATEWAY_KEY, "1"));
			switch(smsToEmailGatewayKey){
		    	case SMS_EMAIL_GATEWAY_KEY_1:{
		    		// (USA) Sprint PCS - 6245 [address message]
		    		String smsToEmailGatewayNumber = "6245";
		    		sms.sendTextMessage(smsToEmailGatewayNumber, null, smsAddress + " " + message, sentPI, deliveredPI);
		    	}
		    	case SMS_EMAIL_GATEWAY_KEY_2:{
		    		// (USA) T-Mobile - 500 [address text | address/subject/text | address#subject#text]
		    		String smsToEmailGatewayNumber = "500";
		    		sms.sendTextMessage(smsToEmailGatewayNumber, null, smsAddress + " " + message, sentPI, deliveredPI);
		    	}
		    	case SMS_EMAIL_GATEWAY_KEY_3:{
		    		// (USA) AT&T - 121 [address text | address (subject) text]
		    		String smsToEmailGatewayNumber = "121";
		    		sms.sendTextMessage(smsToEmailGatewayNumber, null, smsAddress + " " + message, sentPI, deliveredPI);
		    	}
		    	case SMS_EMAIL_GATEWAY_KEY_4:{
		    		// (USA) AT&T - 111 [address text | address (subject) text]
		    		String smsToEmailGatewayNumber = "111";
		    		sms.sendTextMessage(smsToEmailGatewayNumber, null, smsAddress + " " + message, sentPI, deliveredPI);
		    	}
		    	case SMS_EMAIL_GATEWAY_KEY_5:{
		    		// (UK) AQL - 447766 [address text]
		    		String smsToEmailGatewayNumber = "447766";
		    		sms.sendTextMessage(smsToEmailGatewayNumber, null, smsAddress + " " + message, sentPI, deliveredPI);
		    	}
		    	case SMS_EMAIL_GATEWAY_KEY_6:{
		    		// (UK) AQL - 404142 [address text]
		    		String smsToEmailGatewayNumber = "404142";
		    		sms.sendTextMessage(smsToEmailGatewayNumber, null, smsAddress + " " + message, sentPI, deliveredPI);
		    	}
		    	case SMS_EMAIL_GATEWAY_KEY_7:{
		    		// (USA) AT&T - 121 [address text | address (subject) text]
		    		String smsToEmailGatewayNumber = "121";
		    		sms.sendTextMessage(smsToEmailGatewayNumber, null, smsAddress + " " + message, sentPI, deliveredPI);
		    	}
		    	case SMS_EMAIL_GATEWAY_KEY_8:{
		    		// (Croatia) T-Mobile - 100 [address#subject#text]
		    		String smsToEmailGatewayNumber = "100";
		    		sms.sendTextMessage(smsToEmailGatewayNumber, null, smsAddress + "##" + message, sentPI, deliveredPI);
		    	}
		    	default:{
		    		sms.sendTextMessage(smsAddress, null, message, sentPI, deliveredPI);
		    	}
		    	try{
		        	//Store the message in the Sent folder so that it shows in Messaging apps.
		            ContentValues values = new ContentValues();
		            values.put("address", smsAddress);
		            values.put("body", message);
		            getContentResolver().insert(Uri.parse("content://sms/sent"), values);
		    	}catch(Exception ex){
		    		if (_debug) Log.e("QuickReplyActivity.sendSMS() Insert Into Sent Foler ERROR: " + ex.toString());
		    	}
			}        	
		}else{
			//Send to regular text message number.
			sms.sendTextMessage(smsAddress, null, message, sentPI, deliveredPI);
			try{
		    	//Store the message in the Sent folder so that it shows in Messaging apps.
		        ContentValues values = new ContentValues();
		        values.put("address", smsAddress);
		        values.put("body", message);
		        values.put("date", String.valueOf(System.currentTimeMillis()));
		        getContentResolver().insert(Uri.parse("content://sms/sent"), values);
			}catch(Exception ex){
				if (_debug) Log.e("QuickReplyActivity.sendSMS() Insert Into Sent Foler ERROR: " + ex.toString());
			}
			_messageSent = true;
		}
    }
	
	/**
	 * Function that performs custom haptic feedback.
	 * This function performs haptic feedback based on the users preferences.
	 * 
	 * @param hapticFeedbackConstant - What type of action the feedback is responding to.
	 */
	private void customPerformHapticFeedback(int hapticFeedbackConstant){
		if (_debug) Log.v("QuickReplyActivity.customPerformHapticFeedback()");
		Vibrator vibrator = null;
		try{
			vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
			//Perform the haptic feedback based on the users preferences.
			if(_preferences.getBoolean(HAPTIC_FEEDBACK_ENABLED_KEY, true)){
				if(hapticFeedbackConstant == HapticFeedbackConstants.VIRTUAL_KEY){
					if(vibrator != null) vibrator.vibrate(50);
				}
			}
			if(_preferences.getBoolean(HAPTIC_FEEDBACK_ENABLED_KEY, true)){
				if(hapticFeedbackConstant == HapticFeedbackConstants.LONG_PRESS){
					if(vibrator != null) vibrator.vibrate(100);
				}
			}
		}catch(Exception ex){
			if (_debug) Log.e("QuickReplyActivity.customPerformHapticFeedback() ERROR: " + ex.toString());
		}
	}
	
	/**
	 * Sets the focus to the body EditText field.
	 */
	private void setFocus(){
		if (_debug) Log.v("QuickReplyActivity.setFocus()");
		EditText quickReplyMessageEditText = (EditText) findViewById(R.id.message_edit_text);
		quickReplyMessageEditText.requestFocus();
		showSoftKeyboard(true, quickReplyMessageEditText);
	}
	
	/**
	 * Shows or hides the soft keyboard on the Message EditText view.
	 * 
	 * @param showKeyboard - Boolean to either show or hide the soft keyboard.
	 */
	private void showSoftKeyboard(boolean showKeyboard, View view){
		if (_debug) Log.v("QuickReplyActivity.showSoftKeyboard()");
		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		// This will only trigger it if no physical keyboard is open.
		try{
			if(inputMethodManager != null){
				if(showKeyboard){
					inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED);
				}else{
					inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
				}
			}
		}catch(Exception ex){
			if (_debug) Log.e("QuickReplyActivity.showSoftKeyboard() ERROR: " + ex.toString());
		}
	}
	
	/**
	 * Save the message as a draft.
	 */
	private void saveMessageDraft(){
		if(_messageSent){
			return;
		}
		if(_preferences.getBoolean(SAVE_MESSAGE_DRAFT_KEY, true)){
			try{
				Context context = getBaseContext();
				String address = _phoneNumber;
				String message = _messageEditText.getText().toString().trim();
				if(!message.equals("")){
			    	//Store the message in the draft folder so that it shows in Messaging apps.
			        ContentValues values = new ContentValues();
			        values.put("address", address);
			        values.put("body", message);
			        values.put("date", String.valueOf(System.currentTimeMillis()));
			        values.put("type", "3");
			        values.put("thread_id", String.valueOf(Common.getThreadID(context, address)));
			        getContentResolver().insert(Uri.parse("content://sms/draft"), values);
			        Toast.makeText(context, getString(R.string.draft_saved_text), Toast.LENGTH_SHORT).show();
				}
			}catch(Exception ex){
				if (_debug) Log.e("QuickReplyActivity.sendSMS() Insert Into Sent Foler ERROR: " + ex.toString());
			}
		}
	}
	
}