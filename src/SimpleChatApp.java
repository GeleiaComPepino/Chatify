import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SimpleChatApp extends Application {

    private BorderPane mainLayout;
    private Scene chatScene;
    private Stage primaryStage;
    private VBox sidebar;
    private final Map<String, VBox> userMessagesMap = new HashMap<>();
    private final Map<String, HBox> userWelcomeBoxMap = new HashMap<>();

    private static final String DB_URL = "jdbc:mysql://localhost:3306/chatify?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        createChatScene();
        primaryStage.getIcons().add(new Image("https://cdn-icons-png.flaticon.com/512/5962/5962463.png"));
        primaryStage.setTitle("Chatify");
        primaryStage.setScene(chatScene);
        primaryStage.show();
    }

    private void createChatScene() {
        mainLayout = new BorderPane();

        sidebar = buildSidebar();
        VBox initialPanel = buildInitialPanel();

        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(initialPanel);

        chatScene = new Scene(mainLayout, 1080, 720);
    }

    private VBox buildSidebar() {
        VBox sidebarBox = new VBox(10);
        sidebarBox.setPadding(new Insets(10));
        sidebarBox.setPrefWidth(250);
        sidebarBox.setStyle("-fx-background-color: #f9f9fb;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView icon = new ImageView(new Image("https://cdn-icons-png.flaticon.com/512/5962/5962463.png"));
        icon.setFitWidth(20);
        icon.setFitHeight(20);

        Label mensagensLabel = new Label("Mensagens");
        mensagensLabel.setFont(new Font(16));

        Button addButton = new Button("Adicionar");
        addButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand;");
        addButton.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Adicionar Usuário");
            dialog.setHeaderText(null);
            dialog.setContentText("Digite o email do usuário:");
            dialog.showAndWait().ifPresent(email -> {
                if (!userMessagesMap.containsKey(email)) {
                    addUserToDatabase(email);
                    addUserToSidebar(email);
                }
            });
        });

        header.getChildren().addAll(icon, mensagensLabel, new Region(), addButton);
        HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("Pesquisar chat");

        VBox chatList = new VBox(5);
        loadUsersFromDatabase(chatList);

        sidebarBox.getChildren().addAll(header, searchField, chatList);
        return sidebarBox;
    }

    private void addUserToSidebar(String email) {
        VBox chatList = (VBox) sidebar.getChildren().get(2);
        addUserToChatList(chatList, email);
    }

    private void addUserToChatList(VBox chatList, String user) {
        HBox chatItem = new HBox(10);
        chatItem.setPadding(new Insets(5));
        chatItem.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dddddd; cursor: hand;");
        String imageUrl = "https://api.dicebear.com/9.x/fun-emoji/png?seed=" + user.replaceAll(" ", "") + "&radius=50";
        ImageView profile = new ImageView(new Image(imageUrl));
        profile.setFitWidth(40);
        profile.setFitHeight(40);
        VBox chatInfo = new VBox(2);
        chatInfo.getChildren().addAll(new Label(user), new Label("Clique para abrir"));
        Label time = new Label("00:00:00");
        chatItem.getChildren().addAll(profile, chatInfo, new Region(), time);
        HBox.setHgrow(chatItem.getChildren().get(2), Priority.ALWAYS);
        chatItem.setOnMouseClicked(e -> openChat(user, imageUrl));

        chatList.getChildren().add(chatItem);

        VBox messageContainer = new VBox(10);
        messageContainer.setPadding(new Insets(10));
        messageContainer.setStyle("-fx-background-color: #f4f6fc; -fx-background-radius: 10;");
        messageContainer.setPrefHeight(400);
        Label welcome = new Label("Inicie uma conversa com " + user + ".");
        welcome.setPadding(new Insets(10));
        welcome.setStyle("-fx-background-color: #ddd; -fx-background-radius: 10;");
        HBox welcomeBox = new HBox(welcome);
        welcomeBox.setAlignment(Pos.CENTER);
        messageContainer.getChildren().add(welcomeBox);

        userMessagesMap.put(user, messageContainer);
        userWelcomeBoxMap.put(user, welcomeBox);
    }

    private VBox buildInitialPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #ffffff;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        ImageView icon = new ImageView(new Image("https://cdn-icons-png.flaticon.com/512/5962/5962463.png"));
        icon.setFitWidth(40);
        icon.setFitHeight(40);
        Label title = new Label("Chattify\nBem-vindo!");
        header.getChildren().addAll(icon, title);

        VBox msgBox = new VBox();
        msgBox.setPadding(new Insets(10));
        msgBox.setStyle("-fx-background-color: #f4f6fc; -fx-background-radius: 10;");
        msgBox.setPrefHeight(400);
        Label placeholder = new Label("Selecione um chat na esquerda para começar.");
        placeholder.setPadding(new Insets(10));
        placeholder.setStyle("-fx-background-color: #ddd; -fx-background-radius: 10;");
        HBox placeholderBox = new HBox(placeholder);
        placeholderBox.setAlignment(Pos.CENTER);
        msgBox.getChildren().add(placeholderBox);

        panel.getChildren().addAll(header, new Separator(), msgBox);
        return panel;
    }

    private VBox buildChatPanel(VBox userMessages, String name, String imageUrl) {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));
        ImageView img = new ImageView(new Image(imageUrl));
        img.setFitWidth(40);
        img.setFitHeight(40);
        Label nameLabel = new Label(name + "\n#ID" + name.hashCode());
        header.getChildren().addAll(img, nameLabel);

        HBox messageInput = new HBox(10);
        messageInput.setPadding(new Insets(10));
        messageInput.setAlignment(Pos.CENTER);
        TextField messageField = new TextField();
        messageField.setPromptText("Digite a mensagem");
        Button sendButton = new Button("Enviar");

        sendButton.setOnAction(e -> {
            String text = messageField.getText();
            if (!text.isEmpty()) {
                VBox messages = userMessages;
                HBox welcomeBox = userWelcomeBoxMap.get(name);
                if (messages.getChildren().contains(welcomeBox)) {
                    messages.getChildren().remove(welcomeBox);
                }

                Label msg = new Label(text);
                msg.setWrapText(true);
                msg.setPadding(new Insets(10));
                msg.setStyle("-fx-background-color: #e3e3ff; -fx-background-radius: 10;");
                HBox message = new HBox(msg);
                message.setAlignment(Pos.CENTER_RIGHT);
                messages.getChildren().add(message);

                saveMessageToDatabase("Me", name, text);

                messageField.clear();
            }
        });

        messageInput.getChildren().addAll(messageField, sendButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        VBox chatPanel = new VBox(10);
        chatPanel.setPadding(new Insets(10));
        chatPanel.setStyle("-fx-background-color: #ffffff;");
        chatPanel.getChildren().addAll(header, new Separator(), userMessages, messageInput);

        return chatPanel;
    }

    private void openChat(String name, String imageUrl) {
        VBox userMessages = userMessagesMap.get(name);
        if (userMessages != null) {
            userMessages.getChildren().clear();

            HBox welcomeBox = userWelcomeBoxMap.get(name);
            userMessages.getChildren().add(welcomeBox);

            loadMessagesFromDatabase("Me", name, userMessages, welcomeBox);

            VBox chatPanel = buildChatPanel(userMessages, name, imageUrl);
            mainLayout.setCenter(chatPanel);
        }
    }

    private void addUserToDatabase(String email) {
        String sql = "INSERT INTO chatifyusuarios (id, name, email) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String id = UUID.randomUUID().toString().substring(0, 7);
            stmt.setString(1, id);
            stmt.setString(2, email);  // Using email as name here, adjust as needed
            stmt.setString(3, email);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadUsersFromDatabase(VBox chatList) {
        String sql = "SELECT name, email FROM chatifyusuarios";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String name = rs.getString("name");
                String email = rs.getString("email");
                if (email != null && !email.trim().isEmpty()) {
                    addUserToChatList(chatList, name);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveMessageToDatabase(String sender, String receiver, String message) {
        String sql = "INSERT INTO chatifymessages (sender_name, receiver_name, message) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, message);
            stmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadMessagesFromDatabase(String sender, String receiver, VBox messageBox, HBox welcomeBox) {
        String sql = "SELECT sender_name, message FROM chatifymessages " +
                "WHERE (sender_name = ? AND receiver_name = ?) " +
                "OR (sender_name = ? AND receiver_name = ?) " +
                "ORDER BY id ASC";  // assuming messages increment by id

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, sender);
            stmt.setString(2, receiver);
            stmt.setString(3, receiver);
            stmt.setString(4, sender);

            ResultSet rs = stmt.executeQuery();
            boolean hasMessages = false;

            while (rs.next()) {
                hasMessages = true;
                String from = rs.getString("sender_name");
                String msgText = rs.getString("message");

                Label msg = new Label(msgText);
                msg.setWrapText(true);
                msg.setPadding(new Insets(10));
                msg.setStyle("-fx-background-radius: 10;");
                HBox msgBox = new HBox(msg);

                if (from.equals(sender)) {
                    msg.setStyle("-fx-background-color: #e3e3ff; -fx-background-radius: 10;");
                    msgBox.setAlignment(Pos.CENTER_RIGHT);
                } else {
                    msg.setStyle("-fx-background-color: #d9ffd9; -fx-background-radius: 10;");
                    msgBox.setAlignment(Pos.CENTER_LEFT);
                }

                messageBox.getChildren().add(msgBox);
            }

            if (hasMessages) {
                messageBox.getChildren().remove(welcomeBox);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
