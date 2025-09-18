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

public class GamePanel extends JPanel implements ActionListener  { // 實現 ActionListener 介面

    // 遊戲常數
    final int UNIT_SIZE = 25; // 每個方塊的大小
    final int SCREEN_WIDTH;
    final int SCREEN_HEIGHT;
    final int GAME_UNITS;
    boolean paused = false; // 新增變數來追蹤遊戲是否暫停

    // 蛇的身體座標
    ArrayList<Point> snakeBody;
    // 食物的座標
    Point food;
    // 隨機數生成器
    Random random;
    // 遊戲迴圈
    Timer timer;
    // 遊戲狀態
    boolean running = false;
    // 蛇的移動方向
    char direction = 'R'; // 預設向右 (Right)

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
        for (int i = 0; i < GameSettings.screenWidth / UNIT_SIZE; i++) {
            g.drawLine(offsetX + i * UNIT_SIZE, offsetY, offsetX + i * UNIT_SIZE, offsetY + GameSettings.screenHeight);
        }
        for (int i = 0; i < GameSettings.screenHeight / UNIT_SIZE; i++) {
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
            // 自動判斷食物方向並轉向
            makeDecision();
            // 呼叫移動方法
            move();
            // 呼叫碰撞偵測方法
            checkCollisions();
            // 呼叫吃食物方法
            checkFood();
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
        Timer restartTimer = new Timer(5000, new ActionListener() {
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

    // 自動化移動(朝向食物)
    public void makeDecision() {
        // 獲取蛇頭和食物的座標
        Point head = snakeBody.get(0);
        int headX = head.x;
        int headY = head.y;
        int foodX = food.x;
        int foodY = food.y;

        // 優先朝著食物方向移動
        if (headX < foodX && direction != 'L') {
            if (!isCollision(headX + UNIT_SIZE, headY)) {
                direction = 'R';
                return;
            }
        }
        if (headX > foodX && direction != 'R') {
            if (!isCollision(headX - UNIT_SIZE, headY)) {
                direction = 'L';
                return;
            }
        }
        if (headY < foodY && direction != 'U') {
            if (!isCollision(headX, headY + UNIT_SIZE)) {
                direction = 'D';
                return;
            }
        }
        if (headY > foodY && direction != 'D') {
            if (!isCollision(headX, headY - UNIT_SIZE)) {
                direction = 'U';
                return;
            }
        }

        // 如果所有通往食物的路都被擋住了，隨機選擇一個不會碰撞的方向
        if (!isCollision(headX + UNIT_SIZE, headY) && direction != 'L') {
            direction = 'R';
        } else if (!isCollision(headX - UNIT_SIZE, headY) && direction != 'R') {
            direction = 'L';
        } else if (!isCollision(headX, headY + UNIT_SIZE) && direction != 'U') {
            direction = 'D';
        } else if (!isCollision(headX, headY - UNIT_SIZE) && direction != 'D') {
            direction = 'U';
        }
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
}