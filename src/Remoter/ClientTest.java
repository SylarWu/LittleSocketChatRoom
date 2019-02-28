package Remoter;

import java.util.Scanner;

public class ClientTest {
    public static void main(String args[]){
        Scanner scanner = new Scanner(System.in);

        System.out.print("输入远程服务器IP地址：");

        String IP_ADDRESS = scanner.nextLine();

        ChatRoomClient client = new ChatRoomClient(IP_ADDRESS);

        client.onService();
    }
}
