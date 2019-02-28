package Remoter;

import User.Client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class ChatRoomClient {
    //服务器IP地址
    private static String SERVER_IP = "39.108.10.155";
    //服务器端口号
    private static final int PORT = 6666;
    //通信服务
    private Socket core = null;
    //基于通信服务的输出端
    private PrintWriter print = null;
    //基于通信服务的输入端
    private BufferedReader reader = null;
    //该用户
    private Client client = null;
    //本地读取输入端
    private BufferedReader localWord = null;

    public ChatRoomClient(String SERVER_IP){
        try{

            ChatRoomClient.SERVER_IP = SERVER_IP;

            //初始化Socket，与服务端建立连接
            this.core = new Socket(SERVER_IP,PORT);
            //初始用户
            this.client = new Client(this.core);
            //初始该客户端到服务端的输出流
            this.print = new PrintWriter(this.core.getOutputStream());
            //初始该客户端到服务端的输入流
            this.reader = new BufferedReader(new InputStreamReader(this.core.getInputStream(),"utf-8"));
            //初始本地输入流
            this.localWord = new BufferedReader(new InputStreamReader(System.in));
        }catch (IOException e){
            System.out.println("远程服务器无法连接...");
        }
    }
    //处理消息接收
    class MsgHandler implements Runnable{

        @Override
        public void run() {
            try{
                while(true) {
                    //消息接收
                    String back = reader.readLine();
                    if (back.equals("code:quit")) {
                        System.out.println(client.getUsername() + "您已经退出聊天室!");
                        break;
                    } else {
                        //设置了用户名才能读取消息
                        if (client.getUsername() != null || client.getUsername() != ""){
                            System.out.println(back);
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try{
                    reader.close();
                    print.close();
                    core.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
    //主线程
    public void onService(){
        //初始化数据接收线程
        Thread thread = new Thread(new MsgHandler());
        thread.start();

        try{
            //自定义用户名，并发送到服务器
            defineUsername();
            sendMsg(0,this.client.getUsername());

            while(true){
                String buffer = this.localWord.readLine();
                if (buffer.equals("quit")){
                    sendMsg(2,"quit");
                    break;
                }else {
                    sendMsg(1,buffer);
                }
            }
        }catch (IOException e){
            System.out.println("IO 错误！");
        }
    }
    //发送数据到服务端，code操作码，msg主要信息
    //0->名字定义
    //1->消息发送
    //2->退出
    public void sendMsg(int code,String msg){
        this.print.write(code + msg);
        this.print.write("\n");
        this.print.flush();
    }
    public void defineUsername() throws IOException {
        //定义名字的时候锁住控制台输出
        //这样在还未定义名字时，将无法接收其他用户发来的消息
        synchronized (System.out){
            System.out.print("输入你的用户名：");
            String username = this.localWord.readLine();
            this.client.setUsername(username);
        }
    }

}
