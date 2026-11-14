
package shadowescape;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel {

    private final int tileSize = 50;
    private final int mapX = 50;
    private final int mapY = 75;
    private final int playerSize = 30;
    private final int playerSpeed = 6;
    private final int attackDamage = 20;

    private final String[][] dungeonLevels = {
            {
                    "################",
                    "#....C.........#",
                    "#..##..........#",
                    "#........##....#",
                    "#....K.........#",
                    "#....###.......#",
                    "#...........##.#",
                    "#.............E#",
                    "################"
            },
            {
                    "################",
                    "#....C.........#",
                    "#.####.........#",
                    "#........##....#",
                    "#.K............#",
                    "#....####......#",
                    "#...........##.#",
                    "#.............E#",
                    "################"
            },
            {
                    "################",
                    "#....C.........#",
                    "#..##..........#",
                    "#.......##.....#",
                    "#....K.........#",
                    "#.####.........#",
                    "#......#....##.#",
                    "#.............E#",
                    "################"
            }
    };

    private final int[][][] enemySpawns = {
            { { 12, 1 }, { 2, 6 } },
            { { 12, 1 }, { 2, 6 }, { 7, 3 } },
            { { 12, 1 }, { 2, 6 }, { 7, 2 }, { 11, 5 } }
    };

    private String[] dungeonMap;
    private int currentLevel = 0;

    private int playerX;
    private int playerY;

    private int facingX = 0;
    private int facingY = 1;

    private int score = 0;
    private int health = 100;

    private boolean coinCollected;
    private boolean keyCollected;
    private boolean gameWon;
    private boolean gameOver;

    private int damageCooldown;
    private int enemyUpdateCounter;
    private int attackCooldown;
    private int attackTimer;

    private int animationTick;
    private int movementAnimationTicks;

    private String statusMessage = "Find the key and escape!";

    private final GameFrame frame;
    private final List<Enemy> enemies = new ArrayList<>();
    private final Timer gameTimer;

    public GamePanel(GameFrame frame) {
        this.frame = frame;

        setBackground(new Color(10, 8, 18));
        setFocusable(true);

        setupMovementKeys();
        setupActionKeys();

        gameTimer = new Timer(40, e -> updateGame());

        resetGame();
    }

    public void startNewGame() {
        resetGame();
        gameTimer.start();
        requestFocusInWindow();
    }

    public void stopGame() {
        gameTimer.stop();
    }

    // ================= MOVEMENT =================

    private void setupMovementKeys() {
        addMovementKey("W", 0, -playerSpeed);
        addMovementKey("S", 0, playerSpeed);
        addMovementKey("A", -playerSpeed, 0);
        addMovementKey("D", playerSpeed, 0);

        addMovementKey("UP", 0, -playerSpeed);
        addMovementKey("DOWN", 0, playerSpeed);
        addMovementKey("LEFT", -playerSpeed, 0);
        addMovementKey("RIGHT", playerSpeed, 0);
    }

    private void setupActionKeys() {

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("pressed SPACE"), "attack");

        getActionMap().put("attack", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                performAttack();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("pressed R"), "restart");

        getActionMap().put("restart", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (gameOver || gameWon) {
                    resetGame();
                    gameTimer.start();
                }
            }
        });
    }

    private void addMovementKey(String key, int dx, int dy) {

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("pressed " + key), key);

        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {

                if (gameOver || gameWon)
                    return;

                facingX = Integer.signum(dx);
                facingY = Integer.signum(dy);

                int nextX = playerX + dx;
                int nextY = playerY + dy;

                Rectangle nextPosition = new Rectangle(nextX, nextY, playerSize, playerSize);

                if (!isWall(nextPosition)) {

                    playerX = nextX;
                    playerY = nextY;

                    movementAnimationTicks = 8;

                    collectItems();

                    if (keyCollected &&
                            touchesTile(getPlayerBounds(), 'E')) {
                        completeLevel();
                    }
                }

                repaint();
            }
        });
    }

    private void resetGame() {
        score = 0;
        health = 100;
        currentLevel = 0;
        animationTick = 0;
        loadLevel();
    }

    private void loadLevel() {

        dungeonMap = dungeonLevels[currentLevel];

        playerX = mapX + tileSize + 10;
        playerY = mapY + tileSize + 10;

        facingX = 0;
        facingY = 1;

        coinCollected = false;
        keyCollected = false;
        gameWon = false;
        gameOver = false;

        damageCooldown = 0;
        enemyUpdateCounter = 0;
        attackCooldown = 0;
        attackTimer = 0;

        enemies.clear();

        int enemyHealth = 40 + currentLevel * 20;

        for (int[] spawn : enemySpawns[currentLevel]) {

            int enemyX = mapX + spawn[0] * tileSize + 10;
            int enemyY = mapY + spawn[1] * tileSize + 10;

            enemies.add(new Enemy(enemyX, enemyY, enemyHealth));
        }

        statusMessage = "Level " + (currentLevel + 1)
                + ": Find the key and reach the exit!";

        repaint();
    }

    private Rectangle getPlayerBounds() {
        return new Rectangle(playerX, playerY, playerSize, playerSize);
    }

    private boolean touchesTile(Rectangle player, char symbol) {

        for (int row = 0; row < dungeonMap.length; row++) {
            for (int col = 0; col < dungeonMap[row].length(); col++) {

                if (dungeonMap[row].charAt(col) == symbol) {

                    Rectangle tile = new Rectangle(
                            mapX + col * tileSize,
                            mapY + row * tileSize,
                            tileSize, tileSize);

                    if (player.intersects(tile))
                        return true;
                }
            }
        }

        return false;
    }

    private boolean isWall(Rectangle player) {

        int startCol = Math.floorDiv(player.x - mapX, tileSize);
        int endCol = Math.floorDiv(
                player.x + player.width - 1 - mapX, tileSize);

        int startRow = Math.floorDiv(player.y - mapY, tileSize);
        int endRow = Math.floorDiv(
                player.y + player.height - 1 - mapY, tileSize);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {

                if (row < 0 || row >= dungeonMap.length
                        || col < 0 || col >= dungeonMap[0].length()) {
                    return true;
                }

                char tile = dungeonMap[row].charAt(col);

                if (tile == '#')
                    return true;
                if (tile == 'E' && !keyCollected)
                    return true;
            }
        }

        return false;
    }

    // ================= ITEMS =================

    private void collectItems() {

        Rectangle player = getPlayerBounds();

        if (!coinCollected && touchesTile(player, 'C')) {
            coinCollected = true;
            score += 10;
            statusMessage = "Coin collected! +10 points";
        }

        if (!keyCollected && touchesTile(player, 'K')) {
            keyCollected = true;
            statusMessage = "You found the dungeon key!";
        }
    }

    // ================= COMBAT =================

    private Rectangle getAttackBounds() {

        int range = 45;
        int size = 45;

        if (facingX == 1)
            return new Rectangle(
                    playerX + playerSize - 5, playerY - 8, range, size);

        if (facingX == -1)
            return new Rectangle(
                    playerX - range + 5, playerY - 8, range, size);

        if (facingY == -1)
            return new Rectangle(
                    playerX - 8, playerY - range + 5, size, range);

        return new Rectangle(
                playerX - 8, playerY + playerSize - 5, size, range);
    }

    private void performAttack() {

        if (gameOver || gameWon || attackCooldown > 0)
            return;

        attackCooldown = 12;
        attackTimer = 10;

        Rectangle attackArea = getAttackBounds();

        for (Enemy enemy : enemies) {
            attackEnemy(enemy, attackArea);
        }

        repaint();
    }

    private void attackEnemy(Enemy enemy, Rectangle attackArea) {

        if (!enemy.alive)
            return;

        Rectangle enemyBounds = new Rectangle(
                enemy.x, enemy.y, enemy.size, enemy.size);

        if (attackArea.intersects(enemyBounds)) {

            enemy.health -= attackDamage;
            enemy.hitFlash = 6;

            statusMessage = "Enemy hit! -" + attackDamage + " HP";

            if (enemy.health <= 0) {

                enemy.health = 0;
                enemy.alive = false;
                enemy.deathTimer = 16;

                score += 25;

                statusMessage = "Enemy defeated! +25 points";
            }
        }
    }

    // ================= GAME UPDATE =================

    private void updateGame() {

        if (!isShowing() || gameOver || gameWon)
            return;

        animationTick++;

        if (damageCooldown > 0)
            damageCooldown--;
        if (attackCooldown > 0)
            attackCooldown--;
        if (attackTimer > 0)
            attackTimer--;

        if (movementAnimationTicks > 0)
            movementAnimationTicks--;

        enemyUpdateCounter++;

        if (enemyUpdateCounter >= 3) {

            enemyUpdateCounter = 0;

            for (Enemy enemy : enemies) {
                moveEnemy(enemy);
            }
        }

        for (Enemy enemy : enemies) {

            if (enemy.hitFlash > 0)
                enemy.hitFlash--;

            if (!enemy.alive && enemy.deathTimer > 0) {
                enemy.deathTimer--;
            }

            checkEnemyAttack(enemy);

            if (gameOver)
                break;
        }

        repaint();
    }

    private void moveEnemy(Enemy enemy) {

        if (!enemy.alive)
            return;

        int enemySpeed = 2 + currentLevel;

        int dx = Integer.compare(
                playerX + playerSize / 2,
                enemy.x + enemy.size / 2);

        int dy = Integer.compare(
                playerY + playerSize / 2,
                enemy.y + enemy.size / 2);

        Rectangle nextX = new Rectangle(
                enemy.x + dx * enemySpeed,
                enemy.y,
                enemy.size, enemy.size);

        if (dx != 0 && !isWall(nextX))
            enemy.x += dx * enemySpeed;

        Rectangle nextY = new Rectangle(
                enemy.x,
                enemy.y + dy * enemySpeed,
                enemy.size, enemy.size);

        if (dy != 0 && !isWall(nextY))
            enemy.y += dy * enemySpeed;
    }

    private void checkEnemyAttack(Enemy enemy) {

        if (!enemy.alive)
            return;

        Rectangle enemyBounds = new Rectangle(
                enemy.x, enemy.y, enemy.size, enemy.size);

        if (getPlayerBounds().intersects(enemyBounds)
                && damageCooldown == 0) {

            health -= 20;
            damageCooldown = 30;

            statusMessage = "Enemy attacked you! -20 HP";

            if (health <= 0) {
                health = 0;
                loseGame();
            }
        }
    }

    // ================= LEVEL PROGRESSION =================

    private void completeLevel() {

        if (gameWon || gameOver)
            return;

        gameWon = true;
        gameTimer.stop();
        repaint();

        SwingUtilities.invokeLater(() -> {

            boolean finalLevel = currentLevel == dungeonLevels.length - 1;

            JPanel popup = new JPanel();
            popup.setLayout(new BoxLayout(popup, BoxLayout.Y_AXIS));
            popup.setBackground(new Color(20, 14, 35));
            popup.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                            new Color(155, 90, 230), 3),
                    BorderFactory.createEmptyBorder(20, 25, 20, 25)));

            JLabel heading = new JLabel(
                    finalLevel ? "VICTORY!" : "DUNGEON CLEARED!");
            heading.setFont(new Font("Monospaced", Font.BOLD, 25));
            heading.setForeground(new Color(205, 145, 255));
            heading.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel subtitle = new JLabel(
                    finalLevel
                            ? "You conquered all three dungeons!"
                            : "The ancient gate has been conquered!");
            subtitle.setFont(new Font("Monospaced", Font.PLAIN, 12));
            subtitle.setForeground(Color.WHITE);
            subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel scoreLabel = new JLabel("SCORE: " + score);
            scoreLabel.setFont(new Font("Monospaced", Font.BOLD, 17));
            scoreLabel.setForeground(new Color(255, 210, 100));
            scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel healthLabel = new JLabel(
                    "REMAINING HEALTH: " + health);
            healthLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
            healthLabel.setForeground(new Color(120, 235, 155));
            healthLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            popup.add(heading);
            popup.add(Box.createVerticalStrut(12));
            popup.add(subtitle);
            popup.add(Box.createVerticalStrut(20));
            popup.add(scoreLabel);
            popup.add(Box.createVerticalStrut(8));
            popup.add(healthLabel);

            popup.setPreferredSize(new Dimension(370, 205));

            String[] options = finalLevel
                    ? new String[] { "Play Again", "Main Menu", "Exit" }
                    : new String[] { "Next Level", "Main Menu", "Exit" };

            int choice = JOptionPane.showOptionDialog(
                    this,
                    popup,
                    "SHADOW ESCAPE",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (finalLevel) {
                handleEndChoice(choice);
            } else {

                if (choice == 0) {
                    currentLevel++;
                    loadLevel();
                    gameTimer.start();

                } else if (choice == 1) {
                    frame.showMenu();

                } else if (choice == 2) {
                    System.exit(0);

                } else {
                    frame.showMenu();
                }
            }
        });
    }

    private void loseGame() {

        if (gameOver || gameWon)
            return;

        gameOver = true;
        statusMessage = "GAME OVER!";

        gameTimer.stop();
        repaint();

        SwingUtilities.invokeLater(() -> {

            String[] options = {
                    "Restart", "Main Menu", "Exit"
            };

            int choice = JOptionPane.showOptionDialog(
                    this,
                    "You were defeated!\nFinal Score: " + score,
                    "SHADOW ESCAPE - GAME OVER",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.ERROR_MESSAGE,
                    null, options, options[0]);

            handleEndChoice(choice);
        });
    }

    private void handleEndChoice(int choice) {

        if (choice == 0) {
            resetGame();
            gameTimer.start();

        } else if (choice == 1) {
            frame.showMenu();

        } else if (choice == 2) {
            System.exit(0);

        } else {
            frame.showMenu();
        }
    }

    // ================= MAIN DRAWING =================

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        g2.setColor(new Color(10, 8, 18));
        g2.fillRect(0, 0, getWidth(), getHeight());

        drawDungeon(g2);

        // HUD
        g2.setColor(new Color(18, 14, 30));
        g2.fillRect(0, 0, getWidth(), 55);

        g2.setColor(new Color(190, 130, 255));
        g2.setFont(new Font("Monospaced", Font.BOLD, 19));
        g2.drawString("SHADOW ESCAPE", 50, 34);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));

        g2.drawString("LEVEL " + (currentLevel + 1), 390, 32);
        g2.drawString("SCORE: " + score, 520, 32);
        g2.drawString("KEY: " + (keyCollected ? "YES" : "NO"), 620, 32);

        drawHealthBar(g2);

        for (Enemy enemy : enemies) {
            drawEnemy(g2, enemy);
        }

        drawPlayer(g2);

        if (attackTimer > 0) {
            drawAttack(g2);
        }

        // Bottom status bar
        g2.setColor(new Color(18, 14, 30));
        g2.fillRect(0, 550, getWidth(), 50);

        g2.setColor(new Color(215, 190, 240));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
        g2.drawString(statusMessage, 50, 580);

        if (gameOver || gameWon) {

            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRect(0, 55, getWidth(), 495);

            g2.setFont(new Font("Monospaced", Font.BOLD, 42));
            g2.setColor(gameWon ? Color.GREEN : Color.RED);

            String message = gameWon
                    ? "DUNGEON CLEARED!"
                    : "GAME OVER";

            FontMetrics fm = g2.getFontMetrics();

            g2.drawString(
                    message,
                    (getWidth() - fm.stringWidth(message)) / 2,
                    300);
        }

        g2.dispose();
    }

    // ================= DUNGEON =================

    private void drawDungeon(Graphics2D g2) {

        for (int row = 0; row < dungeonMap.length; row++) {

            for (int col = 0; col < dungeonMap[row].length(); col++) {

                int x = mapX + col * tileSize;
                int y = mapY + row * tileSize;

                char tile = dungeonMap[row].charAt(col);

                if (tile == '#') {
                    drawWall(g2, x, y, row, col);

                } else {
                    drawFloor(g2, x, y, row, col);
                }

                if (tile == 'C' && !coinCollected)
                    drawCoin(g2, x, y);

                if (tile == 'K' && !keyCollected)
                    drawKey(g2, x, y);

                if (tile == 'E')
                    drawDoor(g2, x, y);
            }
        }
    }

    // ================= STONE WALL GRAPHICS =================

    private void drawWall(Graphics2D g2, int x, int y,
            int row, int col) {

        // Deep stone background
        g2.setColor(new Color(25, 22, 37));
        g2.fillRect(x, y, tileSize, tileSize);

        // Individual stone bricks
        g2.setColor(new Color(66, 57, 79));
        g2.fillRect(x + 2, y + 3, 46, 19);
        g2.fillRect(x + 2, y + 25, 21, 21);
        g2.fillRect(x + 25, y + 25, 23, 21);

        // Stone highlights
        g2.setColor(new Color(105, 91, 120));
        g2.drawLine(x + 4, y + 5, x + 43, y + 5);
        g2.drawLine(x + 4, y + 27, x + 20, y + 27);
        g2.drawLine(x + 27, y + 27, x + 45, y + 27);

        // Dark brick edges
        g2.setColor(new Color(35, 29, 47));
        g2.drawLine(x + 3, y + 20, x + 47, y + 20);
        g2.drawLine(x + 23, y + 25, x + 23, y + 47);
        g2.drawLine(x + 2, y + 47, x + 48, y + 47);

        // Small cracks
        g2.setColor(new Color(25, 21, 34));

        if ((row + col) % 3 == 0) {
            g2.drawLine(x + 12, y + 9, x + 17, y + 13);
            g2.drawLine(x + 17, y + 13, x + 14, y + 17);
        }

        if ((row * 2 + col) % 4 == 0) {
            g2.drawLine(x + 34, y + 30, x + 39, y + 34);
            g2.drawLine(x + 39, y + 34, x + 35, y + 39);
        }

        // Moss patches
        if ((row + col) % 5 == 0) {

            g2.setColor(new Color(40, 76, 54));

            g2.fillRect(x + 5, y + 17, 9, 3);
            g2.fillRect(x + 8, y + 14, 5, 3);
        }

        // Glowing magical runes on selected walls
        if ((row + col) % 7 == 0) {

            int glow = (int) (35 + 35 *
                    Math.sin(animationTick * 0.09 + row + col));

            g2.setColor(new Color(105, 45, 185, glow));
            g2.fillRect(x + 31, y + 7, 11, 11);

            g2.setColor(new Color(165, 100, 255));
            g2.drawLine(x + 34, y + 9, x + 39, y + 9);
            g2.drawLine(x + 36, y + 9, x + 36, y + 15);
            g2.drawLine(x + 33, y + 15, x + 40, y + 15);
        }

        // Outer wall outline
        g2.setColor(new Color(15, 12, 25));
        g2.drawRect(x, y, tileSize, tileSize);
    }

    // ================= DUNGEON FLOOR =================

    private void drawFloor(Graphics2D g2, int x, int y,
            int row, int col) {

        g2.setColor(new Color(36, 32, 47));
        g2.fillRect(x, y, tileSize, tileSize);

        g2.setColor(new Color(46, 40, 58));
        g2.fillRect(x + 4, y + 4, 42, 42);

        g2.setColor(new Color(23, 20, 34));
        g2.drawRect(x, y, tileSize, tileSize);

        // Floor cracks
        g2.setColor(new Color(20, 17, 29));

        if ((row + col) % 3 == 0) {
            g2.drawLine(x + 8, y + 13, x + 17, y + 20);
            g2.drawLine(x + 17, y + 20, x + 13, y + 29);
        }

        // Tiny stone details
        g2.setColor(new Color(72, 62, 82));

        if ((row * 3 + col) % 4 == 0) {
            g2.fillRect(x + 32, y + 34, 3, 3);
            g2.fillRect(x + 12, y + 39, 2, 2);
        }

        // Subtle purple magic underneath the floor
        if ((row + col) % 6 == 0) {

            int alpha = (int) (12 + 10 *
                    Math.sin(animationTick * 0.07 + col));

            g2.setColor(new Color(130, 65, 220, alpha));
            g2.fillRect(x + 4, y + 4, 42, 42);
        }
    }

    // ================= ANCIENT DUNGEON DOOR =================

    private void drawDoor(Graphics2D g2, int x, int y) {

        boolean unlocked = keyCollected;

        int pulse = (int) (Math.sin(animationTick * 0.12) * 15);

        Color portalColor = unlocked
                ? new Color(145, 55, 255)
                : new Color(170, 35, 45);

        // Magical glow around the doorway
        int glowAlpha = unlocked ? 65 + pulse : 35 + pulse / 2;

        g2.setColor(new Color(
                portalColor.getRed(),
                portalColor.getGreen(),
                portalColor.getBlue(),
                Math.max(10, glowAlpha)));

        g2.fillRect(x + 2, y + 1, 46, 49);

        // Massive stone frame
        g2.setColor(new Color(38, 31, 52));
        g2.fillRect(x + 5, y + 3, 40, 45);

        g2.setColor(new Color(105, 83, 120));
        g2.fillRect(x + 7, y + 3, 36, 5);

        // Pillars
        g2.setColor(new Color(83, 68, 101));
        g2.fillRect(x + 6, y + 8, 7, 38);
        g2.fillRect(x + 37, y + 8, 7, 38);

        g2.setColor(new Color(130, 105, 145));
        g2.drawLine(x + 7, y + 9, x + 7, y + 43);
        g2.drawLine(x + 38, y + 9, x + 38, y + 43);

        // Door arch
        g2.setColor(new Color(110, 88, 130));
        g2.fillRect(x + 12, y + 6, 26, 5);

        g2.setColor(new Color(45, 35, 60));
        g2.fillRect(x + 13, y + 11, 24, 34);

        // Portal interior
        g2.setColor(new Color(13, 8, 25));
        g2.fillRect(x + 15, y + 12, 20, 32);

        // Animated portal waves
        for (int i = 0; i < 4; i++) {

            int waveY = y + 15 + ((animationTick * 2 + i * 9) % 27);

            int waveWidth = 12 + (i % 2) * 5;

            g2.setColor(new Color(
                    unlocked ? 160 : 170,
                    unlocked ? 75 : 30,
                    unlocked ? 255 : 45,
                    100 + i * 30));

            g2.fillRect(
                    x + 25 - waveWidth / 2,
                    waveY,
                    waveWidth,
                    2);
        }

        // Floating magical particles
        for (int i = 0; i < 5; i++) {

            int particleX = x + 17 + (i * 7) % 17;

            int particleY = y + 13 +
                    Math.floorMod(animationTick * (i + 1) + i * 8, 30);

            g2.setColor(unlocked
                    ? new Color(220, 170, 255)
                    : new Color(255, 90, 80));

            g2.fillRect(particleX, particleY, 2, 3);
        }

        // Door center emblem
        g2.setColor(new Color(220, 180, 100));
        g2.fillRect(x + 22, y + 22, 6, 10);

        g2.setColor(new Color(255, 225, 140));
        g2.drawRect(x + 21, y + 21, 8, 12);

        // Door base
        g2.setColor(new Color(65, 52, 79));
        g2.fillRect(x + 5, y + 43, 40, 5);

        // Magical rune above the entrance
        g2.setColor(unlocked
                ? new Color(200, 130, 255)
                : new Color(220, 75, 70));

        g2.drawRect(x + 20, y + 1, 10, 5);
        g2.drawLine(x + 25, y + 2, x + 25, y + 5);

        // Door outline
        g2.setColor(new Color(15, 10, 23));
        g2.drawRect(x + 5, y + 3, 40, 45);

        if (unlocked) {
            g2.setColor(new Color(205, 155, 255));
            g2.drawRect(x + 2, y + 1, 46, 48);
        }
    }

    // ================= COIN =================

    private void drawCoin(Graphics2D g2, int x, int y) {

        int bob = (int) (Math.sin(animationTick * 0.12) * 3);

        g2.setColor(new Color(255, 190, 30, 65));
        g2.fillOval(x + 8, y + 9 + bob, 34, 34);

        g2.setColor(new Color(255, 170, 15));
        g2.fillOval(x + 14, y + 13 + bob, 22, 22);

        g2.setColor(new Color(255, 225, 100));
        g2.drawOval(x + 16, y + 15 + bob, 18, 18);

        g2.setColor(new Color(255, 245, 175));
        g2.fillRect(x + 23, y + 17 + bob, 3, 12);
        g2.fillRect(x + 19, y + 21 + bob, 11, 3);
    }

    // ================= KEY =================

    private void drawKey(Graphics2D g2, int x, int y) {

        int bob = (int) (Math.sin(animationTick * 0.1) * 3);

        g2.setColor(new Color(255, 205, 55, 65));
        g2.fillOval(x + 7, y + 7 + bob, 36, 36);

        g2.setColor(new Color(255, 210, 65));
        g2.fillOval(x + 12, y + 13 + bob, 17, 17);

        g2.setColor(new Color(36, 31, 45));
        g2.fillOval(x + 17, y + 18 + bob, 7, 7);

        g2.setColor(new Color(255, 220, 100));
        g2.fillRect(x + 25, y + 19 + bob, 15, 5);
        g2.fillRect(x + 34, y + 23 + bob, 5, 7);
        g2.fillRect(x + 29, y + 23 + bob, 4, 5);
    }

    // ================= ENEMY GRAPHICS =================

    private void drawEnemy(Graphics2D g2, Enemy enemy) {

        if (!enemy.alive && enemy.deathTimer <= 0)
            return;

        int x = enemy.x;
        int y = enemy.y;

        // Death animation: monster dissolves into shadow particles
        if (!enemy.alive) {

            int alpha = Math.max(0, enemy.deathTimer * 15);

            Graphics2D death = (Graphics2D) g2.create();

            death.setColor(new Color(110, 35, 180, alpha));

            for (int i = 0; i < 7; i++) {

                int px = x + (i * 11 + animationTick * 2) % 30;
                int py = y + (i * 7 - animationTick * 2) % 35;

                death.fillRect(px, py, 4, 4);
            }

            death.dispose();
            return;
        }

        int bob = (int) (Math.sin(animationTick * 0.15 + x) * 2);
        int walk = (animationTick / 4) % 2;

        // Monster shadow underneath
        g2.setColor(new Color(0, 0, 0, 130));
        g2.fillOval(x - 3, y + 24, 35, 9);

        // Purple aura
        int aura = (int) (40 + 25 *
                Math.sin(animationTick * 0.12 + x));

        g2.setColor(new Color(125, 30, 200, aura));
        g2.fillOval(x - 5, y - 6 + bob, 39, 42);

        // Body
        g2.setColor(enemy.hitFlash > 0
                ? new Color(210, 170, 255)
                : new Color(39, 19, 62));

        g2.fillRect(x + 6, y + 7 + bob, 17, 20);
        g2.fillRect(x + 3, y + 12 + bob, 23, 12);

        // Head
        g2.setColor(new Color(23, 13, 38));
        g2.fillRect(x + 5, y + 1 + bob, 19, 15);

        g2.setColor(new Color(78, 35, 110));
        g2.fillRect(x + 8, y + 2 + bob, 13, 8);

        // Horns
        g2.setColor(new Color(130, 90, 160));

        g2.fillPolygon(
                new int[] { x + 7, x + 2, x + 8 },
                new int[] { y + 6 + bob, y - 4 + bob, y + 9 + bob },
                3);

        g2.fillPolygon(
                new int[] { x + 21, x + 27, x + 20 },
                new int[] { y + 6 + bob, y - 4 + bob, y + 9 + bob },
                3);

        // Glowing eyes
        int eyeGlow = (int) (190 + 65 *
                Math.sin(animationTick * 0.2));

        g2.setColor(new Color(255, 35, 70, eyeGlow));
        g2.fillRect(x + 9, y + 7 + bob, 4, 3);
        g2.fillRect(x + 17, y + 7 + bob, 4, 3);

        g2.setColor(new Color(255, 210, 220));
        g2.fillRect(x + 10, y + 7 + bob, 2, 2);
        g2.fillRect(x + 18, y + 7 + bob, 2, 2);

        // Arms
        g2.setColor(new Color(44, 20, 70));

        g2.fillRect(x, y + 12 + bob, 6, 12);
        g2.fillRect(x + 23, y + 12 + bob, 6, 12);

        // Claws
        g2.setColor(new Color(160, 115, 185));
        g2.fillRect(x + 1, y + 23 + bob, 2, 5);
        g2.fillRect(x + 26, y + 23 + bob, 2, 5);

        // Animated legs
        g2.setColor(new Color(26, 15, 43));

        if (walk == 0) {
            g2.fillRect(x + 7, y + 25 + bob, 6, 7);
            g2.fillRect(x + 17, y + 25 + bob, 6, 4);
        } else {
            g2.fillRect(x + 7, y + 25 + bob, 6, 4);
            g2.fillRect(x + 17, y + 25 + bob, 6, 7);
        }

        // Purple chest mark
        g2.setColor(new Color(175, 70, 255));
        g2.fillRect(x + 13, y + 16 + bob, 4, 5);

        // Health bar background
        g2.setColor(Color.BLACK);
        g2.fillRect(x - 1, y - 10, 30, 5);

        g2.setColor(new Color(65, 20, 40));
        g2.fillRect(x, y - 9, 28, 3);

        g2.setColor(new Color(220, 45, 75));
        g2.fillRect(
                x, y - 9,
                28 * enemy.health / enemy.maxHealth, 3);

        // Health bar border
        g2.setColor(new Color(160, 120, 180));
        g2.drawRect(x - 1, y - 10, 29, 4);
    }

    // ================= PLAYER GRAPHICS =================

    private void drawPlayer(Graphics2D g2) {

        if (damageCooldown > 0 && damageCooldown % 6 < 3)
            return;

        Graphics2D hero = (Graphics2D) g2.create();

        hero.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);

        int x = playerX - 8;
        int y = playerY - 24;

        int bob = movementAnimationTicks > 0
                ? (animationTick / 3) % 2
                : 0;

        int walk = movementAnimationTicks > 0
                ? (animationTick / 4) % 2
                : 0;

        y += bob;

        // ================= SHADOW =================

        hero.setColor(new Color(0, 0, 0, 160));
        hero.fillOval(x + 7, y + 43, 34, 9);

        // ================= BACK KATANA SHEATH =================

        hero.setColor(new Color(65, 25, 38));
        hero.fillRect(x + 28, y + 13, 5, 25);

        hero.setColor(new Color(150, 45, 65));
        hero.fillRect(x + 29, y + 15, 2, 20);

        hero.setColor(new Color(210, 175, 105));
        hero.fillRect(x + 27, y + 18, 7, 3);
        hero.fillRect(x + 27, y + 32, 7, 3);

        // ================= FLOWING SCARF =================

        int scarfWave = (int) (Math.sin(animationTick * 0.2) * 3);

        hero.setColor(new Color(95, 15, 43));

        Polygon scarfBack = new Polygon(
                new int[] {
                        x + 13, x + 3, x - 5 + scarfWave,
                        x + 1, x + 15
                },
                new int[] {
                        y + 15, y + 17, y + 28,
                        y + 29, y + 23
                },
                5);

        hero.fillPolygon(scarfBack);

        hero.setColor(new Color(195, 35, 75));

        hero.fillPolygon(new Polygon(
                new int[] {
                        x + 12, x + 4, x - 2 + scarfWave,
                        x + 3, x + 14
                },
                new int[] {
                        y + 16, y + 19, y + 26,
                        y + 25, y + 21
                },
                5));

        // ================= LEGS =================

        hero.setColor(new Color(14, 18, 31));

        if (walk == 0) {
            hero.fillRect(x + 12, y + 34, 8, 11);
            hero.fillRect(x + 24, y + 34, 8, 9);
        } else {
            hero.fillRect(x + 12, y + 34, 8, 9);
            hero.fillRect(x + 24, y + 34, 8, 11);
        }

        // Shin armour
        hero.setColor(new Color(48, 48, 75));
        hero.fillRect(x + 12, y + 37, 8, 5);
        hero.fillRect(x + 24, y + 37, 8, 5);

        hero.setColor(new Color(115, 85, 150));
        hero.fillRect(x + 13, y + 38, 6, 2);
        hero.fillRect(x + 25, y + 38, 6, 2);

        // Ninja boots
        hero.setColor(new Color(8, 10, 20));
        hero.fillRect(x + 9, y + 43, 12, 5);
        hero.fillRect(x + 23, y + 43, 12, 5);

        hero.setColor(new Color(120, 45, 75));
        hero.fillRect(x + 10, y + 44, 9, 2);
        hero.fillRect(x + 24, y + 44, 9, 2);

        // ================= BODY ARMOUR =================

        hero.setColor(new Color(12, 16, 29));
        hero.fillRect(x + 10, y + 17, 23, 19);

        // Chest armour
        hero.setColor(new Color(32, 35, 55));
        hero.fillRect(x + 12, y + 19, 19, 14);

        hero.setColor(new Color(66, 57, 91));
        hero.fillRect(x + 14, y + 20, 3, 10);
        hero.fillRect(x + 27, y + 20, 3, 10);

        // Purple chest emblem
        hero.setColor(new Color(150, 70, 220));
        hero.fillRect(x + 19, y + 22, 5, 7);

        hero.setColor(new Color(210, 145, 255));
        hero.fillRect(x + 20, y + 23, 3, 2);
        hero.fillRect(x + 21, y + 26, 2, 2);

        // Belt
        hero.setColor(new Color(90, 49, 48));
        hero.fillRect(x + 11, y + 32, 21, 4);

        hero.setColor(new Color(235, 180, 80));
        hero.fillRect(x + 20, y + 32, 4, 4);

        // ================= SHOULDERS =================

        hero.setColor(new Color(19, 22, 39));
        hero.fillRect(x + 5, y + 19, 9, 10);
        hero.fillRect(x + 30, y + 19, 9, 10);

        hero.setColor(new Color(85, 70, 112));
        hero.fillRect(x + 6, y + 20, 7, 3);
        hero.fillRect(x + 31, y + 20, 7, 3);

        // Shoulder armour highlights
        hero.setColor(new Color(155, 105, 205));
        hero.fillRect(x + 7, y + 20, 2, 2);
        hero.fillRect(x + 33, y + 20, 2, 2);

        // ================= ARMS =================

        hero.setColor(new Color(17, 21, 36));
        hero.fillRect(x + 5, y + 27, 7, 9);
        hero.fillRect(x + 32, y + 27, 7, 9);

        hero.setColor(new Color(75, 48, 83));
        hero.fillRect(x + 5, y + 29, 7, 3);
        hero.fillRect(x + 32, y + 29, 7, 3);

        // Gloves
        hero.setColor(new Color(8, 12, 24));
        hero.fillRect(x + 5, y + 35, 7, 4);
        hero.fillRect(x + 32, y + 35, 7, 4);

        // ================= NINJA HOOD =================

        // ================= NINJA HOOD =================

        // Head turns only when moving horizontally
        int headTurn = 0;

        if (facingX < 0) {
            headTurn = -3;
        } else if (facingX > 0) {
            headTurn = 3;
        }

        int hx = x + headTurn;

        // Hood
        hero.setColor(new Color(5, 8, 18));

        Polygon hood = new Polygon(
                new int[] {
                        hx + 11, hx + 14, hx + 27,
                        hx + 34, hx + 37, hx + 35,
                        hx + 31, hx + 13
                },
                new int[] {
                        y + 17, y + 5, y + 1,
                        y + 5, y + 16, y + 22,
                        y + 24, y + 24
                },
                8);

        hero.fillPolygon(hood);

        // Hood highlights
        hero.setColor(new Color(43, 42, 69));
        hero.fillRect(hx + 16, y + 6, 14, 3);
        hero.fillRect(hx + 12, y + 10, 4, 8);
        hero.fillRect(hx + 32, y + 10, 3, 7);

        // Purple hood trim
        hero.setColor(new Color(115, 58, 165));
        hero.fillRect(hx + 14, y + 17, 21, 3);

        // Face mask
        hero.setColor(new Color(18, 22, 37));
        hero.fillRect(hx + 14, y + 13, 20, 10);

        hero.setColor(new Color(38, 43, 62));
        hero.fillRect(hx + 16, y + 17, 16, 5);

        // Glowing eyes
        int eyeGlow = (int) (190 + 65 *
                Math.sin(animationTick * 0.18));

        hero.setColor(new Color(30, 190, 255, eyeGlow));
        hero.fillRect(hx + 17, y + 13, 5, 3);
        hero.fillRect(hx + 27, y + 13, 5, 3);

        hero.setColor(new Color(210, 250, 255));
        hero.fillRect(hx + 18, y + 13, 2, 2);
        hero.fillRect(hx + 28, y + 13, 2, 2);

        // Mask details
        hero.setColor(new Color(120, 75, 140));
        hero.fillRect(hx + 18, y + 20, 11, 2);

        // Forehead jewel
        hero.setColor(new Color(170, 90, 255));
        hero.fillRect(hx + 23, y + 7, 4, 4);

        hero.setColor(new Color(230, 190, 255));
        hero.fillRect(hx + 24, y + 7, 2, 2);

        // Scarf front stays in place
        hero.setColor(new Color(150, 25, 58));
        hero.fillRect(x + 12, y + 22, 22, 3);

        hero.setColor(new Color(225, 55, 85));
        hero.fillRect(x + 15, y + 22, 14, 2);

        // Armour highlights
        hero.setColor(new Color(180, 145, 210));
        hero.fillRect(x + 13, y + 27, 2, 2);
        hero.fillRect(x + 29, y + 27, 2, 2);

        hero.dispose();
    }

    // ================= SWORD ATTACK =================

    private void drawAttack(Graphics2D g2) {

        int frameNumber = 10 - attackTimer;

        double baseAngle = Math.atan2(facingY, facingX);
        double swingAngle = baseAngle - 0.72 + frameNumber * 0.15;

        Graphics2D sword = (Graphics2D) g2.create();

        sword.translate(
                playerX + playerSize / 2,
                playerY + playerSize / 2);

        sword.rotate(swingAngle);

        sword.setColor(new Color(65, 35, 24));
        sword.fillRect(-15, -2, 13, 5);

        sword.setColor(new Color(190, 145, 75));
        sword.fillRect(-17, -3, 4, 7);
        sword.fillRect(-5, -4, 4, 9);

        sword.setColor(new Color(210, 166, 75));
        sword.fillRect(-3, -10, 5, 21);

        sword.setColor(new Color(255, 220, 130));
        sword.fillRect(-2, -9, 2, 18);

        Polygon blade = new Polygon(
                new int[] { 1, 31, 39, 31, 1 },
                new int[] { -4, -4, 0, 4, 4 }, 5);

        sword.setColor(new Color(105, 130, 155));
        sword.fillPolygon(blade);

        Polygon brightEdge = new Polygon(
                new int[] { 3, 30, 36, 30, 3 },
                new int[] { -3, -3, 0, 0, 0 }, 5);

        sword.setColor(new Color(235, 245, 255));
        sword.fillPolygon(brightEdge);

        sword.setColor(new Color(75, 95, 120));
        sword.drawLine(3, 3, 31, 3);

        sword.setColor(new Color(255, 225, 145));
        sword.fillRect(2, -1, 3, 2);

        if (frameNumber >= 2 && frameNumber <= 7) {

            sword.setColor(new Color(130, 195, 255, 170));

            sword.fillRect(18, -8, 13, 2);
            sword.fillRect(27, -6, 8, 2);
            sword.fillRect(32, -4, 4, 2);
        }

        sword.dispose();
    }

    // ================= HEALTH BAR =================

    private void drawHealthBar(Graphics2D g2) {

        int barX = 690;
        int barY = 12;
        int barWidth = 150;
        int barHeight = 15;

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(barX, barY, barWidth, barHeight);

        if (health > 50) {
            g2.setColor(new Color(50, 210, 90));

        } else if (health > 20) {
            g2.setColor(Color.ORANGE);

        } else {
            g2.setColor(Color.RED);
        }

        g2.fillRect(
                barX, barY,
                barWidth * health / 100,
                barHeight);

        g2.setColor(Color.WHITE);
        g2.drawRect(barX, barY, barWidth, barHeight);

        g2.setFont(new Font("Monospaced", Font.BOLD, 12));
        g2.drawString("HP: " + health, barX, 43);
    }

    // ================= ENEMY CLASS =================

    private class Enemy {

        int x;
        int y;

        int health;
        final int maxHealth;

        final int size = 28;

        boolean alive = true;

        int deathTimer = 0;
        int hitFlash = 0;

        Enemy(int x, int y, int health) {

            this.x = x;
            this.y = y;

            this.health = health;
            this.maxHealth = health;
        }
    }
}
