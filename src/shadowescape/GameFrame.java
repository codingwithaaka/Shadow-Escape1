
package shadowescape;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameFrame extends JFrame {

    private final CardLayout cardLayout;
    private final JPanel screenPanel;
    private final GamePanel game;
    private final MusicPlayer music;

    public GameFrame() {
        setTitle("SHADOW ESCAPE");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Start background music
        music = new MusicPlayer();

        cardLayout = new CardLayout();
        screenPanel = new JPanel(cardLayout);

        MainMenu menu = new MainMenu(this);
        game = new GamePanel(this);

        screenPanel.add(menu, "MENU");
        screenPanel.add(game, "GAME");

        add(screenPanel);
        cardLayout.show(screenPanel, "MENU");

        // Clean up music when window closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                music.closeMusic();
            }
        });
    }

    public void startGame() {
        game.startNewGame();
        cardLayout.show(screenPanel, "GAME");
        game.requestFocusInWindow();
    }

    public void showMenu() {
        game.stopGame();
        cardLayout.show(screenPanel, "MENU");
    }

    public void toggleMusic() {
        music.toggleMusic();
    }

    public boolean isMusicOn() {
        return music.isMusicOn();
    }
}
