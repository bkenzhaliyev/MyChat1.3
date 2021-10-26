package Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class ClientHandler {
    Socket socket;
    Server server;
    DataInputStream in;
    DataOutputStream out;
    private static final Logger logger = Logger.getLogger(Server.class.getName());

    private boolean authenticated;
    private String nickname;
    private String login;

    public ClientHandler(Socket socket, Server server) {

        LogManager manager = LogManager.getLogManager();
        try {
            manager.readConfiguration(new FileInputStream("logging.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            this.socket = socket;
            this.server = server;

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            ExecutorService executorService = Executors.newFixedThreadPool(4);

//            new Thread(() -> {
            executorService.execute(() -> {
                try {
                    // отключение соккета по таймауту
                    socket.setSoTimeout(120000);

                    // цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.equals("/end")) {
                            sendMsg("/end");
                            logger.log(Level.INFO, "Client disconnected", true);
//                            System.out.println("Client disconnected");
                            break;
                        }
                        if (str.startsWith("/auth ")) {
                            String[] token = str.split("\\s+");
                            nickname = server.getAuthService()
                                    .getNicknameByLoginAndPassword(token[1], token[2]);
                            login = token[1];
                            if (nickname != null) {
                                if (!server.isLoginAuthenticated(login)) {
                                    sendMsg("/authok " + nickname);
                                    server.subscribe(this);
                                    authenticated = true;
                                    break;
                                } else {
                                    logger.log(Level.INFO, "Пользователь с таким логином уже авторизован: " + nickname, true);
//                                    sendMsg("Пользователь с таким логином уже авторизован...");
                                }
                            } else {
                                logger.log(Level.INFO, "Неверный логин/пароль " + login, true);
//                                sendMsg("Неверный логин/пароль");
                            }
                        }

                        if (str.startsWith("/reg ")) {
                            String[] token = str.split("\\s+");
                            if (token.length < 4) {
                                continue;
                            }

                            boolean regOk = server.getAuthService().
                                    registration(token[1], token[2], token[3]);
                            if (regOk) {
                                sendMsg("/regok");
                            } else {
                                sendMsg("/regno");
                            }
                        }
                    }
                    // цикл работы
                    while (authenticated) {
                        socket.setSoTimeout(0);
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
//                            Выход из чата
                            if (str.equals("/end")) {
                                sendMsg("/end");
                                logger.log(Level.INFO, "Client disconnected", true);
//                                System.out.println("Client disconnected");
                                break;
//                          Отправка сообщения конкретному пользователью
                            } else if (str.startsWith("/w")) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }
                        } else {
//                      Отправка сообщения всем пользователям
                            server.broadcastMsg(this, str);
                        }

                    }
                } catch (SocketTimeoutException e){
//                    System.out.println("Истекло время ожидания авторизации...");
                    logger.log(Level.INFO, "Истекло время ожидания авторизации...", true);
                    sendMsg("/end");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }


    public String getLogin() {
        return login;
    }
}