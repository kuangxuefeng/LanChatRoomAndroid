package com.kxf.lanchatroomandroid;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MainActivity extends Activity {
    public static final int port = 21314;
    private InetAddress address;
    private MulticastSocket socket;
    private StringBuffer sb = new StringBuffer();
    private TextView tv_show;
    private EditText et_msg;
    private WifiManager.MulticastLock lock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        startChart();
    }

    private void startChart() {
        try {
            address = InetAddress.getByName("224.0.0.1");//组播地址：称为组播组的一组主机所共享的地址。组播地址的范围在224.0.0.0--- 239.255.255.255之间（都为D类地址 1110开头）。
            socket = new MulticastSocket(port);
            socket.joinGroup(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
//        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        lock = manager.createMulticastLock("test wifi");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    udpRece();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void initView() {
        tv_show = findViewById(R.id.tv_show);
        et_msg = findViewById(R.id.et_msg);
    }

    private void showInfo(final String info) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                sb.append(info + "\n\r");
                tv_show.setText(sb.toString());
            }
        });
    }

    public void sendMsg(View view) {
        final String msg = et_msg.getText().toString();
        if (TextUtils.isEmpty(msg)) {
            Toast.makeText(this, R.string.msg_not_null, Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    udpSend(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void udpRece() throws IOException {
        showInfo("接收端启动......");

        while (true) {

            //2,创建数据包。
            byte[] buf = new byte[1024];
            DatagramPacket dp = new DatagramPacket(buf, buf.length);

//            lock.acquire();
            //3,使用接收方法将数据存储到数据包中。
            socket.receive(dp);//阻塞式的。
//            lock.release();

            //4，通过数据包对象的方法，解析其中的数据,比如，地址，端口，数据内容。
            String ip = dp.getAddress().getHostAddress();
            int port = dp.getPort();
            String text = new String(dp.getData(), 0, dp.getLength());

            showInfo(ip + ":" + port + ":" + text);
        }
    }

    private void udpSend(String msg) throws IOException {
        //1,udpsocket服务。使用DatagramSocket对象。

        String host = IpUtils.getIPAddress(this);
        Log.d("MainActivity", "host=" + host);

        byte[] buf = msg.getBytes();
        DatagramPacket dp =
                new DatagramPacket(buf, buf.length, address, port);
//        lock.acquire();
        socket.send(dp);
//        lock.release();

        showInfo("发送成功：" + msg);
        //4，关闭资源。
//        socket.leaveGroup(address);
//        socket.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            socket.leaveGroup(address);
        } catch (Exception e) {
            e.printStackTrace();
        }
        socket.close();
    }
}
