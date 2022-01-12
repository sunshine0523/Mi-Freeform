package com.sunshine.freeform.utils;

import android.content.Context;
import android.view.WindowManager;
import android.widget.Button;

import com.sunshine.freeform.bean.FreeFormBean;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author sunshine
 * @date 2021/3/12
 */
public class SocketServer {

    private Context context;

    private static final int SOCKET_PORT = 12065;

    SocketServer(Context context) {
        this.context = context;

        initSocketServer();
    }

    private void initSocketServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(SOCKET_PORT);

                    while (true) {
                        Socket socket = serverSocket.accept();

                        System.out.println("连接成功" + socket.toString());

                        try (ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream())) {
                            FreeFormBean freeFormBean;
                            while ((freeFormBean = (FreeFormBean) objectInputStream.readObject()) != null) {
                                System.out.println(freeFormBean);

                                WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                                WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                                layoutParams.type = 2026;
                                Button button = new Button(context);
                                windowManager.addView(button, layoutParams);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            System.out.println("流异常：" + e);
                        }
                        socket.close();
                    }
                } catch (IOException e) {
                    System.out.println("服务端异常：" + e);
                }
            }
        }, "initSocketServerThread").start();
    }
}
