package com.XMU.assembled;

import com.unity3d.player.*;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

public class UnityPlayerActivity extends Activity
{
    protected UnityPlayer mUnityPlayer; // don't change the name of this variable; referenced from native code
    private ServerSocket serverSocket = null;
    StringBuffer stringBuffer = new StringBuffer();

    private InputStream inputStream;

    public Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case 1:
                    UnityPlayer.UnitySendMessage("IPadress", "changetext",msg.obj.toString());
                    break;
                case 2:
                    Unity_Command(Integer.parseInt(msg.obj.toString()));
                    stringBuffer.setLength(0);
                    break;
            }

        }
    };
    // Setup activity layout
    @Override protected void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        mUnityPlayer = new UnityPlayer(this);
        setContentView(mUnityPlayer);
        mUnityPlayer.requestFocus();
        receiveData();
    }

    @Override protected void onNewIntent(Intent intent)
    {
        // To support deep linking, we need to make sure that the client can get access to
        // the last sent intent. The clients access this through a JNI api that allows them
        // to get the intent set on launch. To update that after launch we have to manually
        // replace the intent with the one caught here.
        setIntent(intent);
    }

    // Quit Unity
    @Override protected void onDestroy ()
    {
        mUnityPlayer.quit();
        super.onDestroy();
    }

    // Pause Unity
    @Override protected void onPause()
    {
        super.onPause();
        mUnityPlayer.pause();
    }

    // Resume Unity
    @Override protected void onResume()
    {
        super.onResume();
        mUnityPlayer.resume();
    }

    @Override protected void onStart()
    {
        super.onStart();
        mUnityPlayer.start();
    }

    @Override protected void onStop()
    {
        super.onStop();
        mUnityPlayer.stop();
    }

    // Low Memory Unity
    @Override public void onLowMemory()
    {
        super.onLowMemory();
        mUnityPlayer.lowMemory();
    }

    // Trim Memory Unity
    @Override public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_RUNNING_CRITICAL)
        {
            mUnityPlayer.lowMemory();
        }
    }

    // This ensures the layout will be correct.
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    // For some reason the multiple keyevent type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }

    class ServerThread extends Thread{

        private Socket socket;
        private InputStream inputStream;
        private StringBuffer stringBuffer = UnityPlayerActivity.this.stringBuffer;


        public ServerThread(Socket socket,InputStream inputStream){
            this.socket = socket;
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            int len;
            byte[] bytes = new byte[20];
            boolean isString = false;

            try {
                //在这里需要明白一下什么时候其会等于 -1，其在输入流关闭时才会等于 -1，
                //并不是数据读完了，再去读才会等于-1，数据读完了，最终结果也就是读不到数据为0而已；
                while ((len = inputStream.read(bytes)) != -1) {
                    for(int i=0; i<len; i++){
                        if(bytes[i] != '\0'){
                            stringBuffer.append((char)bytes[i]);
                        }else {
                            isString = true;
                            break;
                        }
                    }
                    if(isString){
                        Message message_2 = handler.obtainMessage();
                        message_2.what = 2;
                        message_2.obj = stringBuffer;
                        handler.sendMessage(message_2);
                        isString = false;
                    }

                }
                //当这个异常发生时，说明客户端那边的连接已经断开
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    inputStream.close();
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }

        }
    }

    public void receiveData(){

        Thread thread = new Thread(){
            @Override
            public void run() {
                super.run();
                /*指明服务器端的端口号*/
                try {
                    serverSocket = new ServerSocket(8000);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                GetIpAddress.getLocalIpAddress(serverSocket);

                Message message_1 = handler.obtainMessage();//发送IP地址
                message_1.what = 1;
                message_1.obj = GetIpAddress.getIP() ;
                handler.sendMessage(message_1);

                while (true){
                    Socket socket = null;
                    try {
                        socket = serverSocket.accept();
                        inputStream  = socket.getInputStream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    new ServerThread(socket,inputStream).start();

                }
            }
        };
        thread.start();

    }

    void Unity_Command(int number) {
        int fg1,fg2;
        fg1=0;
        fg2=0;
        if (number == 1)
        {
            if(fg1==0)
            {
                UnityPlayer.UnitySendMessage("ShowObject", "TMiddle", "1");
                fg1=1;
            }else
            {
                UnityPlayer.UnitySendMessage("ShowObject", "TMiddle", "0");
                fg1=0;
            }
        }
        else if (number == 2)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeBiger", "0.003");
        else if (number == 3)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeSmaller", "0.003");
        else if (number == 4)
            if (fg2 == 0) {
                fg2 = 1;
                UnityPlayer.UnitySendMessage("ShowObject/liver","liverCut", "1");
                UnityPlayer.UnitySendMessage("ShowObject/organ","organCut", "1");
            } else {
                fg2 = 0;
                UnityPlayer.UnitySendMessage("ShowObject/liver","liverCut", "0");
                UnityPlayer.UnitySendMessage("ShowObject/organ","organCut", "0");
            }
        else if (number == 5)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnMinus_x", "3");
        else if (number == 6)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnMinus_y", "3");
        else if (number == 7) {
            UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnPlus_x", "3");
        } else if (number == 8) {
            UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnPlus_y", "3");
        } else if (number == 9)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnPlus_z", "3");
        else if (number == 10)
            UnityPlayer.UnitySendMessage("ShowObject", "MakeTurnMinus_z", "3");

        }

    public void onBackPressed() {
        super.onBackPressed();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
    /*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }
}

class GetIpAddress {

    public static String IP;
    public static int PORT;

    public static String getIP(){
        return IP;
    }
    public static int getPort(){
        return PORT;
    }
    public static void getLocalIpAddress(ServerSocket serverSocket){

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();){
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    String mIP = inetAddress.getHostAddress().substring(0, 3);
                    if(mIP.equals("192")){
                        IP = inetAddress.getHostAddress();    //获取本地IP
                        PORT = serverSocket.getLocalPort();    //获取本地的PORT
                        Log.e("IP",""+IP);
                        Log.e("PORT",""+PORT);
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

    }

}