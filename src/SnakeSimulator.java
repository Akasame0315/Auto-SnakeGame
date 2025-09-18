import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeSimulator {

    // 遊戲常數，與 GamePanel 保持一致
    private final int UNIT_SIZE = 25;
    private final int SCREEN_WIDTH;
    private final int SCREEN_HEIGHT;
    private final int GAME_UNITS;

    // 蛇的身體座標
    private List<Point> snakeBody;
    // 食物的座標
    private Point food;
    // 隨機數生成器
    private Random random;
    // 遊戲狀態
    private boolean running;
    // 蛇的移動方向
    private char direction;
    // 遊戲步數
    private int steps;
    // 最大步數限制，避免無解時無限循環
    private final int MAX_STEPS = 500;

    public SnakeSimulator(int width, int height) {
        this.SCREEN_WIDTH = width;
        this.SCREEN_HEIGHT = height;
        this.GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / (UNIT_SIZE * UNIT_SIZE);
        this.random = new Random();
    }

    // 執行一場完整的遊戲，並回傳得分與步數
    public SimulationResult run(AI_Decision_Maker ai) {
        initGame();

        while (running && steps < MAX_STEPS) {
            steps++;
            direction = ai.decideDirection(snakeBody, food, new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
            move();
            checkFood();
            checkCollisions();
        }

        return new SimulationResult(getScore(), steps);
    }

    // 初始化遊戲
    private void initGame() {
        snakeBody = new ArrayList<>();
        // 初始蛇長度為 3
        snakeBody.add(new Point(0, 0));
        snakeBody.add(new Point(0, 0));
        snakeBody.add(new Point(0, 0));

        direction = 'R'; // 預設向右
        running = true;
        steps = 0;

        // 放置初始食物
        placeFood();
    }

    // 移動邏輯
    private void move() {
        Point head = new Point(snakeBody.get(0));

        switch (direction) {
            case 'U':
                head.y -= UNIT_SIZE;
                break;
            case 'D':
                head.y += UNIT_SIZE;
                break;
            case 'L':
                head.x -= UNIT_SIZE;
                break;
            case 'R':
                head.x += UNIT_SIZE;
                break;
        }

        snakeBody.add(0, head);
        // 如果沒有吃到食物，移除尾巴
        if (!head.equals(food)) {
            snakeBody.remove(snakeBody.size() - 1);
        }
    }

    // 檢查是否吃到食物
    private void checkFood() {
        if (snakeBody.get(0).equals(food)) {
            placeFood();
        }
    }

    // 檢查碰撞
    private void checkCollisions() {
        Point head = snakeBody.get(0);

        // 檢查是否撞牆
        if (head.x < 0 || head.x >= SCREEN_WIDTH || head.y < 0 || head.y >= SCREEN_HEIGHT) {
            running = false;
        }

        // 檢查是否撞到自己
        for (int i = 1; i < snakeBody.size(); i++) {
            if (head.equals(snakeBody.get(i))) {
                running = false;
            }
        }
    }

    // 放置食物
    private void placeFood() {
        int x = random.nextInt((int)(SCREEN_WIDTH / UNIT_SIZE)) * UNIT_SIZE;
        int y = random.nextInt((int)(SCREEN_HEIGHT / UNIT_SIZE)) * UNIT_SIZE;
        food = new Point(x, y);
    }

    // 獲取得分
    private int getScore() {
        return snakeBody.size() - 3;
    }

    // 模擬結果類別
    public static class SimulationResult {
        public int score;
        public int steps;

        public SimulationResult(int score, int steps) {
            this.score = score;
            this.steps = steps;
        }
    }
}