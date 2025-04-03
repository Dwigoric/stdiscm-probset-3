package ph.dlsu.edu.ccs.stdiscm.jgang.probset3;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GUI extends Application {
    private static final String VIDEO_FOLDER = Config.VIDEO_FOLDER; // Path to your video folder
    private final Map<String, MediaPlayer> previewPlayers = new ConcurrentHashMap<>();
    private MediaPlayer mainPlayer;
    private MediaView mainMediaView;
    private TilePane videoGrid;
    private VBox playerContainer;
    private Button closeButton;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        // Top navigation bar with search functionality
        root.setTop(createTopBar());

        // Main content area with video grid
        ScrollPane scrollPane = new ScrollPane();
        videoGrid = createVideoGrid();
        scrollPane.setContent(videoGrid);
        root.setCenter(scrollPane);

        // Video player section for full playback
        mainMediaView = new MediaView();
        mainMediaView.setPreserveRatio(true);
        playerContainer = createPlayerContainer();
        root.setBottom(playerContainer);

        loadVideos("");
        setupDirectoryWatcher();

        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        stage.setTitle("YouTube-like Media Viewer");
        stage.setScene(scene);
        stage.show();
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

    private TilePane createVideoGrid() {
        TilePane grid = new TilePane();
        grid.getStyleClass().add("video-grid");
        grid.setPrefColumns(4); // Number of columns in the grid
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(15));
        return grid;
    }

    private VBox createPlayerContainer() {
        VBox container = new VBox();
        container.getStyleClass().add("player-container");
        container.getChildren().add(mainMediaView);

        // Add close button
        closeButton = new Button("Close");
        closeButton.setOnAction(e -> closeVideo());
        container.getChildren().add(closeButton);

        return container;
    }

    private void loadVideos(String filter) {
        File folder = new File(VIDEO_FOLDER);
        File[] files = folder.listFiles();

        if (files != null) {
            Platform.runLater(() -> {
                videoGrid.getChildren().clear();
                for (File file : files) {
                    if (file.isFile() && file.getName().toLowerCase().contains(filter)) {
                        addVideoCard(file.getName());
                    }
                }
            });
        }
    }

    private void addVideoCard(String fileName) {
        VBox card = new VBox(5);
        card.getStyleClass().add("video-card");

        StackPane thumbnail = createThumbnail(fileName);

        Label title = new Label(fileName);
        title.getStyleClass().add("video-title");

        card.getChildren().addAll(thumbnail, title);

        videoGrid.getChildren().add(card);
    }

    private StackPane createThumbnail(String fileName) {
        StackPane thumbnail = new StackPane();
        thumbnail.getStyleClass().add("video-thumbnail");

        // Temporary placeholder - replace with actual thumbnail generation logic
        ImageView placeholder = new ImageView(new Image("file:placeholder.png"));
        placeholder.setFitWidth(320); // Thumbnail width
        placeholder.setFitHeight(180); // Thumbnail height

        thumbnail.getChildren().add(placeholder);

        setupHoverActions(thumbnail, fileName);

        return thumbnail;
    }

    private void setupHoverActions(StackPane thumbnail, String fileName) {
        Timeline hoverTimer = new Timeline();
        KeyFrame hoverStart = new KeyFrame(Duration.millis(300), e -> showPreview(fileName, thumbnail));
        KeyFrame hoverEnd = new KeyFrame(Duration.seconds(10));

        thumbnail.setOnMouseEntered(e -> {
            hoverTimer.getKeyFrames().setAll(hoverStart, hoverEnd);
            hoverTimer.play();
        });

        thumbnail.setOnMouseExited(e -> {
            hoverTimer.stop();
            stopPreview(fileName);
        });

        thumbnail.setOnMouseClicked(e -> playFullVideo(fileName));
    }

    private void showPreview(String fileName, StackPane container) {
        try {
            String filePath = VIDEO_FOLDER + "/" + fileName;
            Media media = new Media(new File(filePath).toURI().toString());
            MediaPlayer player = new MediaPlayer(media);

            player.setOnReady(() -> {
                MediaView previewView = new MediaView(player);
                previewView.setFitWidth(320);
                previewView.setFitHeight(180);
                container.getChildren().add(previewView);

                player.setStartTime(Duration.ZERO);
                player.setStopTime(Duration.seconds(10));
                player.play();
            });

            previewPlayers.put(fileName, player);
        } catch (Exception e) {
            System.err.println("Error loading preview: " + e.getMessage());
        }
    }

    private void stopPreview(String fileName) {
        MediaPlayer player = previewPlayers.get(fileName);
        if (player != null) {
            player.stop();
            player.dispose();
            previewPlayers.remove(fileName);
        }
    }

    private void playFullVideo(String fileName) {
        if (mainPlayer != null) {
            mainPlayer.stop();
            mainPlayer.dispose();
        }

        try {
            String filePath = VIDEO_FOLDER + "/" + fileName;
            Media media = new Media(new File(filePath).toURI().toString());
            mainPlayer = new MediaPlayer(media);
            mainMediaView.setMediaPlayer(mainPlayer);

            mainPlayer.setOnReady(() -> {
                mainMediaView.setFitWidth(640);
                mainMediaView.setFitHeight(360);
                mainPlayer.play();
            });
        } catch (Exception e) {
            System.err.println("Error playing video: " + e.getMessage());
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

    private void closeVideo() {
        if (mainPlayer != null) {
            mainPlayer.stop();
            mainPlayer.dispose();
            mainPlayer = null;
        }
        mainMediaView.setMediaPlayer(null);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
