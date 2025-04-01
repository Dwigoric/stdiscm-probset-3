package ph.dlsu.edu.ccs.stdiscm.jgang.probset3;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import java.io.File;

public class VideoPlayer {
    public static void playVideo(String filePath, MediaView mediaView) {
        File videoFile = new File(filePath);
        Media media = new Media(videoFile.toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(media);
        mediaPlayer.setAutoPlay(true);
        mediaView.setMediaPlayer(mediaPlayer);
    }
}
