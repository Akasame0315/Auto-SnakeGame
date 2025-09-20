import java.awt.Point;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

public class NeuralNetworkAI implements AI_Decision_Maker {
    private final NeuralNetwork neuralNetwork;

    // 神經網路結構參數
    private static final int NUM_INPUTS = 22;
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
        char currentDirection = getCurrentDirection(snakeBody);

        // 添加安全檢查 - 避免立即撞死
        char[] possibleMoves = new char[3];
        possibleMoves[0] = currentDirection;           // Forward
        possibleMoves[1] = turnLeft(currentDirection);  // Left
        possibleMoves[2] = turnRight(currentDirection); // Right

        // 檢查每個方向的安全性
        boolean[] isSafe = new boolean[3];
        Point head = snakeBody.get(0);

        for (int i = 0; i < 3; i++) {
            Point nextPos = getNextPosition(head, possibleMoves[i]);
            isSafe[i] = isPositionSafe(nextPos, snakeBody, boardSize);
        }
        // 找出最高分且安全的移動
        double maxSafeOutput = Double.NEGATIVE_INFINITY;
        int bestSafeIndex = -1;

        for (int i = 0; i < 3; i++) {
            if (isSafe[i] && outputs[i] > maxSafeOutput) {
                maxSafeOutput = outputs[i];
                bestSafeIndex = i;
            }
        }

        // 如果有安全的移動，選擇最佳的
        if (bestSafeIndex != -1) {
            return possibleMoves[bestSafeIndex];
        }

        double maxOutput = outputs[0];
        int bestOutputIndex = 0;
        for (int i = 1; i < 3; i++) {
            if (outputs[i] > maxOutput) {
                maxOutput = outputs[i];
                bestOutputIndex = i;
            }
        }
        return possibleMoves[bestOutputIndex];
    }

    private double[] createInputVector(List<Point> snakeBody, Point food, Dimension boardSize) {
        double[] inputs = new double[22]; //牆壁跟身體各八個(方向)，食物3個(定位x、y軸&距離) + 蛇的身體 + 頭的定位 (2 x 8) + 3 + 1 + 2
        Point head = snakeBody.get(0);

        int[] dx = {0, 0, -1, 1, -1, 1, -1, 1};
        int[] dy = {-1, 1, 0, 0, -1, -1, 1, 1};

        // 前16個輸入：8方向的牆壁和身體距離
        for (int i = 0; i < 8; i++) {
            double distanceToWall = 1.0;
            double distanceToBody = 1.0;

            // 保持 20 格的檢測範圍，偵測立即危險
            for (int d = 1; d <= 20; d++) {
                int nextX = head.x + dx[i] * GameSettings.UNIT_SIZE * d; // 下一個點為八個方向的20格外
                int nextY = head.y + dy[i] * GameSettings.UNIT_SIZE * d;
                Point nextPoint = new Point(nextX, nextY);

                // 檢查牆壁
                if (nextX < 0 || nextX >= boardSize.width || nextY < 0 || nextY >= boardSize.height) {
                    distanceToWall = (double) d / 20.0; //距離越近，值越小。
                    break;
                }

                // 檢查身體碰撞
                for (Point body : snakeBody) {
                    if (body.equals(nextPoint)) {
                        distanceToBody = (double) d / 20.0;
                        break;
                    }
                }
                if (distanceToBody > 0) break; // 找到最近的就停
            }
            inputs[i * 2] = distanceToWall;
            inputs[i * 2 + 1] = distanceToBody;
        }

        // 全局食物位置資訊
        double foodX = (food.x - head.x) / (double) boardSize.width;
        double foodY = (food.y - head.y) / (double) boardSize.height;
        // 將全局資訊加到輸入向量的最後
        inputs[16] = foodX;
        inputs[17] = foodY;
        // 食物距離
        double foodDistance = Math.sqrt(Math.pow(food.x - head.x, 2) + Math.pow(food.y - head.y, 2));
        double maxDistance = Math.sqrt(Math.pow(boardSize.width, 2) + Math.pow(boardSize.height, 2));
        inputs[18] = 1.0 - (foodDistance / maxDistance); // 正規化食物距離

        // 蛇的長度（正規化）
        inputs[19] = Math.min(1.0, snakeBody.size() / 1400.0); // 假設最大長度為20
        // 頭部位置（正規化）
        inputs[20] = (double) head.x / boardSize.width;
        inputs[21] = (double) head.y / boardSize.height;

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

    // ============== 輔助方法 ==============
    private Point getNextPosition(Point current, char direction) {
        Point next = new Point(current);
        switch (direction) {
            case 'U': next.y -= GameSettings.UNIT_SIZE; break;
            case 'D': next.y += GameSettings.UNIT_SIZE; break;
            case 'L': next.x -= GameSettings.UNIT_SIZE; break;
            case 'R': next.x += GameSettings.UNIT_SIZE; break;
        }
        return next;
    }

    private boolean isPositionSafe(Point pos, List<Point> snakeBody, Dimension boardSize) {
        // 檢查邊界
        if (pos.x < 0 || pos.x >= boardSize.width || pos.y < 0 || pos.y >= boardSize.height) {
            return false;
        }

        // 檢查身體碰撞（排除尾巴，因為下一步尾巴會移動）
        for (int i = 0; i < snakeBody.size() - 1; i++) {
            if (snakeBody.get(i).equals(pos)) {
                return false;
            }
        }

        return true;
    }
}