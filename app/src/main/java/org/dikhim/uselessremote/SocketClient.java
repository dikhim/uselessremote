package org.dikhim.uselessremote;

import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by dikobraz on 15.12.17.
 */

public class SocketClient implements Serializable {
    private boolean changed = false;

    private boolean available = false;

    private String remoteAddress;
    private int remotePort;
    private TextView textResponse;
    private Socket socket;
    private PrintWriter out;

    private BufferedReader in;

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        changed();

    }


    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
        changed();
    }

    public SocketClient(String remoteAddress, final int remotePort, TextView textResponse) {
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.textResponse = textResponse;
        changed();
    }

    public synchronized void start() {
        Log.e("ClientThread", "Start");
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (isChanged()) {
                    restart();
                }
                if (sendRequest("hi").equals("hi")) {
                    // OK
                } else {
                    try {
                        createSocket(remoteAddress, remotePort);
                        available = true;
                        response("Connected to " + remoteAddress + ":" + remotePort);
                    } catch (IOException e) {
                        Log.e("ClientThread", "IOException");
                        available = false;
                        response("Couldn't connect to " + remoteAddress + ":" + remotePort);
                    }
                }
            }
        }, 5, 1000, TimeUnit.MILLISECONDS);
        changed = false;
    }

    public synchronized void stop() {
        sendMessage("bye");
        try {
            Log.e("ClientThread", "Stop");
            scheduler.shutdown();
            scheduler.awaitTermination(1100, TimeUnit.MILLISECONDS);
            scheduler.shutdownNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                Log.e("ClientThread", "Finally");
                out.close();
                in.close();
                socket.close();
                Log.e("ClientThread", "AllClosed");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {

            }
        }
    }


    private void restart() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                stop();
                start();
            }
        });
        thread.start();
    }

    private void changed() {
        changed = true;
    }

    private boolean isChanged() {
        return changed;
    }

    private String sendRequest(String message) {
        try {
            out.println(message);
            String response = in.readLine();
            if (response == null) return "closed";
            return response;
        } catch (SocketTimeoutException e) {
            return "timeout";
        } catch (Exception e) {
            return "error";
        }
    }

    private void createSocket(String hostName, int portNumber) throws IOException {
        socket = new Socket(hostName, portNumber);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        socket.setSoTimeout(1000);
    }

    private void response(final String response) {
        MainActivity.mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (textResponse != null)
                    textResponse.setText(response);
            }
        });
    }

    void sendMessage(String message) {
        if (available) {
            SendMessageTask sendMessageTask = new SendMessageTask(out);
            sendMessageTask.setMessage(message);
            sendMessageTask.execute();
        }
    }


}
