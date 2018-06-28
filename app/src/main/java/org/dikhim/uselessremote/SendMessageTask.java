package org.dikhim.uselessremote;

import android.os.AsyncTask;

import java.io.PrintWriter;

/**
 * Created by dikobraz on 15.12.17.
 */

public class SendMessageTask extends AsyncTask {
    private String message;

    private PrintWriter out;

    public void setMessage(String message){
        this.message = message;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public SendMessageTask() {
    }

    public SendMessageTask(PrintWriter out) {
        this.out = out;
    }


    @Override
    protected Object doInBackground(Object[] objects) {
        out.println(message);
        return null;
    }
}
