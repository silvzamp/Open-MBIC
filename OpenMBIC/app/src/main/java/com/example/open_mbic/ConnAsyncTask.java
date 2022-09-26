package com.example.open_mbic;

import static java.lang.String.valueOf;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/*
This class set-up a TCP/IP connection.
You can create more connections to the same IP address, different ports using more instances from this class.
For example, a connection for each sensor transmitting to the same server.
*/


public class ConnAsyncTask extends AsyncTask<Void, Integer, String> {
    private String mip = "";
    private int mport;
    private ArrayList<String> mdata = null;


    public ConnAsyncTask(String ip, int port, ArrayList<String> data) {
        this.mip = ip;
        this.mdata = data;
        this.mport = port;
    }

    protected String doInBackground(Void... arg0) {

        try {
            Socket socket = new Socket(valueOf(this.mip), this.mport);
            socket.setTcpNoDelay(true);
            DataOutputStream DOS = new DataOutputStream(socket.getOutputStream());

            if(this.mdata.size()>0){
                String value2send = this.mdata.get(0);
                if(value2send!=""){
                    DOS.writeUTF(value2send);
                }
                this.mdata.remove(0);
            }
            DOS.flush();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
