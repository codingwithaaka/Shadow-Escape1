
package shadowescape;

import javax.swing.*;
import java.awt.*;

public class MainMenu extends JPanel {

    private final GameFrame frame;
    private JButton musicButton;

    private final Color BLACK = new Color(9, 7, 18);
    private final Color PURPLE = new Color(35, 18, 48);
    private final Color ORANGE = new Color(255, 125, 35);
    private final Color MOON = new Color(255, 190, 105);

    public MainMenu(GameFrame frame) {
        this.frame = frame;

        setLayout(null);

        JButton playButton = new JButton("PLAY");
        JButton instructionsButton = new JButton("INSTRUCTIONS");
        JButton exitButton = new JButton("EXIT");

        musicButton = new JButton();

        styleButton(playButton);
        styleButton(instructionsButton);
        styleButton(exitButton);
        styleButton(musicButton);

        playButton.setBounds(350, 315, 200, 48);
        instructionsButton.setBounds(350, 380, 200, 48);
        exitButton.setBounds(350, 445, 200, 48);
        musicButton.setBounds(710, 25, 155, 38);

        updateMusicButton();

        add(playButton);
        add(instructionsButton);
        add(exitButton);
        add(musicButton);

        playButton.addActionListener(e -> frame.startGame());

        instructionsButton.addActionListener(e -> JOptionPane.showMessageDialog(
                this,
                "SHADOW ESCAPE - INSTRUCTIONS\n\n" +
                        "W / UP    - Move Up\n" +
                        "S / DOWN  - Move Down\n" +
                        "A / LEFT  - Move Left\n" +
                        "D / RIGHT - Move Right\n" +
                        "SPACE     - Attack\n" +
                        "ESC       - Pause Game\n\n" +
                        "Explore the dungeon and survive!",
                "HOW TO PLAY",
                JOptionPane.INFORMATION_MESSAGE));

        exitButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to leave the dungeon?",
                    "EXIT SHADOW ESCAPE",
                    JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                frame.dispose();
                System.exit(0);
            }
        });

        musicButton.addActionListener(e -> {
            frame.toggleMusic();
            updateMusicButton();
        });
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("Monospaced", Font.BOLD, 17));
        button.setBackground(new Color(30, 15, 35));
        button.setForeground(new Color(255, 175, 85));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createLineBorder(
                new Color(220, 105, 40), 3));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void updateMusicButton() {
        musicButton.setText(
                frame.isMusicOn() ? "MUSIC: ON" : "MUSIC: OFF");
    }

    // Pixel rectangle helper
    private void pixel(Graphics2D g, int x, int y, int w, int h, Color c) {
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        // No smooth edges: keep the pixel-art look
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        int w = getWidth();
        int h = getHeight();

        // Night sky
        pixel(g2, 0, 0, w, h, BLACK);

        // Pixel stars
        // Pixel-art twinkling stars
int[][] stars = {
    {65, 65}, {125, 145}, {195, 55},
    {300, 115}, {375, 65}, {470, 155},
    {555, 45}, {590, 205}, {810, 65},
    {850, 150}, {75, 260}, {330, 205},
    {510, 235}, {875, 285}
};

for (int i = 0; i < stars.length; i++) {
    int x = stars[i][0];
    int y = stars[i][1];

    Color starColor = i % 3 == 0
        ? new Color(255, 190, 105)
        : new Color(185, 155, 190);

    // Pixel sparkle
    pixel(g2, x + 4, y, 4, 12, starColor);
    pixel(g2, x, y + 4, 12, 4, starColor);
    pixel(g2, x + 4, y + 4, 4, 4, Color.WHITE);
}

        // Pixel moon
        drawMoon(g2, 645, 75);

        // Haunted castle
        drawCastle(g2);

        // Pixel bats
        drawBat(g2, 160, 100);
        drawBat(g2, 260, 170);
        drawBat(g2, 735, 265);
        drawBat(g2, 525, 90);

        // Distant mountains
        pixel(g2, 0, 390, 900, 110, new Color(20, 12, 30));
        pixel(g2, 0, 425, 900, 75, new Color(13, 9, 23));

        // Ground
        pixel(g2, 0, 500, 900, 100, new Color(12, 8, 17));

        // Ground details
        for (int i = 0; i < 45; i++) {
            int x = (i * 83) % 900;
            int y = 505 + (i * 19) % 90;

            pixel(g2, x, y, 12, 4, new Color(40, 24, 39));
        }

        // Pixel pumpkins
        drawPumpkin(g2, 65, 490);
        drawPumpkin(g2, 790, 490);
        drawPumpkin(g2, 235, 535);
        drawPumpkin(g2, 680, 535);

        // Title
        g2.setFont(new Font("Monospaced", Font.BOLD, 46));

        String title = "SHADOW ESCAPE";
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(title)) / 2;

        // Pixel-style shadow
        g2.setColor(new Color(75, 25, 20));
        g2.drawString(title, tx + 5, 210);

        // Orange title
        g2.setColor(ORANGE);
        g2.drawString(title, tx, 205);

        // Subtitle
        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.setColor(new Color(210, 170, 155));

        String subtitle = "ENTER THE DUNGEON. SURVIVE THE DARKNESS.";
        FontMetrics sm = g2.getFontMetrics();

        g2.drawString(
                subtitle,
                (w - sm.stringWidth(subtitle)) / 2,
                250);

        // Decorative pixel divider
        pixel(g2, 310, 270, 280, 4, ORANGE);
        pixel(g2, 330, 278, 240, 3, new Color(120, 55, 65));

        // Footer
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(new Color(190, 125, 115));

        String footer = "THE NIGHT HAS JUST BEGUN...";
        FontMetrics ff = g2.getFontMetrics();

        g2.drawString(
                footer,
                (w - ff.stringWidth(footer)) / 2,
                585);

        g2.dispose();
    }

    private void drawMoon(Graphics2D g, int x, int y) {
        int s = 10;

        String[] moon = {
                "....#####....",
                "..#########..",
                ".###########.",
                "#############",
                "#############",
                "#############",
                "#############",
                ".###########.",
                "..#########..",
                "....#####...."
        };

        for (int r = 0; r < moon.length; r++) {
            for (int c = 0; c < moon[r].length(); c++) {
                if (moon[r].charAt(c) == '#') {
                    pixel(g, x + c * s, y + r * s, s, s, MOON);
                }
            }
        }

        pixel(g, x + 35, y + 25, 10, 10,
                new Color(220, 140, 85));
        pixel(g, x + 75, y + 55, 10, 10,
                new Color(220, 140, 85));
    }

    private void drawCastle(Graphics2D g) {
        Color wall = new Color(23, 15, 35);
        Color roof = new Color(13, 9, 24);
        Color window = new Color(255, 100, 30);

        // Main castle
        pixel(g, 250, 260, 400, 170, wall);

        // Left tower
        pixel(g, 220, 200, 90, 230, wall);
        pixel(g, 210, 190, 110, 15, roof);
        pixel(g, 230, 165, 70, 25, roof);

        // Right tower
        pixel(g, 590, 190, 90, 240, wall);
        pixel(g, 580, 180, 110, 15, roof);
        pixel(g, 600, 155, 70, 25, roof);

        // Tower tops
        pixel(g, 230, 145, 15, 20, roof);
        pixel(g, 260, 145, 15, 20, roof);
        pixel(g, 600, 135, 15, 20, roof);
        pixel(g, 630, 135, 15, 20, roof);

        // Castle battlements
        for (int i = 0; i < 8; i++) {
            pixel(g, 260 + i * 45, 245, 20, 20, roof);
        }

        // Windows
        pixel(g, 250, 250, 15, 30, window);
        pixel(g, 620, 240, 15, 30, window);
        pixel(g, 390, 300, 20, 35, window);
        pixel(g, 490, 300, 20, 35, window);

        // Main entrance
        pixel(g, 420, 360, 60, 70, roof);
        pixel(g, 435, 375, 30, 55, new Color(70, 25, 35));
    }

    private void drawBat(Graphics2D g, int x, int y) {
        Color c = new Color(3, 2, 10);

        pixel(g, x + 12, y + 8, 12, 12, c);
        pixel(g, x, y, 12, 8, c);
        pixel(g, x + 5, y + 8, 8, 8, c);
        pixel(g, x + 24, y, 12, 8, c);
        pixel(g, x + 24, y + 8, 8, 8, c);
        pixel(g, x + 36, y + 8, 12, 8, c);

        pixel(g, x + 16, y + 20, 4, 5, c);
        pixel(g, x + 22, y + 20, 4, 5, c);
    }

    private void drawPumpkin(Graphics2D g, int x, int y) {
        Color orange = new Color(225, 90, 20);
        Color dark = new Color(125, 45, 15);
        Color glow = new Color(255, 215, 80);

        // Stem
        pixel(g, x + 20, y, 10, 10,
                new Color(65, 110, 45));

        // Body
        pixel(g, x + 5, y + 10, 40, 30, orange);
        pixel(g, x, y + 18, 50, 20, orange);
        pixel(g, x + 10, y + 5, 30, 40, orange);

        // Ridges
        pixel(g, x + 5, y + 15, 5, 25, dark);
        pixel(g, x + 40, y + 15, 5, 25, dark);

        // Eyes
        pixel(g, x + 10, y + 18, 10, 10, glow);
        pixel(g, x + 30, y + 18, 10, 10, glow);

        // Mouth
        pixel(g, x + 15, y + 32, 20, 5, glow);
        pixel(g, x + 20, y + 37, 10, 4, glow);
    }
}
