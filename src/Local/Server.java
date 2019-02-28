package Local;

import User.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class Server {
    //服务器端口号
    private static final int port = 6666;

    //最大用户数
    private static final int MAXUSERS = 256;

    //单例模式
    private static Server server = null;

    //核心服务
    private ServerSocket core = null;

    // 存放消息基于线程安全的队列
    private BlockingQueue<String> msgQueue = null;

    //存放客户
    private ArrayList<ClientTask> clientTasks = null;

    private Server(){
        try{
            //初始化服务Socket,监听端口
            this.core = new ServerSocket(this.port);
            //初始化消息队列
            this.msgQueue = new ArrayBlockingQueue<String>(20);
            //初始化客户
            clientTasks = new ArrayList<>();
        }catch (IOException IOE){
            System.out.println("端口被占用，请关闭端口再使用！");
        }

    }

    public static Server getInstarnce(){
        if (server == null){
            System.out.println("服务初始化中...");
            server = new Server();
        }
        return server;
    }

    //接收消息，收到一个连接就新开一个线程去处理
    public void OnService(){
        System.out.println("消息队列初始化中...");
        //初始化处理消息队列线程
        Thread [] threadOfMsg = new Thread[5];
        for (int i = 0 ; i < 5 ; i++){
            threadOfMsg[i] = new Thread(new MsgHandler());
            threadOfMsg[i].start();
        }

        Socket tempClient;

        while(true){
            try{
                //接收连接
                tempClient = this.core.accept();
                //检测是否超过上限值
                boolean mark = isAvailable();
                //超过无法加入聊天室
                if (!mark){
                    continue;
                }
                //初始化客户任务
                ClientTask clientTask = new ClientTask(new Client(tempClient),this.msgQueue);
                //加入客户列表
                this.clientTasks.add(clientTask);
                //新的客户线程
                Thread threadOfClientTask = new Thread(clientTask);
                //使用新的客户线程执行
                threadOfClientTask.start();
            }catch (IOException IOE){
                System.out.println("IO 错误!");
            }

        }
    }
    //检测是否达到最大连接数
    private boolean isAvailable(){
        if (this.clientTasks.size() <= MAXUSERS) {
            return true;
        }else {
            return false;
        }
    }
    //处理消息转发
    class MsgHandler implements Runnable {
        @Override
        public void run() {
            while(true){
                String msg = null;
                try{
                    msg = msgQueue.take();
                    if (msg != null){
                        System.out.println(msg);
                        for (ClientTask clientTask :clientTasks){
                            clientTask.sendMsg(msg);
                        }
                    }
                }catch (InterruptedException e){
                    System.out.println("阻塞队列出错...");
                }
            }
        }
    }
    //处理消息接收
    class ClientTask implements Runnable {
        //用户
        private Client client ;

        //字符缓冲区读取
        private BufferedReader reader = null;

        //字符打印流
        private PrintWriter print = null;

        //消息队列
        private BlockingQueue<String> msgQueue = null;

        //初始化需要客户和消息队列
        ClientTask(Client client,BlockingQueue<String> msgQueue){
            //初始化客户端
            this.client = client;
            //得到服务的消息队列
            this.msgQueue = msgQueue;
            try{
                //服务器对该客户的数据读取端
                this.reader = new BufferedReader(new InputStreamReader(this.client.getSocket().getInputStream(),"UTF-8"));
                //服务器对该客户的数据输出端
                this.print = new PrintWriter(this.client.getSocket().getOutputStream());
            }catch (Exception e){
                System.out.println("数据输出/输入端错误...");
            }
        }
        //发送消息
        public synchronized void sendMsg(String msg){
            print.write(msg);
            print.write("\n");
            print.flush();
        }

        public Client getClient(){
            return client;
        }

        @Override
        public void run() {
            try{
                //死循环接收客户端传来的消息
                while(true){
                    //获取客户用户名字
                    String username = this.client.getUsername();

                    if (username == null || username == ""){
                        username = "Someone";
                    }
                    //读取传来数据
                    String buffer = this.reader.readLine();
                    //退出标志
                    boolean quitflag = false;

                    //这样规定：第一个字符数字为操作码
                    // 0->初始化，传来用户名字
                    // 1->消息
                    // 2->退出
                    //截取操作码
                    int operation = Integer.parseInt(buffer.substring(0,1));
                    //截取消息
                    String msg =  buffer.substring(1);
                    //加入消息队列消息
                    switch (operation){
                        case 0:
                            this.client.setUsername(msg);
                            this.msgQueue.put( "【" + this.client.getUsername() + "加入了聊天室】");
                            break;
                        case 1:
                            this.msgQueue.put(username + ":" + msg);
                            break;
                        case 2:
                            quitflag = true;
                            this.msgQueue.put("【" + username + "退出了聊天室】");
                            break;
                    }
                    if (quitflag){
                        break;
                    }
                }
            }catch (Exception e){
                System.out.println("读取用户消息数据错误...");
            }finally {
                try {
                    //从客户队列中移除
                    clientTasks.remove(this);
                    //给客户端发送退出消息
                    sendMsg("code:quit");
                    reader.close();
                    print.close();
                    this.client.getSocket().close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
