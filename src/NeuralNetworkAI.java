import java.awt.Point;
import java.awt.Dimension;
import java.util.List;
import java.util.ArrayList;

public class NeuralNetworkAI implements AI_Decision_Maker {
    private final NeuralNetwork neuralNetwork;

    // 神經網路結構參數
    private static final int NUM_INPUTS = 24;
    private static final int NUM_HIDDEN = 16;
    private static final int NUM_OUTPUTS = 3;

    public NeuralNetworkAI(double[] genes) {
        this.neuralNetwork = new NeuralNetwork(NUM_INPUTS, NUM_HIDDEN, NUM_OUTPUTS);
        this.neuralNetwork.setGenome(genes);
    }

    public NeuralNetworkAI(String filePath) {
        this.neuralNetwork = NeuralNetwork.loadFromFile(filePath, NUM_INPUTS, NUM_HIDDEN, NUM_OUTPUTS);
        if (this.neuralNetwork == null) {
            System.err.println("Failed to load neural network from file: " + filePath);
        }
    }

    /**
     * 這是一個簡單的範例，實際中你需要根據你的基因設計來寫決策邏輯
     * 統一訓練和實際遊戲的邏輯
     * 根據訓練好的基因決定下一步移動方向
     * 1. 初始蛇和食物位置
     * 2. 根據基因來決定移動方向的邏輯
     * 3. 移動、檢查碰撞、吃食物的邏輯
     * 4. 記錄得分和步數
     */
    @Override
    public char decideDirection(List<Point> snakeBody, Point food, Dimension boardSize) {
        if (neuralNetwork == null) {
            return 'R'; // fallback to a default direction
        }
        double[] inputs = createInputVector(snakeBody, food, boardSize);
        double[] outputs = neuralNetwork.predict(inputs);

        int bestOutputIndex = 0;
        if (outputs[1] > outputs[0]) {
            bestOutputIndex = 1;
        }
        if (outputs[2] > outputs[bestOutputIndex]) {
            bestOutputIndex = 2;
        }

        char currentDirection = getCurrentDirection(snakeBody);
        switch (bestOutputIndex) {
            case 0: // Forward
                return currentDirection;
            case 1: // Left
                return turnLeft(currentDirection);
            case 2: // Right
                return turnRight(currentDirection);
        }
        return currentDirection;
    }

    private double[] createInputVector(List<Point> snakeBody, Point food, Dimension boardSize) {
        double[] inputs = new double[24];
        Point head = snakeBody.get(0);

        int[] dx = {0, 0, -1, 1, -1, 1, -1, 1};
        int[] dy = {-1, 1, 0, 0, -1, -1, 1, 1};

        for (int i = 0; i < 8; i++) {
            double distanceToWall = 0;
            double distanceToFood = 0;
            double distanceToBody = 0;

            for (int d = 1; d <= 20; d++) {
                int nextX = head.x + dx[i] * GameSettings.UNIT_SIZE * d;
                int nextY = head.y + dy[i] * GameSettings.UNIT_SIZE * d;
                Point nextPoint = new Point(nextX, nextY);

                if (nextX < 0 || nextX >= boardSize.width || nextY < 0 || nextY >= boardSize.height) {
                    distanceToWall = (double) d / 20.0;
                    break;
                }

                if (nextPoint.equals(food)) {
                    // 使用曼哈頓距離（ Manhattan distance ）來計算從蛇頭到食物的距離
                    // 距離越近，值越小，可以根據需求正規化
                    // 這裡我們將距離正規化到 0 到 1 之間
                    int dist = Math.abs(nextX - head.x) + Math.abs(nextY - head.y);
                    distanceToFood = 1.0 / (dist + 1); // +1 避免除以零
//                    distanceToFood = 1.0;
                }

                if (snakeBody.contains(nextPoint)) {
                    distanceToBody = (double) d / 20.0;
                }
            }
            inputs[i * 3] = distanceToWall;
            inputs[i * 3 + 1] = distanceToFood;
            inputs[i * 3 + 2] = distanceToBody;
        }

        return inputs;
    }

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