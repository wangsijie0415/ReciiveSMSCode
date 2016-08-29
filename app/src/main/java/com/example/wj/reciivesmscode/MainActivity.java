package com.example.wj.reciivesmscode;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import java.security.Permission;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private EditText code;
    private String smsContent;
    private IntentFilter intentFilter;
    private Handler handler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            code.setText(smsContent);
        }
    };

    private String patternCoder = "(?<!\\d)\\d{6}(?!\\d)";
    final public static int REQUEST_CODE_ASK_CALL_PHONE = 123;


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Object[] objs = (Object[]) intent.getExtras().get("pdus");
            String format = intent.getStringExtra("format");//23以后需要的
            if(format != null){
                Log.e("format", format);
            }

            for (Object obj : objs) {
                byte[] pdu = (byte[]) obj;
                SmsMessage sms = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    sms = SmsMessage.createFromPdu(pdu,format);//API23以后修改成这个
                }else{
                    sms = SmsMessage.createFromPdu(pdu);
                }
                // 短信的内容
                String message = sms.getMessageBody();
                Log.e("logo", "message     " + message);
                // 短息的手机号。。+86开头？
                String from = sms.getOriginatingAddress();
                Log.e("logo", "from     " + from);
                if (!TextUtils.isEmpty(from)) {
                    String code = patternCode(message);
                    if (!TextUtils.isEmpty(code)) {
                        smsContent = code;
                        handler.sendEmptyMessage(1);
                    }
                }
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        code = (EditText) findViewById(R.id.code);

        //动态获取读短信权限
        if (Build.VERSION.SDK_INT >= 23) {
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
            if(checkCallPhonePermission != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECEIVE_SMS},REQUEST_CODE_ASK_CALL_PHONE);
                return;
            }
        }else{
            intentFilter = new IntentFilter();
            intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
            intentFilter.setPriority(Integer.MAX_VALUE);
            registerReceiver(receiver, intentFilter);
        }






    }


    /***
     * 获取权限回调
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_CALL_PHONE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted
                    intentFilter = new IntentFilter();
                    intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
                    intentFilter.setPriority(Integer.MAX_VALUE);
                    registerReceiver(receiver, intentFilter);
                } else {
                    // Permission Denied
                    intentFilter = new IntentFilter();
                    intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
                    intentFilter.setPriority(Integer.MAX_VALUE);
                   registerReceiver(receiver, intentFilter);
                    Toast.makeText(MainActivity.this, "CALL_PHONE Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * 匹配短信中间的6个数字（验证码等）
     *
     * @param patternContent
     * @return
     */
    private String patternCode(String patternContent) {
        if (TextUtils.isEmpty(patternContent)) {
            return null;
        }
        Pattern p = Pattern.compile(patternCoder);
        Matcher matcher = p.matcher(patternContent);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

}
