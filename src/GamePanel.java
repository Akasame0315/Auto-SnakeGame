import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;
import java.awt.event.KeyAdapter;
import java.util.concurrent.Semaphore;

public class GamePanel extends JPanel implements ActionListener  { // 實現 ActionListener 介面

    // 遊戲常數
    final int UNIT_SIZE = 25;
    final int SCREEN_WIDTH;
    final int SCREEN_HEIGHT;
    final int GAME_UNITS;
    private int score;
    private int moves;
    ArrayList<Point> snakeBody;
    Point food;
    Random random;
    Timer timer;
    boolean running;
    boolean paused = false; // 新增變數來追蹤遊戲是否暫停
    char direction = 'R'; //蛇的移動方向預設向右

    // 神經網路
    private NeuralNetwork brain;
    // 將靜態屬性移出，讓 GamePanel 獨立
    private static final int MAX_MOVES_WITHOUT_FOOD = 2000;
    // 用於主程式等待遊戲結束的信號量
    private Semaphore gameSemaphore;

    public GamePanel(NeuralNetwork brain) { //新增建構子來接收神經網路
        // 從 GameSettings 類別中取得畫面大小
        this.SCREEN_WIDTH = GameSettings.screenWidth;
        this.SCREEN_HEIGHT = GameSettings.screenHeight;
        this.GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / (UNIT_SIZE * UNIT_SIZE);

        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        this.setBackground(Color.white);
        // 設定焦點，以便處理鍵盤事件 (未來可能會用到)
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter());

        this.brain = brain;
        this.random = new Random();
        this.gameSemaphore = new Semaphore(0); // 初始為0，需要 acquire() 才能繼續
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
        score = 0;
        moves = 0;
        // 計算蛇的初始位置在遊戲畫面的中心
        int startX = (SCREEN_WIDTH / (2 * UNIT_SIZE)) * UNIT_SIZE;
        int startY = (SCREEN_HEIGHT / (2 * UNIT_SIZE)) * UNIT_SIZE;
        // 初始蛇頭位置
        snakeBody.add(new Point(startX, startY));
        // 初始蛇的長度為 3
        for (int i = 0; i < 3; i++) {
            snakeBody.add(new Point(startX - i * UNIT_SIZE, startY));
        }
        newFood();
        // 啟動遊戲迴圈
        running = true;
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

            // AI決策視覺化
            if (brain != null) {
                g.setColor(Color.black);
                g.setFont(new Font("Arial", Font.PLAIN, 12));
                double[] finalOutputs = brain.getFinalOutputs(getInputs());
                g.drawString("AI Outputs:", offsetX + 10, offsetY + SCREEN_HEIGHT + 30);
                g.drawString("Front: " + String.format("%.2f", finalOutputs[0]), offsetX + 10, offsetY + SCREEN_HEIGHT + 45);
                g.drawString("Left: " + String.format("%.2f", finalOutputs[1]), offsetX + 10, offsetY + SCREEN_HEIGHT + 60);
                g.drawString("Right: " + String.format("%.2f", finalOutputs[2]), offsetX + 10, offsetY + SCREEN_HEIGHT + 75);
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
            // 自動判斷食物方向並轉向(改用神經網路方法)
            // 取得輸入
            double[] inputs = getInputs();

//            // --- 顯示神經網路的輸入 ---
//            System.out.println("Inputs: " + java.util.Arrays.toString(inputs));
//            // 取得神經網路的最終輸出
//            double[] finalOutputs = brain.getFinalOutputs(inputs);
//            // --- 顯示神經網路的輸出 ---
//            System.out.println("Outputs (Straight, Left, Right): " + java.util.Arrays.toString(finalOutputs));

            // 讓神經網路做出決策
            int output = brain.predict(inputs);
            setDirectionFromOutput(output);
            move();
            // 呼叫碰撞偵測方法
            checkCollisions();
            // 呼叫吃食物方法
            checkFood();
        }
        if (!running) {
            timer.stop();
            // 如果有設定信號量，則在遊戲結束時釋放它
            if (gameSemaphore != null) {
                gameSemaphore.release();
            }
        }

        // 重新繪製畫面
        repaint();
    }
    // 以下為下一步要實作的方法
    public void move() {
        // 這裡寫蛇的移動邏輯
        // 暫時讓它動一下
        // 每次移動都將蛇的身體往後移一格
        for(int i = snakeBody.size() - 1; i > 0; i--) {
            snakeBody.get(i).setLocation(snakeBody.get(i-1));
        }

        // 根據當前方向移動蛇頭
        switch (direction) {
            case 'U': // 上
                snakeBody.get(0).y = snakeBody.get(0).y - UNIT_SIZE;
                break;
            case 'D': // 下
                snakeBody.get(0).y = snakeBody.get(0).y + UNIT_SIZE;
                break;
            case 'L': // 左
                snakeBody.get(0).x = snakeBody.get(0).x - UNIT_SIZE;
                break;
            case 'R': // 右
                snakeBody.get(0).x = snakeBody.get(0).x + UNIT_SIZE;
                break;
        }
    }

    // 檢查蛇頭是否吃到食物(重疊)，增加蛇的長度
    public void checkFood() {
        if (snakeBody.get(0).equals(food)) {
            score++;
            // 取得蛇尾的位置
            Point lastSegment = snakeBody.get(snakeBody.size() - 1);
            /// 根據蛇的移動方向，計算新的身體節點應該位於哪裡,
            /// 這是一種簡單的處理方式，讓新身體出現在看得見的地方，
            /// 並在下一幀移動時自然地接上蛇尾。
            int newX = lastSegment.x;
            int newY = lastSegment.y;

            // 新增一個新的身體節點，其位置與蛇尾相同
            snakeBody.add(new Point(newX, newY));
            // 生成新的食物
            newFood();
        }
    }

    // 碰撞檢查
    public void checkCollisions() {
        // 1. 檢查蛇頭是否撞到自己
        // 從蛇身體的第三個節點 (索引 2) 開始檢查
        for (int i = 2; i < snakeBody.size(); i++) {
            if (snakeBody.get(0).equals(snakeBody.get(i))) {
                running = false;
            }
        }

        // 2. 檢查蛇頭是否撞牆
        if (snakeBody.get(0).x < 0 || snakeBody.get(0).x >= SCREEN_WIDTH ||
                snakeBody.get(0).y < 0 || snakeBody.get(0).y >= SCREEN_HEIGHT) {
            running = false;
        }

        // 如果遊戲結束，停止 Timer
        if (!running) {
            timer.stop();
            // 釋放信號，讓主程式繼續
            gameSemaphore.release();
        }
    }

    // 遊戲結束處理，加入偏移量參數
    public void gameOver(Graphics g, int offsetX, int offsetY, int panelWidth) {
        // 停止主要的遊戲迴圈
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        // 顯示分數
        g.setColor(Color.red);
        g.setFont(new java.awt.Font("Ink Free", java.awt.Font.BOLD, 40));
        java.awt.FontMetrics metrics1 = getFontMetrics(g.getFont());
        int score = snakeBody.size() - 3; // 初始長度為 3
        g.drawString("Score: " + score, (panelWidth - metrics1.stringWidth("Score: " + score)) / 2, offsetY + g.getFont().getSize());

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

    // 實作一個檢查碰撞的方法
    public boolean isCollision(int nextX, int nextY) {
        // 檢查是否撞牆
        if (nextX < 0 || nextX >= SCREEN_WIDTH || nextY < 0 || nextY >= SCREEN_HEIGHT) {
            return true;
        }
        // 檢查是否撞到自己
        for (int i = snakeBody.size() - 1; i > 0; i--) {
            if (nextX == snakeBody.get(i).x && nextY == snakeBody.get(i).y) {
                return true;
            }
        }
        return false;
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

    // 與神經網路互動
    public double[] getInputs() {
        double[] inputs = new double[6];
        Point head = snakeBody.get(0);

        // 判斷前方、左方、右方是否有障礙物 (牆壁或身體)
        // 0: 前方, 1: 左方, 2: 右方
        for (int i = 0; i < 3; i++) {
            Point nextPosition = getNextPosition(i);
            inputs[i] = isCollision(nextPosition.x, nextPosition.y) ? 1.0 : 0.0;
        }

        // 判斷食物是否在前方、左方、右方
        // 3: 食物在前方, 4: 食物在左方, 5: 食物在右方
        // 這是基於當前方向的相對位置
        inputs[3] = isFoodInFront() ? 1.0 : 0.0;
        inputs[4] = isFoodToLeft() ? 1.0 : 0.0;
        inputs[5] = isFoodToRight() ? 1.0 : 0.0;

        return inputs;
    }
    // 新增：判斷食物是否在前方
    private boolean isFoodInFront() {
        Point head = snakeBody.get(0);
        // 如果蛇向右，食物在右方代表在前方
        if (direction == 'R' && food.x > head.x) return true;
        // 如果蛇向左，食物在左方代表在前方
        if (direction == 'L' && food.x < head.x) return true;
        // 如果蛇向上，食物在上方代表在前方
        if (direction == 'U' && food.y < head.y) return true;
        // 如果蛇向下，食物在下方代表在前方
        if (direction == 'D' && food.y > head.y) return true;
        return false;
    }

    // 新增：判斷食物是否在左方
    private boolean isFoodToLeft() {
        Point head = snakeBody.get(0);
        // 根據當前方向判斷「左方」的位置
        if (direction == 'R' && food.y < head.y) return true;
        if (direction == 'L' && food.y > head.y) return true;
        if (direction == 'U' && food.x < head.x) return true;
        if (direction == 'D' && food.x > head.x) return true;
        return false;
    }

    // 新增：判斷食物是否在右方
    private boolean isFoodToRight() {
        Point head = snakeBody.get(0);
        // 根據當前方向判斷「右方」的位置
        if (direction == 'R' && food.y > head.y) return true;
        if (direction == 'L' && food.y < head.y) return true;
        if (direction == 'U' && food.x > head.x) return true;
        if (direction == 'D' && food.x < head.x) return true;
        return false;
    }

    // 新增：根據方向索引取得下一個位置
    // 0: 前方, 1: 左方, 2: 右方
    private Point getNextPosition(int relativeDirection) {
        Point head = snakeBody.get(0);
        int nextX = head.x;
        int nextY = head.y;
        char currentDirection = direction;

        if (relativeDirection == 0) { // 前方
            if (currentDirection == 'R') nextX += UNIT_SIZE;
            else if (currentDirection == 'L') nextX -= UNIT_SIZE;
            else if (currentDirection == 'U') nextY -= UNIT_SIZE;
            else if (currentDirection == 'D') nextY += UNIT_SIZE;
        } else if (relativeDirection == 1) { // 左方
            if (currentDirection == 'R') nextY -= UNIT_SIZE;
            else if (currentDirection == 'L') nextY += UNIT_SIZE;
            else if (currentDirection == 'U') nextX -= UNIT_SIZE;
            else if (currentDirection == 'D') nextX += UNIT_SIZE;
        } else if (relativeDirection == 2) { // 右方
            if (currentDirection == 'R') nextY += UNIT_SIZE;
            else if (currentDirection == 'L') nextY -= UNIT_SIZE;
            else if (currentDirection == 'U') nextX += UNIT_SIZE;
            else if (currentDirection == 'D') nextX -= UNIT_SIZE;
        }

        return new Point(nextX, nextY);
    }

    // 神經網路的輸出
    private void setDirectionFromOutput(int output) {
        // 取得當前方向
        char currentDirection = direction;

        // 根據輸出更新方向
        if (output == 0) {
            // 直走，方向不變
        } else if (output == 1) {
            // 向左轉
            switch (currentDirection) {
                case 'U': direction = 'L'; break;
                case 'D': direction = 'R'; break;
                case 'L': direction = 'D'; break;
                case 'R': direction = 'U'; break;
            }
        } else if (output == 2) {
            // 向右轉
            switch (currentDirection) {
                case 'U': direction = 'R'; break;
                case 'D': direction = 'L'; break;
                case 'L': direction = 'U'; break;
                case 'R': direction = 'D'; break;
            }
        }
    }
    // 回傳這條蛇的適應度
    public double getFitness() {
        return score * 1000 + moves; // 這裡可以調整權重
    }

    // 專為訓練設計的模擬方法 (無畫面)
    public double runSimulation() {
        snakeBody = new ArrayList<>();
        direction = 'R';
        score = 0;
        moves = 0;
        int startX = (SCREEN_WIDTH / (2 * UNIT_SIZE)) * UNIT_SIZE;
        int startY = (SCREEN_HEIGHT / (2 * UNIT_SIZE)) * UNIT_SIZE;
        snakeBody.add(new Point(startX, startY));
        for (int i = 0; i < 3; i++) {
            snakeBody.add(new Point(startX - (i + 1) * UNIT_SIZE, startY));
        }
        newFood();
        running = true;

        while(running && moves < MAX_MOVES_WITHOUT_FOOD) {
            actionPerformed(null);
            moves++;
        }
        return getFitness();
    }

    // 更換 AI 腦袋
    public void setBrain(NeuralNetwork newBrain) {
        this.brain = newBrain;
        initGame();
    }

    public void setGameSemaphore(Semaphore semaphore) {
        this.gameSemaphore = semaphore;
    }
}