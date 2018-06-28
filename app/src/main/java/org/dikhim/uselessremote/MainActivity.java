package org.dikhim.uselessremote;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.dikobraz.myapplication.R;

public class MainActivity extends AppCompatActivity {
    public static MainActivity mainActivity;

    public static MainActivity getInstance() {
        return mainActivity;
    }

    TextView txtOut;
    EditText txtPort;
    EditText txtAddress;
    Button left;
    Button right;
    Button wheel;
    ImageView pad;

    SocketClient socketClient;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        mainActivity = this;
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        txtOut = findViewById(R.id.txtOut);

        initAddressEditText();
        initPortEditText();

        initLeftButton();
        initRightButton();
        initWheel();

        initPad();

        createSocketClient();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Log.e("main", "onPause");
        socketClient.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("main", "onResume");
        socketClient.start();
    }



    @SuppressWarnings("SameParameterValue")
    private double getMultiplier(int lowerThreshold, int upperThreshold, double distance) {
        if (distance < lowerThreshold) {
            return 1;
        } else if ((distance >= lowerThreshold) && (distance < upperThreshold)) {
            return distance / lowerThreshold;
        } else {
            return upperThreshold / lowerThreshold;
        }
    }

    private void initLeftButton() {
        // left button
        left = findViewById(R.id.left);
        //noinspection AndroidLintClickableViewAccessibility
        left.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        socketClient.sendMessage("path=/mouse/press&button=LEFT");
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        socketClient.sendMessage("path=/mouse/release&button=LEFT");
                        break;
                }
                return true;
            }

        });
    }

    private void initRightButton() {
        // right button
        right = findViewById(R.id.right);
        //noinspection AndroidLintClickableViewAccessibility
        right.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        socketClient.sendMessage("path=/mouse/press&button=RIGHT");
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        socketClient.sendMessage("path=/mouse/release&button=RIGHT");
                        break;
                }
                return true;
            }

        });
    }

    private void initWheel() {
        // wheel
        wheel = findViewById(R.id.wheel);
        //noinspection AndroidLintClickableViewAccessibility
        wheel.setOnTouchListener(new View.OnTouchListener() {
            float xDown, yDown, oldY, newX, newY, dy, dx;
            String direction;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldY = yDown = motionEvent.getY();
                        xDown = motionEvent.getX();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        newY = motionEvent.getY();
                        dy = newY - oldY;
                        if (Math.abs(dy) < 5) break;
                        oldY = newY;
                        if (dy < 0) {
                            direction = "UP";

                        } else if (dy > 0) {
                            direction = "DOWN";
                        }
                        socketClient.sendMessage("path=/mouse/wheel&direction=" + direction + "&amount=" + 1);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if (motionEvent.getX() == xDown && motionEvent.getY() == yDown) {
                            socketClient.sendMessage("path=/mouse/press&button=MIDDLE");
                            socketClient.sendMessage("path=/mouse/release&button=MIDDLE");
                        }
                        break;
                }
                return true;
            }

        });

    }

    private void initPad() {
        // pad
        pad = findViewById(R.id.pad);
        //noinspection AndroidLintClickableViewAccessibility
        pad.setOnTouchListener(new View.OnTouchListener() {
            float oldX, oldY, newX, newY, dx, dy;

            float xDown, yDown;

            double distance, temp, multiplier;

            @Override
            @SuppressWarnings("ClickableViewAccessibility")
            public boolean onTouch(View view, MotionEvent motionEvent) {
                StringBuilder sb = new StringBuilder();

                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldX = xDown = motionEvent.getX();
                        oldY = yDown = motionEvent.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        newX = motionEvent.getX();
                        newY = motionEvent.getY();
                        dx = newX - oldX;
                        dy = newY - oldY;
                        oldX = newX;
                        oldY = newY;

                        distance = Math.sqrt(dx * dx + dy * dy);
                        multiplier = getMultiplier(6, 18, distance);
                        dx *= multiplier;
                        dy *= multiplier;

                        sb.append("path=/mouse/move&dx=").append((int) dx).
                                append("&dy=").append((int) dy);
                        socketClient.sendMessage(sb.toString());
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        if (motionEvent.getX() == xDown && motionEvent.getY() == yDown) {
                            socketClient.sendMessage("path=/mouse/press&button=LEFT");
                            socketClient.sendMessage("path=/mouse/release&button=LEFT");
                        }
                        break;
                }
                return true;
            }
        });
    }

    private void initAddressEditText() {
        txtAddress = findViewById(R.id.txtAddress);
        txtAddress.setText(mainActivity.getPreferences(MODE_PRIVATE).getString("hostAddress", "192.168.1.2"));
        txtAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String address = editable.toString();
                mainActivity.getPreferences(MODE_PRIVATE).
                        edit().putString("hostAddress", address).apply();
                socketClient.setRemoteAddress(address);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });
    }

    private void initPortEditText() {

        txtPort = findViewById(R.id.txtPort);
        txtPort.setText(String.valueOf(mainActivity.getPreferences(MODE_PRIVATE).getInt("hostPort", 5000)));
        txtPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                int port = Integer.parseInt(editable.toString());
                mainActivity.getPreferences(MODE_PRIVATE).
                        edit().putInt("hostPort", port).apply();
                socketClient.setRemotePort(port);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
        });
    }

    private void createSocketClient() {
        String hostAddress = mainActivity.getPreferences(MODE_PRIVATE).getString("hostAddress", "192.168.1.2");
        int port = mainActivity.getPreferences(MODE_PRIVATE).getInt("hostPort", 5000);
        socketClient = new SocketClient(hostAddress, port, txtOut);

    }


    private void restartSocketClient() {
        if(socketClient==null)return;
        Log.e("main", "restart method");
        socketClient.stop();
        String hostAddress = mainActivity.getPreferences(MODE_PRIVATE).getString("hostAddress", "192.168.1.2");
        int port = mainActivity.getPreferences(MODE_PRIVATE).getInt("hostPort", 5000);
        socketClient.setRemoteAddress(hostAddress);
        socketClient.setRemotePort(port);
        socketClient.start();
    }
}
