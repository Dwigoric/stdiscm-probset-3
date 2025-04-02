package ph.dlsu.edu.ccs.stdiscm.jgang.probset3;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;

import java.io.File;

/**
 * Displays the list of videos stored in the VIDEO_FOLDER
 * and allows users to select and play videos
 */
public class GUI extends Application {
    private static final String VIDEO_FOLDER = Config.VIDEO_FOLDER;

    @Override
    public void start(Stage stage) {
        ListView<String> videoList = new ListView<>();
        File folder = new File(VIDEO_FOLDER);
        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                videoList.getItems().add(file.getName());
            }
        }

        // Create MediaView to display the selected video
        MediaView mediaView = new MediaView();

        videoList.setOnMouseClicked(event -> {
            String selectedVideo = videoList.getSelectionModel().getSelectedItem();
            if (selectedVideo != null) {
                VideoPlayer.playVideo(VIDEO_FOLDER + "/" + selectedVideo, mediaView);
            }
        });

        // Create a BorderPane layout to add both ListView and MediaView
        BorderPane root = new BorderPane();
        root.setLeft(videoList);  // Add the video list to the left side
        root.setCenter(mediaView);  // Add the media view to the center

        // Add listener to adjust MediaView's size to maintain the aspect ratio
        mediaView.setPreserveRatio(true);  // Preserve the aspect ratio of the video
        mediaView.setFitWidth(800);        // Make sure the width scales with the window
        mediaView.setFitHeight(600);       // Make sure the height scales with the window

        // Create and set the scene
        Scene scene = new Scene(root, 1200, 600);
        stage.setTitle("Uploaded Videos");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
