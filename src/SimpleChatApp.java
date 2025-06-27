import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Popup;
import javafx.stage.Stage;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

public class SimpleChatApp extends Application {

    private BorderPane mainLayout;
    private Scene chatScene;
    private Stage primaryStage;
    private VBox sidebar;
    private final Map<String, VBox> userMessagesMap = new HashMap<>();
    private final Map<String, HBox> userWelcomeBoxMap = new HashMap<>();
    private String currentUserEmail;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/chatify?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private String currentUserId;
    private Scene loginScene;
    private final Map<String, String> userIdMap = new HashMap<>();
    private String currentChatUserId;
    private Timeline messageRefreshTimeline;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        showLoginScene();
        primaryStage.getIcons().add(new Image("https://cdn-icons-png.flaticon.com/512/5962/5962463.png"));
        primaryStage.setTitle("Chatify");
        primaryStage.show();
    }

    private void showLoginScene() {
        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(20));
        loginLayout.setAlignment(Pos.CENTER);

        // Header with logo and title
        VBox headerBox = new VBox(5);
        headerBox.setAlignment(Pos.CENTER);
        ImageView logo = new ImageView(new Image("https://cdn-icons-png.flaticon.com/512/5962/5962463.png"));
        logo.setFitWidth(60);
        logo.setFitHeight(60);
        Label title = new Label("Chattify");
        title.setFont(new Font("Arial", 28));
        headerBox.getChildren().addAll(logo, title);

        TextField nameField = new TextField();
        nameField.setPromptText("Nome (somente para registro)");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Senha");

        Button loginButton = new Button("Entrar");
        loginButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 5 20 5 20;");
        loginButton.setOnMouseEntered(e -> loginButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 5 20 5 20;"));
        loginButton.setOnMouseExited(e -> loginButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 5 20 5 20;"));
        Button registerButton = new Button("Criar Conta");
        registerButton.setStyle("-fx-background-color: #43A047; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 5 20 5 20;");
        registerButton.setOnMouseEntered(e -> registerButton.setStyle("-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 5 20 5 20;"));
        registerButton.setOnMouseExited(e -> registerButton.setStyle("-fx-background-color: #43A047; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 5 20 5 20;"));

        Label statusLabel = new Label();

        loginButton.setOnAction(e -> {
            String email = emailField.getText();
            String password = passwordField.getText();
            Task<String> loginTask = new Task<>() {
                @Override
                protected String call() {
                    return authenticateUser(email, password);
                }
            };
            loginTask.setOnSucceeded(ev -> {
                String userId = loginTask.getValue();
                if (userId != null) {
                    currentUserEmail = email;
                    currentUserId = userId;
                    createChatScene();
                    primaryStage.setScene(chatScene);
                } else {
                    statusLabel.setText("Email ou senha incorretos.");
                }
            });
            loginTask.setOnFailed(ev -> statusLabel.setText("Erro de conexão."));
            new Thread(loginTask).start();
        });

        registerButton.setOnAction(e -> {
            String name = nameField.getText();
            String email = emailField.getText();
            String password = passwordField.getText();
            Task<Boolean> registerTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return registerUser(name, email, password);
                }
            };
            registerTask.setOnSucceeded(ev -> {
                if (registerTask.getValue()) {
                    statusLabel.setText("Conta criada com sucesso. Faça login agora.");
                } else {
                    statusLabel.setText("Erro: conta já existe ou dados inválidos.");
                }
            });
            registerTask.setOnFailed(ev -> statusLabel.setText("Erro de conexão."));
            new Thread(registerTask).start();
        });

        loginLayout.getChildren().addAll(headerBox, nameField, emailField, passwordField, loginButton, registerButton, statusLabel);
        loginScene = new Scene(loginLayout, 400, 450);
        primaryStage.setScene(loginScene);
    }

    private String authenticateUser(String email, String password) {
        String sql = "SELECT id FROM chatifyusuarios WHERE email = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean registerUser(String name, String email, String password) {
        String checkSql = "SELECT * FROM chatifyusuarios WHERE email = ?";
        String insertSql = "INSERT INTO chatifyusuarios (id, name, email, password) VALUES (?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) return false;

            String id = UUID.randomUUID().toString().substring(0, 7);
            insertStmt.setString(1, id);
            insertStmt.setString(2, name);
            insertStmt.setString(3, email);
            insertStmt.setString(4, password);
            insertStmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createChatScene() {
        mainLayout = new BorderPane();

        sidebar = buildSidebar();
        VBox initialPanel = buildInitialPanel();

        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(initialPanel);

        chatScene = new Scene(mainLayout, 1080, 720);
    }

    private boolean userExistsInDatabase(String email) {
        final boolean[] result = {false};
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                String sql = "SELECT 1 FROM chatifyusuarios WHERE email = ?";
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, email);
                    ResultSet rs = stmt.executeQuery();
                    return rs.next();
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        };
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
        try {
            t.join();
            result[0] = task.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result[0];
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
                if (email.equals(currentUserEmail)) {
                    // Evita adicionar a si mesmo
                    return;
                }
                if (!userMessagesMap.containsKey(email) && userExistsInDatabase(email)) {
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
        // Find the username by email
        String userName = userIdMap.entrySet().stream().filter(entry -> entry.getValue().equals(email)).findFirst().map(Map.Entry::getKey).orElse(null);
        if (userName != null) {
            Platform.runLater(() -> addUserToChatList(chatList, userName, email));
        }
    }

    private void addUserToChatList(VBox chatList, String user, String email) {
        // Remove user if email matches currentUserEmail
        if (currentUserEmail != null && email.equals(currentUserEmail)) {
            // Remove any existing chat item for this user
            chatList.getChildren().removeIf(node -> {
                if (node instanceof HBox) {
                    HBox hbox = (HBox) node;
                    for (javafx.scene.Node child : hbox.getChildren()) {
                        if (child instanceof VBox) {
                            VBox vbox = (VBox) child;
                            for (javafx.scene.Node labelNode : vbox.getChildren()) {
                                if (labelNode instanceof Label) {
                                    Label label = (Label) labelNode;
                                    if (label.getText().equals(user)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            });
            return;
        }
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

        // Enable Ctrl+Scroll zoom on chat messages
        messageContainer.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double delta = event.getDeltaY() > 0 ? 2 : -2;
                for (javafx.scene.Node node : messageContainer.getChildren()) {
                    if (node instanceof HBox) {
                        for (javafx.scene.Node child : ((HBox) node).getChildren()) {
                            if (child instanceof Label) {
                                Label label = (Label) child;
                                double currentSize = label.getFont().getSize();
                                double newSize = Math.max(10, Math.min(40, currentSize + delta));
                                label.setFont(new javafx.scene.text.Font(label.getFont().getFamily(), newSize));
                            }
                        }
                    }
                }
                event.consume();
            }
        });
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
        
        // Emoji Button
        Button emojiButton = new Button("\uD83D\uDE00"); // 😀
        emojiButton.setStyle("-fx-background-color: #fff; -fx-border-color: #e0e0e0; -fx-background-radius: 30; -fx-border-radius: 30; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 5 12 5 12;");
        emojiButton.setOnMouseEntered(e -> emojiButton.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #bdbdbd; -fx-background-radius: 30; -fx-border-radius: 30; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 5 12 5 12;"));
        emojiButton.setOnMouseExited(e -> emojiButton.setStyle("-fx-background-color: #fff; -fx-border-color: #e0e0e0; -fx-background-radius: 30; -fx-border-radius: 30; -fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 5 12 5 12;"));
        
        // Modern Emoji Picker Popup
        emojiButton.setOnAction(e -> {
            Popup emojiPopup = new Popup();
            emojiPopup.setAutoHide(true);
            
            TabPane tabPane = new TabPane();
            tabPane.setStyle("-fx-background-radius: 16; -fx-border-radius: 16; -fx-background-color: #fff; -fx-border-color: #e0e0e0;");
            tabPane.setTabMinWidth(60);
            tabPane.setTabMaxWidth(120);
            tabPane.setTabMinHeight(30);
            tabPane.setTabMaxHeight(30);
            
            // Emoji categories (sample, can be expanded)
            String[][] emojiCategories = {
                {"Smileys", "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE09", "\uD83D\uDE0D", "\uD83D\uDE18", "\uD83D\uDE1C", "\uD83D\uDE22", "\uD83D\uDE2D", "\uD83D\uDE31", "\uD83D\uDE0E", "\uD83D\uDE12", "\uD83D\uDE44"},
                {"Hands", "\uD83D\uDC4D", "\uD83D\uDC4E", "\uD83D\uDC4C", "\uD83D\uDC4F", "\uD83D\uDC4B", "\uD83D\uDC50", "\uD83D\uDC46", "\uD83D\uDC47", "\uD83D\uDC48", "\uD83D\uDC49"},
                {"Animals", "\uD83D\uDC36", "\uD83D\uDC31", "\uD83D\uDC2D", "\uD83D\uDC39", "\uD83D\uDC30", "\uD83D\uDC3B", "\uD83D\uDC3C", "\uD83D\uDC28", "\uD83D\uDC2F", "\uD83D\uDC35"},
                {"Food", "\uD83C\uDF4E", "\uD83C\uDF53", "\uD83C\uDF49", "\uD83C\uDF4A", "\uD83C\uDF4D", "\uD83C\uDF52", "\uD83C\uDF50", "\uD83C\uDF47", "\uD83C\uDF48", "\uD83C\uDF4C"},
                {"Hearts", "\u2764\uFE0F", "\uD83D\uDC9B", "\uD83D\uDC99", "\uD83D\uDC9A", "\uD83D\uDC9C", "\uD83D\uDC94", "\uD83D\uDC97", "\uD83D\uDC98", "\uD83D\uDC93", "\uD83D\uDC95"}
            };
            String[] colors = {"#FFEB3B", "#FFCDD2", "#B2DFDB", "#BBDEFB", "#FFE0B2", "#E1BEE7", "#C8E6C9", "#F8BBD0", "#D1C4E9", "#FFF9C4"};
            int colorIdx = 0;
            for (String[] category : emojiCategories) {
                String tabName = category[0];
                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(10);
                grid.setPadding(new Insets(14));
                int col = 0, row = 0;
                for (int i = 1; i < category.length; i++) {
                    Button emBtn = new Button(category[i]);
                    String bgColor = colors[(colorIdx + i) % colors.length];
                    emBtn.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #e0e0e0; -fx-font-size: 28px; -fx-cursor: hand; -fx-padding: 10 16 10 16; -fx-effect: dropshadow(gaussian, #bbb, 4, 0.2, 0, 2);");
                    emBtn.setOnMouseEntered(ev -> emBtn.setStyle("-fx-background-color: #fff176; -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: #2196F3; -fx-font-size: 32px; -fx-cursor: hand; -fx-padding: 10 16 10 16; -fx-effect: dropshadow(gaussian, #90caf9, 8, 0.3, 0, 4); -fx-scale-x: 1.12; -fx-scale-y: 1.12;"));
                    emBtn.setOnMouseExited(ev -> emBtn.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-color: #e0e0e0; -fx-font-size: 28px; -fx-cursor: hand; -fx-padding: 10 16 10 16; -fx-effect: dropshadow(gaussian, #bbb, 4, 0.2, 0, 2);"));
                    emBtn.setOnAction(ev -> {
                        messageField.appendText(emBtn.getText());
                        emojiPopup.hide();
                        messageField.requestFocus();
                    });
                    grid.add(emBtn, col, row);
                    col++;
                    if (col > 4) { col = 0; row++; }
                }
                colorIdx++;
                Tab tab = new Tab(tabName, grid);
                tab.setClosable(false);
                tabPane.getTabs().add(tab);
            }
            StackPane popupContent = new StackPane(tabPane);
            popupContent.setStyle("-fx-background-radius: 16; -fx-border-radius: 16; -fx-background-color: #fff; -fx-border-color: #e0e0e0; -fx-effect: dropshadow(gaussian, #888, 8, 0.2, 0, 2);");
            emojiPopup.getContent().add(popupContent);
            // Position popup below emoji button
            javafx.geometry.Point2D point = emojiButton.localToScreen(0, emojiButton.getHeight());
            emojiPopup.show(emojiButton, point.getX() - 40, point.getY() + 8);
        });

        Button sendButton = new Button("Enviar");
        sendButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 5 20 5 20;");
        sendButton.setOnMouseEntered(e -> sendButton.setStyle("-fx-background-color: #1976D2; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 5 20 5 20;"));
        sendButton.setOnMouseExited(e -> sendButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 30; -fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 5 20 5 20;"));

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

                saveMessageToDatabase(currentUserId, currentChatUserId, text);

                messageField.clear();
            }
        });

        messageInput.getChildren().addAll(messageField, emojiButton, sendButton);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        VBox chatPanel = new VBox(10);
        chatPanel.setPadding(new Insets(10));
        chatPanel.setStyle("-fx-background-color: #ffffff;");
        chatPanel.getChildren().addAll(header, new Separator(), userMessages, messageInput);

        return chatPanel;
    }

    private void openChat(String name, String imageUrl) {
        currentChatUserId = userIdMap.get(name); // pega o id do usuário com quem o chat será aberto
        VBox userMessages = userMessagesMap.get(name);
        if (userMessages != null) {
            userMessages.getChildren().clear();

            HBox welcomeBox = userWelcomeBoxMap.get(name);
            userMessages.getChildren().add(welcomeBox);

            // Stop previous timeline if running
            if (messageRefreshTimeline != null) {
                messageRefreshTimeline.stop();
            }
            // Load messages immediately
            loadMessagesFromDatabase(currentUserId, currentChatUserId, userMessages, welcomeBox);
            // Start timeline to refresh messages every 3 seconds
            messageRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(3), ev -> {
                    loadMessagesFromDatabase(currentUserId, currentChatUserId, userMessages, welcomeBox);
                })
            );
            messageRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
            messageRefreshTimeline.play();

            VBox chatPanel = buildChatPanel(userMessages, name, imageUrl);
            mainLayout.setCenter(chatPanel);
        }
    }

    private void addUserToDatabase(String email) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                String sql = "INSERT INTO chatifyusuarios (id, name, email) VALUES (?, ?, ?)";
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    String id = UUID.randomUUID().toString().substring(0, 7);
                    stmt.setString(1, id);
                    stmt.setString(2, email);
                    stmt.setString(3, email);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void loadUsersFromDatabase(VBox chatList) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                String sql = "SELECT id, name, email FROM chatifyusuarios";
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(sql)) {
                    while (rs.next()) {
                        String id = rs.getString("id");
                        String name = rs.getString("name");
                        String email = rs.getString("email");
                        if (email != null && !email.trim().isEmpty() && (currentUserEmail == null || !email.equals(currentUserEmail))) {
                            userIdMap.put(name, id);
                            Platform.runLater(() -> addUserToChatList(chatList, name, email));
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    private void saveMessageToDatabase(String sender, String receiver, String message) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
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
                return null;
            }
        };
        new Thread(task).start();
    }

    private void loadMessagesFromDatabase(String senderId, String receiverId, VBox messageBox, HBox welcomeBox) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                String sql = "SELECT sender_name, message FROM chatifymessages " +
                        "WHERE (sender_name = ? AND receiver_name = ?) " +
                        "OR (sender_name = ? AND receiver_name = ?) " +
                        "ORDER BY id ASC";
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, senderId);
                    stmt.setString(2, receiverId);
                    stmt.setString(3, receiverId);
                    stmt.setString(4, senderId);
                    ResultSet rs = stmt.executeQuery();
                    boolean hasMessages = false;
                    // Clear previous messages before adding new ones
                    Platform.runLater(() -> messageBox.getChildren().clear());
                    while (rs.next()) {
                        hasMessages = true;
                        String from = rs.getString("sender_name");
                        String msgText = rs.getString("message");
                        Platform.runLater(() -> {
                            Label msg = new Label(msgText);
                            msg.setWrapText(true);
                            msg.setPadding(new Insets(10));
                            msg.setStyle("-fx-background-radius: 10;");
                            HBox msgBox = new HBox(msg);
                            if (from.equals(senderId)) {
                                msg.setStyle("-fx-background-color: #e3e3ff; -fx-background-radius: 10;");
                                msgBox.setAlignment(Pos.CENTER_RIGHT);
                            } else {
                                msg.setStyle("-fx-background-color: #d9ffd9; -fx-background-radius: 10;");
                                msgBox.setAlignment(Pos.CENTER_LEFT);
                            }
                            messageBox.getChildren().add(msgBox);
                        });
                    }
                    if (!hasMessages) {
                        Platform.runLater(() -> messageBox.getChildren().add(welcomeBox));
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
