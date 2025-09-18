import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Random;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;
import java.util.List;

public class GamePanel extends JPanel implements ActionListener  { // 實現 ActionListener 介面

    // 遊戲常數
    final int UNIT_SIZE = 25; // 每個方塊的大小
    final int SCREEN_WIDTH;
    final int SCREEN_HEIGHT;
    final int GAME_UNITS;
    boolean paused = false; // 新增變數來追蹤遊戲是否暫停

    // 蛇的身體座標，改為 List 介面
    List<Point> snakeBody;
    // 食物的座標
    Point food;
    // 隨機數生成器
    Random random;
    // 遊戲迴圈
    Timer timer;
    // 遊戲狀態
    boolean running = false;
    // 在 GamePanel 類別中新增一個 AI 決策器的變數
    private AI_Decision_Maker ai;
    // 原有的 direction 變數改為私有
    private char direction;
    // 儲存遊戲失敗的原因
    private String failureReason = "";


    public GamePanel() {
        // 從 GameSettings 類別中取得畫面大小
        this.SCREEN_WIDTH = GameSettings.screenWidth;
        this.SCREEN_HEIGHT = GameSettings.screenHeight;
        this.GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / (UNIT_SIZE * UNIT_SIZE);

        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        // 設定背景顏色
        this.setBackground(Color.white);
        // 設定焦點，以便處理鍵盤事件 (未來可能會用到)
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter()); // 為之後的自動化邏輯做準備

        random = new Random();

        // 步驟1: 先使用預設 AI，讓遊戲在 AI 訓練時不會凍結
        this.ai = new TraditionalAI();

        // 步驟2: 創建一個 GeneticAlgorithmAI 實例
        // 並將 GamePanel 自身的參考傳遞給它，以便在訓練完成時能回呼
        GeneticAlgorithmAI geneticAI = new GeneticAlgorithmAI(bestGenes -> {
            SwingUtilities.invokeLater(() -> {
                // 步驟3: 訓練完成後，切換 GamePanel 的 AI 決策者
                this.ai = new AI_Decision_Maker() {
                    @Override
                    public char decideDirection(List<Point> snakeBody, Point food, Dimension boardSize) {
                        // 這裡應該是根據 `bestGenes` 運算決策的邏輯
                        // 為了簡化，我們暫時使用一個簡單的範例
                        double foodXDist = food.x - snakeBody.get(0).x;
                        double foodYDist = food.y - snakeBody.get(0).y;
                        if (Math.abs(foodXDist) > Math.abs(foodYDist)) {
                            return (foodXDist > 0) ? 'R' : 'L';
                        } else {
                            return (foodYDist > 0) ? 'D' : 'U';
                        }
                    }
                };
                System.out.println("AI train completed。The Snake smarter now！");
            });
        });

        // 步驟4: 在背景執行緒中啟動訓練，不會阻塞 GUI
        geneticAI.startTraining();

        // 初始化遊戲物件
        initGame();
    }

    // 初始化遊戲狀態
    public void initGame() {
        // 在重新開始遊戲時，先檢查並停止舊的 Timer
        if (timer != null) {
            timer.stop();
        }
        // 初始化蛇的身體
        snakeBody = new ArrayList<>();
        direction = 'R'; // 重新開始時，方向重置為向右
        // 重置失敗原因
        this.failureReason = "";

        // 計算蛇的初始位置在遊戲畫面的中心
        int startX = (SCREEN_WIDTH / (2 * UNIT_SIZE)) * UNIT_SIZE;
        int startY = (SCREEN_HEIGHT / (2 * UNIT_SIZE)) * UNIT_SIZE;
        // 初始蛇頭位置
        snakeBody.add(new Point(startX, startY));
        // 初始蛇的長度為 3
        for (int i = 0; i < 3; i++) {
            snakeBody.add(new Point(startX - i * UNIT_SIZE, startY));
        }

        // 生成第一顆食物
        newFood();

        // 啟動遊戲迴圈
        running = true;
        // 使用 GameSettings 的 gameSpeed
        timer = new Timer(GameSettings.gameSpeed, this);
        timer.start();
    }
    // 套用設定並重新啟動遊戲的方法
    public void applyAndRestart() {
        // 先停止舊的遊戲迴圈
        if (timer != null) {
            timer.stop();
        }

        // 重新初始化遊戲，新的參數將在 initGame 中被讀取
        initGame();

        // 確保遊戲狀態為運行中，並開始新的計時器
        running = true;
        paused = false;
        timer = new Timer(GameSettings.gameSpeed, this);
        timer.start();
    }
    // 繼續遊戲的方法
    public void resumeGame() {
        paused = false;
        timer.start();
    }

    // 隨機生成食物
    public void newFood() {
        /// 檢查新生成的食物位置是否與蛇的身體重疊。如果重疊，就重新生成食物，直到找到一個空白的位置。
        boolean foodOnSnake = true;
        while (foodOnSnake) {
            int foodX = random.nextInt((int) (SCREEN_WIDTH / UNIT_SIZE)) * UNIT_SIZE;
            int foodY = random.nextInt((int) (SCREEN_HEIGHT / UNIT_SIZE)) * UNIT_SIZE;
            food = new Point(foodX, foodY);

            foodOnSnake = false;
            // 檢查食物是否在蛇的身體上
            for (Point segment : snakeBody) {
                if (segment.equals(food)) {
                    foodOnSnake = true;
                    break;
                }
            }
        }
    }

    // 覆寫 JPanel 的 paintComponent 方法來繪製遊戲內容
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    // 繪製遊戲畫面
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

        // 如果遊戲正在運行，畫蛇和食物
        if (running) {
            // 畫食物
            g.setColor(Color.red);
            g.fillOval(offsetX + food.x, offsetY + food.y, UNIT_SIZE, UNIT_SIZE);

            // 畫蛇
            for (int i = 0; i < snakeBody.size(); i++) {
                if (i == 0) {
                    g.setColor(Color.green);
                } else {
                    g.setColor(new Color(45, 180, 0));
                }
                g.fillRect(offsetX + snakeBody.get(i).x, offsetY + snakeBody.get(i).y, UNIT_SIZE, UNIT_SIZE);
            }
        } else {
            // 遊戲結束畫面
            gameOver(g, offsetX, offsetY, this.getWidth());
        }
    }

    // 處理遊戲迴圈的事件
    @Override
    public void actionPerformed(ActionEvent e) {
        if (running && !paused) {
            // 讓 AI 決定移動方向
            char aiDecision = ai.decideDirection(snakeBody, food, new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
//            direction = aiDecision;

            // 2. 檢查 AI 的決策是否為 180 度迴轉
            if (isValidDirection(aiDecision)) {
                // 如果決策有效，更新方向
                direction = aiDecision;
            } else {
                // 如果無效，不改變方向，蛇會繼續前進
                // 這個 else 區塊是空的，所以 direction 保持不變
            }

            // 2. 準備移動
            Point newHead = new Point(snakeBody.get(0));
            switch (direction) {
                case 'U': newHead.y -= UNIT_SIZE; break;
                case 'D': newHead.y += UNIT_SIZE; break;
                case 'L': newHead.x -= UNIT_SIZE; break;
                case 'R': newHead.x += UNIT_SIZE; break;
            }

            // 3. 檢查下一格是否為食物
            boolean foodEaten = newHead.equals(food);

            // 4. 進行移動
            snakeBody.add(0, newHead); // 增加新的蛇頭
            if (!foodEaten) {
                snakeBody.remove(snakeBody.size() - 1); // 如果沒有吃到食物，移除舊的尾巴
            }

            // 5. 檢查碰撞
            String collisionType = checkCollisions();
            if (collisionType != null) {
                running = false;

                // 新增偵錯訊息輸出
                System.out.println("debug info：");
                System.out.println("fail reason：" + collisionType);
                System.out.println("head：" + snakeBody.get(0));
                System.out.println("body：" + snakeBody);
                System.out.println("------------------------------------");

                this.failureReason = "Game Over: " + collisionType;
            }

            // 6. 如果吃到食物，放置新食物
            if (foodEaten) {
                newFood();
            }
        }
        // 如果遊戲結束，停止 Timer
        if (!running) {
            timer.stop();
        }
        // 重新繪製畫面
        repaint();
    }

    // 遊戲結束處理，加入偏移量參數
    public void gameOver(Graphics g, int offsetX, int offsetY, int panelWidth) {
        // 停止主要的遊戲迴圈
        if (timer != null) {
            timer.stop();
        }

        // 顯示分數
        g.setColor(Color.red);
        g.setFont(new java.awt.Font("Ink Free", java.awt.Font.BOLD, 40));
        java.awt.FontMetrics metrics1 = getFontMetrics(g.getFont());
        int score = snakeBody.size() - 3; // 初始長度為 3
        g.drawString("Score: " + score, (panelWidth - metrics1.stringWidth("Score: " + score)) / 2, offsetY + g.getFont().getSize());

        // 顯示失敗原因
        g.setColor(Color.red);
        g.setFont(new java.awt.Font("Ink Free", java.awt.Font.BOLD, 30));
        java.awt.FontMetrics metricsReason = getFontMetrics(g.getFont());
        g.drawString(failureReason, (panelWidth - metricsReason.stringWidth(failureReason)) / 2, offsetY + g.getFont().getSize() + 50);

        // 顯示遊戲結束訊息
        g.setColor(Color.red);
        g.setFont(new java.awt.Font("Ink Free", java.awt.Font.BOLD, 75));
        java.awt.FontMetrics metrics2 = getFontMetrics(g.getFont());
        g.drawString("Game Over", (panelWidth - metrics2.stringWidth("Game Over")) / 2, offsetY + GameSettings.screenHeight / 2);

        // 讓程式延遲五秒後重新開始
        // 我們需要使用 Swing 的 Timer 來避免阻塞事件處理線程
        Timer restartTimer = new Timer(1500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 重新開始遊戲
                initGame();
                // 停止這個計時器
                ((Timer)e.getSource()).stop();
            }
        });
        restartTimer.setRepeats(false); // 只執行一次
        restartTimer.start();
    }

    // 檢查方向是否合法（防止 180 度迴轉）
    private boolean isValidDirection(char newDirection) {
        switch (newDirection) {
            case 'U':
                return direction != 'D';
            case 'D':
                return direction != 'U';
            case 'L':
                return direction != 'R';
            case 'R':
                return direction != 'L';
            default:
                return false;
        }
    }
    // 獨立的碰撞檢查方法，並回傳失敗原因
    public String checkCollisions() {
        Point head = snakeBody.get(0);

        // 檢查是否撞到自己（從第二節身體開始檢查，因為第一節是剛移動的頭）
        for (int i = 1; i < snakeBody.size(); i++) {
            if (head.equals(snakeBody.get(i))) {
                return "crush self!";
            }
        }
        // 檢查是否撞牆
        if (head.x < 0 || head.x >= SCREEN_WIDTH || head.y < 0 || head.y >= SCREEN_HEIGHT) {
            return "crush board!";
        }
        return null; // 沒有碰撞
    }

    // (選用) 鍵盤事件處理，雖然是自動化，但可以留著用於測試
    private class MyKeyAdapter extends java.awt.event.KeyAdapter {
        @Override
        public void keyPressed(java.awt.event.KeyEvent e) {
            // 如果按下 ESC 鍵
            if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                // 退出程式
                System.exit(0);
            }
            if (e.getKeyCode() == KeyEvent.VK_O) {
                if (!paused) {
                    pauseGame();
                    // 開啟設定視窗，並傳入 GamePanel 的實例
                    SwingUtilities.invokeLater(() -> new SettingsFrame(GamePanel.this));
                }
            }
        }
    }

    // 置中用偏移量計算
    private Point getOffset() {
        int panelWidth = this.getWidth();
        int panelHeight = this.getHeight();
        int offsetX = (panelWidth - GameSettings.screenWidth) / 2;
        int offsetY = (panelHeight - GameSettings.screenHeight) / 2;
        return new Point(offsetX, offsetY);
    }

    private void pauseGame() {
        paused = true;
        timer.stop();
        repaint();
    }

    // 檢查碰撞
    private boolean isCollision(Point head, char direction, List<Point> snakeBody, Dimension boardSize) {
        int nextX = head.x;
        int nextY = head.y;
        int UNIT_SIZE = 25; // 與 GamePanel 保持一致

        switch (direction) {
            case 'U': nextY -= UNIT_SIZE; break;
            case 'D': nextY += UNIT_SIZE; break;
            case 'L': nextX -= UNIT_SIZE; break;
            case 'R': nextX += UNIT_SIZE; break;
        }

        // 檢查撞牆
        if (nextX < 0 || nextX >= boardSize.width || nextY < 0 || nextY >= boardSize.height) {
            return true;
        }

        // 檢查撞到自己
        for (int i = 1; i < snakeBody.size(); i++) {
            if (nextX == snakeBody.get(i).x && nextY == snakeBody.get(i).y) {
                return true;
            }
        }
        return false;
    }
    // 判斷當前方向
    private char getCurrentDirection(List<Point> snakeBody) {
        if (snakeBody.size() < 2) return 'R';
        Point head = snakeBody.get(0);
        Point neck = snakeBody.get(1);
        if (head.x > neck.x) return 'R';
        if (head.x < neck.x) return 'L';
        if (head.y > neck.y) return 'D';
        if (head.y < neck.y) return 'U';
        return 'R';
    }

    // 計算左轉或右轉的方向
    private char turnLeft(char direction) {
        switch (direction) {
            case 'U': return 'L';
            case 'L': return 'D';
            case 'D': return 'R';
            case 'R': return 'U';
        }
        return direction;
    }

    private char turnRight(char direction) {
        switch (direction) {
            case 'U': return 'R';
            case 'R': return 'D';
            case 'D': return 'L';
            case 'L': return 'U';
        }
        return direction;
    }
}