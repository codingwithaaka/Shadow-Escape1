
package shadowescape;

import javax.sound.sampled.*;
import java.io.File;

public class MusicPlayer {

    private Clip clip;
    private boolean musicOn = true;

    public MusicPlayer() {
        try {
            File musicFile = new File("assets/dungeon.wav");

            AudioInputStream audio = AudioSystem.getAudioInputStream(musicFile);

            clip = AudioSystem.getClip();
            clip.open(audio);

            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();

        } catch (Exception e) {
            System.out.println("Music error: " + e.getMessage());
        }
    }

    public void toggleMusic() {

        if (clip == null)
            return;

        if (musicOn) {
            clip.stop();
            musicOn = false;
        } else {
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();
            musicOn = true;
        }
    }

    public boolean isMusicOn() {
        return musicOn;
    }

    public void stopMusic() {
        if (clip != null) {
            clip.stop();
        }
    }

    public void closeMusic() {
        if (clip != null) {
            clip.stop();
            clip.close();
        }
    }
}
