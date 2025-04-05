package ph.dlsu.edu.ccs.stdiscm.jgang.probset3;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class GUI extends Application {

    private static String VIDEO_FOLDER;
    private final Map<String, MediaPlayer> previewPlayers = new ConcurrentHashMap<>();
    private Stage primaryStage;
    private Scene mainScene;
    private Scene fullscreenScene;
    private MediaPlayer mainPlayer;
    private MediaView mainMediaView;
    private FlowPane videoGrid; // Changed to FlowPane for grid layout
    private String currentVideoPath;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("YouTube-like Media Viewer");

        // Initialize main scene
        createMainScene();

        // Initialize fullscreen scene
        createFullscreenScene();

        // Load videos into the grid
        loadVideos("");
        setupDirectoryWatcher();

        // Set the initial scene
        primaryStage.setScene(mainScene);
        primaryStage.show();
    }

    // Method to create the main scene
    private void createMainScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top navigation bar with search functionality
        root.setTop(createTopBar());

        // Main content area with video grid
        ScrollPane scrollPane = new ScrollPane();
        videoGrid = createVideoGrid();
        scrollPane.setContent(videoGrid);
        scrollPane.setFitToWidth(true); // Make scrollpane fit to width
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Show vertical scrollbar when needed
        root.setCenter(scrollPane);

        mainScene = new Scene(root, 1280, 720);
        mainScene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
    }

    // Method to create the fullscreen scene
    private void createFullscreenScene() {
        StackPane root = new StackPane();
        root.getStyleClass().add("fullscreen-root");

        // MediaView for fullscreen playback
        mainMediaView = new MediaView();
        mainMediaView.setPreserveRatio(true);
        root.getChildren().add(mainMediaView);

        // Back button to return to main scene
        Button backButton = new Button("Back to Main");
        backButton.setOnAction(e -> {
            stopPlayback();
            primaryStage.setScene(mainScene);
        });
        StackPane.setAlignment(backButton, Pos.TOP_LEFT);
        StackPane.setMargin(backButton, new Insets(10));
        root.getChildren().add(backButton);

        fullscreenScene = new Scene(root, 1280, 720);
        fullscreenScene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(10);
        topBar.getStyleClass().add("top-bar");

        TextField searchField = new TextField();
        searchField.getStyleClass().add("search-field");
        searchField.setPromptText("Search videos...");

        // Add search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            loadVideos(newValue.toLowerCase());
        });

        topBar.getChildren().add(searchField);
        return topBar;
    }

    private FlowPane createVideoGrid() {
        FlowPane grid = new FlowPane();
        grid.getStyleClass().add("video-grid");
        grid.setHgap(15); // Horizontal gap
        grid.setVgap(15); // Vertical gap
        grid.setPadding(new Insets(15));
        grid.setPrefWidth(1200); // Preferred width
        return grid;
    }

    private void loadVideos(String filter) {
        ArrayList<File> files = new ArrayList<>();
        File folder = new File(VIDEO_FOLDER);
        List<String> remoteFileNames = Producer.getVideoFiles(filter);

        // Clean local files if they are not in remote files
        for (File localFile : Objects.requireNonNull(folder.listFiles())) {
            if (!localFile.isDirectory() && !remoteFileNames.contains(localFile.getName())) {
                localFile.delete();
            }
        }

        for (String fileName : remoteFileNames) {
            // If already in local files, use local file
            File localFile = new File(VIDEO_FOLDER + "/" + fileName);
            if (localFile.exists()) {
                files.add(localFile);
            } else {
                // If not in local files, use remote file
                File remoteFile = Producer.getVideoFile(fileName);
                if (remoteFile != null) {
                    files.add(remoteFile);
                }
            }
        }

        Platform.runLater(() -> {
            videoGrid.getChildren().clear();
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().contains(filter)) {
                    addVideoCard(file.getName());
                }
            }
        });
    }

    private void addVideoCard(String fileName) {
        VBox card = new VBox(5); // VBox to hold thumbnail and title
        card.getStyleClass().add("video-card");

        StackPane thumbnail = createThumbnail(fileName); // Create thumbnail
        Label title = new Label(fileName); // Create title label
        title.getStyleClass().add("video-title");

        card.getChildren().addAll(thumbnail, title); // Add thumbnail and title to card
        videoGrid.getChildren().add(card); // Add card to grid
    }

    private StackPane createThumbnail(String fileName) {
        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("video-thumbnail");

        String filePath = VIDEO_FOLDER + "/" + fileName;
        Media media = new Media(new File(filePath).toURI().toString());
        MediaPlayer player = new MediaPlayer(media);

        player.setOnReady(() -> {
            MediaView previewView = new MediaView(player);
            previewView.setFitWidth(320);
            previewView.setFitHeight(180);
            thumbnail.getChildren().add(previewView);

            player.setCycleCount(MediaPlayer.INDEFINITE);
            player.setAutoPlay(false);
            player.setOnEndOfMedia(() -> player.seek(Duration.ZERO));

            player.setStartTime(Duration.ZERO);
            player.setStopTime(Duration.seconds(10));
        });

        previewPlayers.put(fileName, player);

        setupHoverActions(thumbnail, fileName, player);

        return thumbnail;
    }

    private void setupHoverActions(StackPane thumbnail, String fileName, MediaPlayer player) {
        Timeline hoverTimer = new Timeline();
        KeyFrame hoverStart = new KeyFrame(Duration.millis(300), e -> showPreview(player));
        KeyFrame hoverEnd = new KeyFrame(Duration.seconds(10));

        thumbnail.setOnMouseEntered(e -> {
            hoverTimer.getKeyFrames().setAll(hoverStart, hoverEnd);
            hoverTimer.play();
        });

        thumbnail.setOnMouseExited(e -> {
            hoverTimer.stop();
            stopPreview(fileName);
        });

        thumbnail.setOnMouseClicked(e -> goToFullscreen(fileName));
    }

    private void showPreview(MediaPlayer player) {
        try {
            player.play();
        } catch (Exception e) {
            System.err.println("Error loading preview: " + e.getMessage());
        }
    }

    private void stopPreview(String fileName) {
        MediaPlayer player = previewPlayers.get(fileName);
        if (player != null) player.stop();
    }

    private void goToFullscreen(String fileName) {
        currentVideoPath = VIDEO_FOLDER + "/" + fileName;
        playFullVideo(currentVideoPath);
        primaryStage.setScene(fullscreenScene);
    }

    private void playFullVideo(String videoPath) {
        if (mainPlayer != null) {
            stopPlayback();
        }

        try {
            Media media = new Media(new File(videoPath).toURI().toString());
            mainPlayer = new MediaPlayer(media);
            mainMediaView.setMediaPlayer(mainPlayer);

            mainPlayer.setOnReady(() -> {
                mainMediaView.setFitWidth(1280);
                mainMediaView.setFitHeight(720);
                mainPlayer.play();
            });
        } catch (Exception e) {
            System.err.println("Error playing video: " + e.getMessage());
        }
    }

    private void stopPlayback() {
        if (mainPlayer != null) {
            mainPlayer.stop();
            mainPlayer.dispose();
            mainPlayer = null;
        }
    }

    private void setupDirectoryWatcher() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Paths.get(VIDEO_FOLDER).register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                                String newFile = event.context().toString();
                                Platform.runLater(() -> addVideoCard(newFile));
                            }
                        }
                        key.reset();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        } catch (IOException e) {
            System.err.println("Directory watcher error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (!ProducerConfig.init()) {
            System.err.println("Failed to initialize Producer configuration.");
            return;
        }

        VIDEO_FOLDER = ProducerConfig.get("cache_directory");

        if (VIDEO_FOLDER == null || VIDEO_FOLDER.isEmpty()) {
            System.err.println("Video folder not specified in configuration.");
            return;
        }

        // Check if the video folder exists
        File folder = new File(VIDEO_FOLDER);
        if (!folder.exists() || !folder.isDirectory()) {
            // Create the folder if it doesn't exist
            if (!folder.mkdirs()) {
                System.err.println("Failed to create video directory: " + VIDEO_FOLDER);
                return;
            }
        }

        launch(args);
    }
}
