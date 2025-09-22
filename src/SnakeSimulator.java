import java.awt.*;
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
    private int MAX_STEPS = 300;
    private int TOTAL_STEPS;

    public SnakeSimulator(int width, int height) {
        this.SCREEN_WIDTH = width;
        this.SCREEN_HEIGHT = height;
        this.GAME_UNITS = (SCREEN_WIDTH * SCREEN_HEIGHT) / (UNIT_SIZE * UNIT_SIZE);
        this.random = new Random();
        this.MAX_STEPS =  SCREEN_WIDTH + SCREEN_HEIGHT;
    }

    // 執行一場完整的遊戲，並回傳得分與步數
    public SimulationResult run(AI_Decision_Maker ai) {
        initGame();
        double totalProximityReward = 0; // <<< 初始化過程獎勵

        while (running && steps < MAX_STEPS) {
            // 1. 移動前的距離
            // 使用曼哈頓距離，計算成本較低且適合網格遊戲
            int distBefore = Math.abs(snakeBody.get(0).x - food.x) + Math.abs(snakeBody.get(0).y - food.y);

            direction = ai.decideDirection(snakeBody, food, new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
            move();

            // 2. 移動後的距離
            int distAfter = Math.abs(snakeBody.get(0).x - food.x) + Math.abs(snakeBody.get(0).y - food.y);
            // 3. 根據距離變化給予獎懲
            if (distAfter < distBefore) {
                totalProximityReward += 1.0; // 靠近食物，給予獎勵
            } else {
                // 沒有靠近 (可能平行移動或遠離)，給予懲罰
                // 懲罰稍微大於獎勵，可以避免蛇原地“抖動”來刷分
                totalProximityReward -= 1.5;
            }

            // 檢查是否吃到食物，這部分邏輯很重要
            boolean foodEaten = (snakeBody.get(0).equals(food)); // 假設 checkFood() 會回傳是否吃到
            // 如果這一步吃到了食物，通常食物會刷新到新位置
            // 此時 distAfter 會突然變很大，導致一次不公平的巨大懲罰
            // 因此，如果吃到食物，可以抵銷這一步的懲罰，甚至給予額外獎勵
            if (foodEaten) {
                // 選擇一種策略：
                // 策略 A: 給予一個大獎勵，覆蓋剛才的計算
                totalProximityReward += 5.0; // 例如額外給 5 分
                // 策略 B: 移除剛剛發生的懲罰（如果有的話）
                // (這個比較複雜，因為你不知道上一步是獎是罰，策略A更簡單直接)
            }

            checkFood();
            checkCollisions();
        }

        return new SimulationResult(getScore(), TOTAL_STEPS, totalProximityReward);
    }

    // 初始化遊戲
    private void initGame() {
        snakeBody = new ArrayList<>();
        int intiX = 0;
        int intiY = 0;
        // 初始蛇長度為 3
        snakeBody.add(new Point(intiX, intiY));
        snakeBody.add(new Point(intiX - UNIT_SIZE, intiY));
        snakeBody.add(new Point(intiX - UNIT_SIZE * 2, intiY));

        direction = 'R'; // 預設向右
        running = true;
        steps = 0;
        TOTAL_STEPS = 0;

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
        TOTAL_STEPS++;
        steps++;
    }

    // 檢查是否吃到食物
    private void checkFood() {
        if (snakeBody.get(0).equals(food)) {
            placeFood();
            steps = 0;
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
        public final double proximityReward; //鄰近獎勵

        public SimulationResult(int score, int steps, double proximityReward) {
            this.score = score;
            this.steps = steps;
            this.proximityReward = proximityReward;
        }
    }
}