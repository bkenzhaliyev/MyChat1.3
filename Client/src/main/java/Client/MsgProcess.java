package Client;

import java.io.*;
import java.util.ArrayList;

public class MsgProcess {
    public static void writeMessage(String login, String msg){
        String file = "1/" + login + ".txt";
        try {
            fileF(file, msg);
        } catch(IOException e){
            System.out.println("Ошибка при записи файла");
        }
    }

    public static void fileF(String file, String msg) throws IOException {
        // Запись строки в файл в кодировке UTF-8
        DataOutputStream dataOutput = new DataOutputStream(new FileOutputStream(file, true));
        dataOutput.writeUTF(msg.toString() + "\n");
        dataOutput.close();
    }

    public static ArrayList<String> readMessage(String file) throws IOException, ClassNotFoundException {
        ArrayList<String> msgList = new ArrayList<>();
        DataInputStream dataInput = new DataInputStream(new FileInputStream(file));
        System.out.println("Read data from file...");
        while (dataInput.available() > 0) {
            String a = dataInput.readUTF();
            msgList.add(a);
        }
        dataInput.close();
        return msgList;
    }
}
