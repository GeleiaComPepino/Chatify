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

import java.util.HashMap;
import java.util.Map;

public class SimpleChatApp extends Application {

    private BorderPane mainLayout;
    private Scene chatScene;
    private Stage primaryStage;
    private VBox sidebar;
    private final String[] Lista = {"Gustavo", "Davi", "Daniel", "Rafael", "Luiz", "Lucas"};
    private final Map<String, VBox> userMessagesMap = new HashMap<>();
    private final Map<String, HBox> userWelcomeBoxMap = new HashMap<>();

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        createChatScene();

        primaryStage.setTitle("Chattify");
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

        Label mensagensLabel = new Label("Mensagens");
        mensagensLabel.setFont(new Font(16));

        TextField searchField = new TextField();
        searchField.setPromptText("Pesquisar chat");

        VBox chatList = new VBox(5);
        for (String user : Lista) {
            HBox chatItem = new HBox(10);
            chatItem.setPadding(new Insets(5));
            chatItem.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dddddd; cursor: hand;");
            String imageUrl = "https://api.dicebear.com/9.x/fun-emoji/png?seed=" + user;
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

        sidebarBox.getChildren().addAll(mensagensLabel, searchField, chatList);
        return sidebarBox;
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
            VBox chatPanel = buildChatPanel(userMessages, name, imageUrl);
            mainLayout.setCenter(chatPanel);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}