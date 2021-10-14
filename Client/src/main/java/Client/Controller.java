package Client;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller  implements Initializable {
    @FXML
    public TextArea MsgArea;
    @FXML
    public TextField MsgField;
    @FXML
    public Button SendMsg;
    @FXML
    public HBox authPanel;
    @FXML
    public HBox msgPanel;
    @FXML
    public ListView clientList;

    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField loginField;

    private Socket socket;
    private final int PORT = 8188;
    private final String IP_ADDRESS = "localhost";
    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nickname;
    private Stage stage;
    private Stage regStage;
    private regController regController;

@Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(() -> {
            stage = (Stage) MsgField.getScene().getWindow();
            stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    MsgArea.clear();
                    System.out.println("bye");
                    if (socket != null && !socket.isClosed()) {
                        try {
                            out.writeUTF("/end");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    // цикл аутентификации
                    while (true) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/authok")) {
                                nickname = str.split("\\s")[1];
                                setAuthenticated(true);
                                break;
                            }
                            if (str.equals("/regok")) {
                                regController.regResult("Регистрация прошла успешно");
                            }
                            if (str.equals("/regno")) {
                                regController.regResult("Логин или никнейм уже заняты");
                            }
                        } else {
                            MsgArea.appendText(str + "\n");
                        }
                    }
                    // цикл работы
                    while (authenticated) {
                        String str = in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/clientlist ")) {
                                String[] token = str.split("\\s+");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                        } else {
                            MsgArea.appendText(str + "\n");
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("disconnected");
                    setAuthenticated(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnSendMsg(ActionEvent actionEvent) {
        try {
            if(MsgField.getText() != ""){
                out.writeUTF(MsgField.getText());
                MsgField.clear();
                MsgField.requestFocus();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        String login = loginField.getText().trim();
        String password = passwordField.getText().trim();
        String msg = String.format("/auth %s %s", login, password);

        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nickname) {
        Platform.runLater(() -> {
            if (!nickname.equals("")) {
                stage.setTitle(String.format("SAP чат[ %s ]", nickname));
            } else {
                stage.setTitle("SAP чат");
            }
        });
    }

    public void js2controller(ObservableValue observable, String oldValue, String newValue) {
        if (!newValue.isEmpty()) {
            SendMsg.setDisable(false);
        } else {
            SendMsg.setDisable(true);
        }
    }

    public void btnClearChat(ActionEvent actionEvent) {
        MsgArea.clear();
    }

    public void clickClose(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            Stage stage = (Stage) SendMsg.getScene().getWindow();
            stage.close();
        });
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clientList.setVisible(authenticated);
        clientList.setManaged(authenticated);

        if (!authenticated) {
            nickname = "";
        }
        setTitle(nickname);
        MsgArea.clear();
    }

    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("../../resources/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("SAP чат, регистрация...");
            regStage.setScene(new Scene(root, 600, 400));
            regController = fxmlLoader.getController();
            regController.setController(this);

            regStage.initStyle(StageStyle.UTILITY);
            regStage.initModality(Modality.APPLICATION_MODAL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showRegWindow(ActionEvent actionEvent) {
        if (regStage == null) {
            createRegWindow();
        }
        regStage.show();
    }

    public void registration(String login, String password, String nickname){
        String msg = String.format("/reg %s %s %s",login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clientListClick(MouseEvent mouseEvent) {
        String receiver = (String) clientList.getSelectionModel().getSelectedItem();
        MsgField.setText(String.format("/w %s ", receiver));
    }
}
