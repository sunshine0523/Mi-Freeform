package com.sunshine.freeform;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.sunshine.freeform.bean.KeyEventBean;
import com.sunshine.freeform.bean.MotionEventBean;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author sunshine
 * @date 2021/3/13
 */
public class SocketServer {

    private static final int SOCKET_PORT = 12065;
    private static final int LOG_SOCKET_PORT = 10248;

    private ServerSocket serverSocket;
    private ServerSocket logServerSocket;

    private Context context;

    private InputManager inputManager;

    private final Handler handler;

    SocketServer() {
        Looper.prepare();

        handler = new Handler();

        try {
            inputManager = new ServiceManager().getInputManager();
            serverSocket = new ServerSocket(SOCKET_PORT);
            logServerSocket = new ServerSocket(LOG_SOCKET_PORT);
            initSocket();
        } catch (IOException e) {
            MyLog.e("init SocketServer " + e);
        }

        Looper.loop();
    }

    private void initSocket() {
        new Thread(() -> {
            Socket mainSocket = null;
            while (true) {
                try {
                    MyLog.d("start listener mainSocket");
                    mainSocket = serverSocket.accept();

                    MyLog.d("connect " + mainSocket.toString());

                    new OISThread(mainSocket).start();

                } catch (IOException e) {
                    MyLog.e("initSocket " + e);
                }
            }
        }, "initSocket").start();

        new Thread(() -> {
            Socket logSocket = null;
            while (true) {
                try {
                    logSocket = logServerSocket.accept();

                    ObjectOutputStream oos = new ObjectOutputStream(logSocket.getOutputStream());
                    oos.writeObject(MyLog.getLog());
                    oos.writeObject(null);
                    oos.flush();
                    oos.close();
                    logSocket.close();
                } catch (IOException e) {
                    MyLog.e("init SocketServer " + e);
                }
            }
        }, "initLogSocket").start();
    }

    private void execCommand(String command) {
        handler.post(() -> {
            try {
                Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                MyLog.e("execCommand " + e);
            }
        });

    }

    private void toInjectMotionEvent(MotionEventBean motionEventBean) {
        int count = motionEventBean.getXArray().length;

        MotionEvent.PointerCoords[] pointerCoords = new MotionEvent.PointerCoords[count];
        MotionEvent.PointerProperties[] pointerProperties = new MotionEvent.PointerProperties[count];

        for (int i = 0; i < count; i++) {
            pointerProperties[i] = new MotionEvent.PointerProperties();
            pointerProperties[i].id = i;
            pointerProperties[i].toolType = MotionEvent.TOOL_TYPE_FINGER;

            pointerCoords[i] = new MotionEvent.PointerCoords();
            pointerCoords[i].orientation = 0f;
            pointerCoords[i].pressure = 1f;
            pointerCoords[i].size = 1f;
            pointerCoords[i].x = motionEventBean.getXArray()[i];
            pointerCoords[i].y = motionEventBean.getYArray()[i];
        }

        MotionEvent motionEvent = MotionEvent.obtain(
                SystemClock.uptimeMillis(),
                SystemClock.uptimeMillis(),
                motionEventBean.getAction(),
                count,
                pointerProperties,
                pointerCoords,
                0,
                0,
                1.0f,
                1.0f,
                -1,
                0,
                motionEventBean.getSource(),
                motionEventBean.getFlags()
        );

        handler.post(() -> {
            InputManager.Companion.setDisplayId(motionEvent, motionEventBean.getDisplayId());
            inputManager.injectInputEvent(motionEvent, 0);
            motionEvent.recycle();
        });

    }

    private void toInjectKeyEvent(KeyEventBean keyEventBean) {
        KeyEvent down = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0);
        KeyEvent up = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0);

        handler.post(() -> {
            try {
                InputManager.Companion.setDisplayId(down, keyEventBean.getDisplayId());
                KeyEvent.class.getMethod("setSource", int.class).invoke(down, InputDevice.SOURCE_KEYBOARD);
                inputManager.injectInputEvent(down, 0);

                InputManager.Companion.setDisplayId(up, keyEventBean.getDisplayId());
                KeyEvent.class.getMethod("setSource", int.class).invoke(up, InputDevice.SOURCE_KEYBOARD);
                inputManager.injectInputEvent(up, 0);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                MyLog.e("toInjectKeyEvent " + e);
            }
        });
    }

    private void moveStack(int displayId) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                String stackId = ShellUtils.execCommand("am stack list | grep displayId=" + displayId, false).successMsg.split(" ")[1].replace("id=", "");
                ShellUtils.execCommand("am display move-stack " + stackId + " 0", false);
            }
        });
    }

    class OISThread extends Thread {

        private final Socket socket;

        OISThread(Socket socket) {
            super("OISThread");
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                Object object;
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                while (true) {
                    while ((object = objectInputStream.readObject()) != null) {
                        if (object instanceof MotionEventBean) {
                            toInjectMotionEvent((MotionEventBean) object);
                        } else if (object instanceof String) {
                            if (object == "exit") {
                                objectInputStream.close();
                                socket.close();
                                interrupt();
                                return;
                            }
                            //移动栈标记
                            if (((String) object).contains("move-stack")) {
                                moveStack(Integer.parseInt(((String) object).split("#")[1]));
                            } else {
                                execCommand((String) object);
                            }
                        } else if (object instanceof KeyEventBean) {
                            toInjectKeyEvent((KeyEventBean) object);
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                MyLog.e("OISThread " + e);
            }
        }
    }
}
