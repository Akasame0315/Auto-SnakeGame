import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

// 繼承 AI_Decision_Maker 介面
public class GeneticAlgorithmAI implements AI_Decision_Maker {

    // 演算法參數
    private static final int POPULATION_SIZE = 100;  // 族群大小
    private static final int GENE_SIZE = 9;         // 基因長度（與食物、牆壁、身體的距離權重，每個因素各有三個方向）
    private static final double MUTATION_RATE = 0.1; // 突變機率
    private static final int MAX_GENERATIONS = 1000; // 最大世代數

    // 遊戲環境參數（與 GamePanel 相同）
    private static final int UNIT_SIZE = 25;

    // 遊戲環境參數
//    private static final int GAME_WIDTH = 800; // 你可以根據需求調整
//    private static final int GAME_HEIGHT = 600; // 你可以根據需求調整
    private static final int GAME_WIDTH = GameSettings.screenWidth;
    private static final int GAME_HEIGHT = GameSettings.screenHeight;

    // 隨機數生成器
    private static final Random random = new Random();
    // 訓練好的最佳基因
    private double[] bestGenes;
    private TrainingCompletionListener listener;

    public GeneticAlgorithmAI(TrainingCompletionListener listener) {
        this.listener = listener;
        // 注意：建構子中不再進行訓練，它只負責初始化
    }
    // 定義一個回呼介面，用於在訓練完成時通知遊戲
    public interface TrainingCompletionListener {
        void onTrainingComplete(double[] bestGenes);
    }

    // 個體類別，代表一條蛇和它的基因
    private static class Individual {
        double[] genes = new double[GENE_SIZE]; // 基因組：權重
        int fitness; // 適應度：遊戲得分
    }

    // 訓練紀錄
    private static class TrainingRecord {
        int generation;
        double fitness;
        double[] genes;

        public TrainingRecord(int generation, double fitness, double[] genes) {
            this.generation = generation;
            this.fitness = fitness;
            this.genes = genes;
        }
    }

    /**
     * 遺傳演算法訓練流程
     */
    public void startTraining() { // 使用 CompletableFuture 在背景執行緒中執行訓練
        CompletableFuture.runAsync(() -> {
            Individual[] population = new Individual[POPULATION_SIZE];
            for (int i = 0; i < POPULATION_SIZE; i++) {
                population[i] = new Individual();
                for (int j = 0; j < GENE_SIZE; j++) {
                    population[i].genes[j] = random.nextDouble() * 2 - 1;
                }
            }

            for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
                for (Individual individual : population) {
                    individual.fitness = simulateGame(individual.genes);
                }
                Arrays.sort(population, Comparator.comparingInt(i -> i.fitness));

                // 記錄當前世代的最佳適應度和基因
                Individual bestIndividual = population[POPULATION_SIZE - 1];
//                trainingRecords.add(new TrainingRecord(generation, bestIndividual.fitness, bestIndividual.genes));

                // 交叉與突變...
                Individual[] nextPopulation = new Individual[POPULATION_SIZE];
                nextPopulation[0] = population[POPULATION_SIZE - 1];
                for (int i = 1; i < POPULATION_SIZE; i++) {
                    Individual parent1 = population[random.nextInt(POPULATION_SIZE / 2) + POPULATION_SIZE / 2];
                    Individual parent2 = population[random.nextInt(POPULATION_SIZE / 2) + POPULATION_SIZE / 2];

                    Individual child = new Individual();
                    int crossoverPoint = random.nextInt(GENE_SIZE);
                    for (int j = 0; j < GENE_SIZE; j++) {
                        child.genes[j] = (j < crossoverPoint) ? parent1.genes[j] : parent2.genes[j];
                    }
                    if (random.nextDouble() < MUTATION_RATE) {
                        child.genes[random.nextInt(GENE_SIZE)] = random.nextDouble() * 2 - 1;
                    }
                    nextPopulation[i] = child;
                }
                population = nextPopulation;
                System.out.println("Generation " + generation + "：Best Fitness = " + population[0].fitness);
            }

            this.bestGenes = population[0].genes;
            System.out.println("AI Training Finished. Best Genes: " + Arrays.toString(bestGenes));

//            exportTrainingHistory();

            // 訓練完成後，通知遊戲
            if (listener != null) {
                listener.onTrainingComplete(bestGenes);
            }
        });
    }

    /**
     *注意：這裡需要一個完整的遊戲模擬器**
     * 模擬遊戲來評估基因的適應度
     * 這是你論文的重點：你需要設計一個能夠讓蛇根據基因移動的演算法
     * @param genes 基因組
     * @return 遊戲得分（適應度）
     */
    private int simulateGame(double[] genes) {

        SnakeSimulator simulator = new SnakeSimulator(GameSettings.screenWidth, GameSettings.screenHeight);

        AI_Decision_Maker ai = (snakeBody, food, boardSize) -> getBestDirection(genes, snakeBody, food, boardSize);

        SnakeSimulator.SimulationResult result = simulator.run(ai);
        // 適應度計算公式：得分*1000 + 步數
        return result.score * 1000 + result.steps;
    }
    /**
     * 根據訓練好的基因決定下一步移動方向
     */
    @Override
    public char decideDirection(List<Point> snakeBody, Point food, Dimension boardSize) {
        // 你需要在這裡實現根據基因決策的邏輯
        // 這是你論文的重點之一，需要仔細設計
        if (bestGenes == null) {
            // 如果還沒訓練好，使用隨機方向
            return new char[]{'U', 'D', 'L', 'R'}[random.nextInt(4)];
        }

        return getBestDirection(bestGenes, snakeBody, food, boardSize);
    }

    /**
     * 這是一個簡單的範例，實際中你需要根據你的基因設計來寫決策邏輯
     * 統一訓練和實際遊戲的邏輯
     * 1. 初始蛇和食物位置
     * 2. 根據基因來決定移動方向的邏輯
     * 3. 移動、檢查碰撞、吃食物的邏輯
     * 4. 記錄得分和步數
     */
    private char getBestDirection(double[] genes, List<Point> snakeBody, Point food, Dimension boardSize) {
        char currentDirection = getCurrentDirection(snakeBody);
        Point head = snakeBody.get(0);

        double[] inputs = new double[GENE_SIZE];

        inputs[0] = isCollision(head, currentDirection, snakeBody, boardSize) ? -1.0 : 1.0;
        inputs[1] = isCollision(head, turnLeft(currentDirection), snakeBody, boardSize) ? -1.0 : 1.0;
        inputs[2] = isCollision(head, turnRight(currentDirection), snakeBody, boardSize) ? -1.0 : 1.0;

        double foodAngle = getAngleToFood(head, food, currentDirection);
        inputs[3] = (Math.abs(foodAngle) < 45) ? 1.0 : 0.0;
        inputs[4] = (foodAngle < -45) ? 1.0 : 0.0;
        inputs[5] = (foodAngle > 45) ? 1.0 : 0.0;

        // 新增的輸入，讓 AI 能做出更全面的判斷
        inputs[6] = getDistanceToFood(head, food) / (double)(GameSettings.screenWidth + GameSettings.screenHeight);
        inputs[7] = getMinDistanceToWall(head, boardSize) / (double)GameSettings.screenWidth;
        inputs[8] = getMinDistanceToSelf(head, snakeBody) / (double)GameSettings.screenWidth;

        double score_straight = genes[0] * inputs[0] + genes[1] * inputs[3];
        double score_left = genes[2] * inputs[1] + genes[3] * inputs[4];
        double score_right = genes[4] * inputs[2] + genes[5] * inputs[5];

        if (score_straight >= score_left && score_straight >= score_right) {
            return currentDirection;
        } else if (score_left >= score_straight && score_left >= score_right) {
            return turnLeft(currentDirection);
        } else {
            return turnRight(currentDirection);
        }
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
    // 判斷前進方向
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
    // 計算食物相對於蛇頭的方位角 (以度為單位)
    private double getAngleToFood(Point head, Point food, char direction) {
        int dx = food.x - head.x;
        int dy = food.y - head.y;
        double angle = Math.toDegrees(Math.atan2(dy, dx));

        switch (direction) {
            case 'U': angle -= 90; break;
            case 'D': angle += 90; break;
            case 'L': angle += 180; break;
        }

        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }

    // 計算蛇頭與食物的直線距離
    private double getDistanceToFood(Point head, Point food) {
        return Math.sqrt(Math.pow(food.x - head.x, 2) + Math.pow(food.y - head.y, 2));
    }
    // 計算蛇頭與最近牆壁的距離
    private double getMinDistanceToWall(Point head, Dimension boardSize) {
        double distLeft = head.x;
        double distRight = boardSize.width - head.x;
        double distUp = head.y;
        double distDown = boardSize.height - head.y;
        return Math.min(Math.min(distLeft, distRight), Math.min(distUp, distDown));
    }

    // 計算蛇頭與最近身體節點的距離
    private double getMinDistanceToSelf(Point head, List<Point> snakeBody) {
        if (snakeBody.size() < 2) return Double.MAX_VALUE;
        double minDistance = Double.MAX_VALUE;
        for (int i = 1; i < snakeBody.size(); i++) {
            double distance = Math.sqrt(Math.pow(snakeBody.get(i).x - head.x, 2) + Math.pow(snakeBody.get(i).y - head.y, 2));
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }
    // 將訓練歷史匯出至 JSON 檔案
//    private void exportTrainingHistory() {
//        Gson gson = new Gson();
//        try (FileWriter writer = new FileWriter(HISTORY_FILE)) {
//            gson.toJson(trainingRecords, writer);
//            System.out.println("訓練記錄已匯出至 " + HISTORY_FILE);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}