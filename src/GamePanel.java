import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;

public class GamePanel extends JPanel {

    final int UNIT_SIZE = GameSettings.UNIT_SIZE;
    final int SCREEN_WIDTH;
    final int SCREEN_HEIGHT;
    final int GAME_UNITS;

    private List<Point> snakeBody;
    private Point food;
    private Random random;

    private boolean running = false; // 標記遊戲是否結束 (Game Over)
    private boolean paused = false;  // 標記遊戲是否暫停
    private int score = 0;
    private final Consumer<Void> restartAction;

    private AI_Decision_Maker ai;
    private char direction = 'R';
    private String failureReason = "";

    // 遊戲步數
    private int steps;
    private int MAX_STEPS;

    public GamePanel(int screenWidth, int screenHeight, AI_Decision_Maker ai, Consumer<Void> restartAction) {
        this.SCREEN_WIDTH = screenWidth;
        this.SCREEN_HEIGHT = screenHeight;
        this.GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / (UNIT_SIZE * UNIT_SIZE);
        this.ai = ai;
        this.restartAction = restartAction;
        this.steps = 0;
        this.MAX_STEPS = SCREEN_WIDTH * SCREEN_HEIGHT;

        // 設定面板填滿整個視窗
        this.setPreferredSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize()));
        this.setBackground(Color.WHITE); // 背景設為黑色，遊戲區域外的地方就是黑邊
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter()); // 為之後的自動化邏輯做準備

        random = new Random();
        startGame();
    }

    public void startGame() {
        snakeBody = new ArrayList<>();
        direction = 'R'; // 重新開始時，方向重置為向右
        // 重置失敗原因
        failureReason = "";
        score = 0;
        paused = false;
        running = true;

        // 計算蛇的初始位置在遊戲畫面的中心
        int startX = (SCREEN_WIDTH / (2 * UNIT_SIZE)) * UNIT_SIZE;
        int startY = (SCREEN_HEIGHT / (2 * UNIT_SIZE)) * UNIT_SIZE;

        // 初始蛇頭位置
        snakeBody.add(new Point(startX, startY));
        for (int i = 1; i <= 3; i++) {
            snakeBody.add(new Point(startX - i * UNIT_SIZE, startY));
        }

        // 生成第一顆食物
        newFood();
    }

    // 繼續遊戲的方法
    public void resumeGame() {
        paused = false;
        // 確保面板重新獲得焦點以繼續監聽鍵盤
        this.requestFocusInWindow();
    }

    // Getter 方法讓 Main 可以檢查狀態
    public boolean isRunning() {
        return running;
    }

    // 隨機生成食物
    public void newFood() {
        /// 檢查新生成的食物位置是否與蛇的身體重疊。如果重疊，就重新生成食物，直到找到一個空白的位置。
        boolean foodOnSnake;
        do {
            foodOnSnake = false;
            int foodX = random.nextInt(SCREEN_WIDTH / UNIT_SIZE) * UNIT_SIZE;
            int foodY = random.nextInt(SCREEN_HEIGHT / UNIT_SIZE) * UNIT_SIZE;
            food = new Point(foodX, foodY);
            for (Point segment : snakeBody) {
                if (segment.equals(food)) {
                    foodOnSnake = true;
                    break;
                }
            }
        } while (foodOnSnake);
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        Point offset = getOffset();
        int offsetX = offset.x;
        int offsetY = offset.y;

        // 畫網格線 (選用)
        g.setColor(new Color(10, 10, 10, 30));
        /// 計算水平和垂直網格線的數量
        /// 如果畫面尺寸不能被 UNIT_SIZE 整除，則多畫一條線
        int horizontalLines = GameSettings.screenHeight / UNIT_SIZE;
        if (GameSettings.screenHeight % UNIT_SIZE != 0) {
            horizontalLines++;
        }
        int verticalLines = GameSettings.screenWidth / UNIT_SIZE;
        if (GameSettings.screenWidth % UNIT_SIZE != 0) {
            verticalLines++;
        }

        // 畫垂直網格線
        for (int i = 0; i <= verticalLines; i++) { // 修正迴圈條件，確保從 0 到最後一條線都畫
            g.drawLine(offsetX + i * UNIT_SIZE, offsetY, offsetX + i * UNIT_SIZE, offsetY + GameSettings.screenHeight);
        }
        // 畫水平網格線
        for (int i = 0; i <= horizontalLines; i++) { // 修正迴圈條件
            g.drawLine(offsetX, offsetY + i * UNIT_SIZE, offsetX + GameSettings.screenWidth, offsetY + i * UNIT_SIZE);
        }

        // 畫分數
        g.setColor(Color.red);
        g.setFont(new Font("Ink Free", Font.BOLD, 40));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Score: " + score, (getWidth() - metrics.stringWidth("Score: " + score)) / 2, g.getFont().getSize());

        if (running || paused) { // 只要遊戲還沒結束，就畫蛇跟食物
            // 畫蛇
            for (int i = 0; i < snakeBody.size(); i++) {
                if (i == 0) {
                    g.setColor(Color.green);
                } else {
                    g.setColor(new Color(45, 180, 0));
                }
                g.fillRect(offsetX + snakeBody.get(i).x, offsetY + snakeBody.get(i).y, UNIT_SIZE, UNIT_SIZE);
            }
            g.setColor(Color.red);
            g.fillOval(offsetX + food.x, offsetY + food.y, UNIT_SIZE, UNIT_SIZE);

        } else {
            gameOver(g, offsetX, offsetY);
        }
    }

    public void move() {
        direction = ai.decideDirection(snakeBody, food, new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));

        Point head = new Point(snakeBody.get(0));
        switch (direction) {
            case 'U': head.y -= UNIT_SIZE; break;
            case 'D': head.y += UNIT_SIZE; break;
            case 'L': head.x -= UNIT_SIZE; break;
            case 'R': head.x += UNIT_SIZE; break;
        }

        snakeBody.add(0, head);

        if (head.equals(food)) {
            score++;
            newFood();
        } else {
            snakeBody.remove(snakeBody.size() - 1);
        }
    }

    public void checkCollisions() {
        Point head = snakeBody.get(0);
        for (int i = 1; i < snakeBody.size(); i++) {
            if (head.equals(snakeBody.get(i))) {
                running = false;
                failureReason = "Crushed itself!";
            }
        }
        if (head.x < 0 || head.x >= SCREEN_WIDTH || head.y < 0 || head.y >= SCREEN_HEIGHT) {
            running = false;
            failureReason = "Crushed a wall!";
        }
    }

    public void checkMaxStep(){
        if(steps > MAX_STEPS){
            running = false;
            failureReason = "The snake is in the Endless loop!";
        }
    }

    public void gameOver(Graphics g, int offsetX, int offsetY) {

        // 顯示分數
        g.setColor(Color.red);
        g.setFont(new java.awt.Font("Ink Free", java.awt.Font.BOLD, 40));
        java.awt.FontMetrics metrics1 = getFontMetrics(g.getFont());
        int score = snakeBody.size() - 3; // 初始長度為 3
        g.drawString("Score: " + score, (SCREEN_WIDTH - metrics1.stringWidth("Score: " + score)) / 2, offsetY + g.getFont().getSize());

        // 顯示失敗原因
        g.setColor(Color.red);
        g.setFont(new java.awt.Font("Ink Free", java.awt.Font.BOLD, 30));
        java.awt.FontMetrics metricsReason = getFontMetrics(g.getFont());
        g.drawString(failureReason, offsetX + (SCREEN_WIDTH - metricsReason.stringWidth("Game Over")) / 2, offsetY + g.getFont().getSize() + 50);

        // 顯示遊戲結束訊息
        g.setColor(Color.red);
        g.setFont(new Font("Ink Free", Font.BOLD, 75));
        FontMetrics metrics = getFontMetrics(g.getFont());
        g.drawString("Game Over", offsetX + (SCREEN_WIDTH - metrics.stringWidth("Game Over")) / 2, offsetY + SCREEN_HEIGHT / 2);

        System.out.println("Game Over - Reason: " + failureReason + ", Score: " + score);

        Timer restartTimer = new Timer(1500, e -> startGame());
        restartTimer.setRepeats(false); // 只執行一次
        restartTimer.start();
    }

    // 統一的鍵盤監聽器
    public class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
            if (e.getKeyCode() == KeyEvent.VK_O) {
                if (!paused) {
                    pauseGame();
                    // 傳遞 GamePanel 實例和重啟動作給 SettingsFrame
                    new SettingsFrame(GamePanel.this, restartAction);
                }
            }
        }
    }

    // 置中用偏移量計算
    private Point getOffset() {
        int panelWidth = this.getWidth();
        int panelHeight = this.getHeight();
        int offsetX = (panelWidth - SCREEN_WIDTH) / 2;
        int offsetY = (panelHeight - SCREEN_HEIGHT) / 2;
        return new Point(offsetX, offsetY);
    }

    public void pauseGame() {
        paused = true;
    }

    public void updateGame() {
        steps++;
        move();
        checkCollisions();
        checkMaxStep();
        repaint();
    }
}