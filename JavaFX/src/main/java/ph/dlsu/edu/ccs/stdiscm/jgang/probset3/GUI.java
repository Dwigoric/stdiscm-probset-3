package ph.dlsu.edu.ccs.stdiscm.jgang.probset3;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import ph.dlsu.edu.ccs.stdiscm.jgang.probset3.Config;

import java.io.File;

/**
 * Displays the list of videos stored in the VIDEO_FOLDER
 * and allows uers to select and play videos
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

        // Create and set the scene
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("Uploaded Videos");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
