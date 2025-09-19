import java.awt.Point;
import java.awt.Dimension;
import java.util.List;

public class NeuralNetworkAI implements AI_Decision_Maker {
    private double[] genes;

    public NeuralNetworkAI(double[] genes) {
        this.genes = genes;
    }

    /**
     * 根據訓練好的基因決定下一步移動方向
     */
    @Override
    public char decideDirection(List<Point> snakeBody, Point food, Dimension boardSize) {
        // This is the function you wanted to reuse.
        // It uses the 'this.genes' that were set in the constructor.
        return getBestDirection(this.genes, snakeBody, food, boardSize);
    }
    // Move your private functions here and make them part of this class.
    // It's a good idea to change them to `private` or `protected` as needed.

    // The method you want to reuse
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

        // We will calculate a score for each of the three possible directions:
        // Straight, Left, and Right.
        double scoreStraight = calculateDirectionScore(genes, head, currentDirection, snakeBody, food, boardSize);
        double scoreLeft = calculateDirectionScore(genes, head, turnLeft(currentDirection), snakeBody, food, boardSize);
        double scoreRight = calculateDirectionScore(genes, head, turnRight(currentDirection), snakeBody, food, boardSize);

//        if (isCollision(head, currentDirection, snakeBody, boardSize)) {
//            scoreStraight = Double.NEGATIVE_INFINITY;
//        }
        // CHG: You can add a check here to ensure the AI doesn't crash.
        // This gives a heavy penalty to any move that would result in an immediate crash.
        if (isCollision(head, currentDirection, snakeBody, boardSize)) {
            scoreStraight = -Double.MAX_VALUE;
        }
        if (isCollision(head, turnLeft(currentDirection), snakeBody, boardSize)) {
            scoreLeft = -Double.MAX_VALUE;
        }
        if (isCollision(head, turnRight(currentDirection), snakeBody, boardSize)) {
            scoreRight = -Double.MAX_VALUE;
        }
        // Now, let's select the best direction based on the scores.
        if (scoreStraight >= scoreLeft && scoreStraight >= scoreRight) {
            return currentDirection;
        } else if (scoreLeft >= scoreRight) {
            return turnLeft(currentDirection);
        } else {
            return turnRight(currentDirection);
        }
    }

    // The helper methods should also be moved here
    // A new helper method to calculate the score for a given direction.
    private double calculateDirectionScore(double[] genes, Point head, char direction, List<Point> snakeBody, Point food, Dimension boardSize) {
        // Determine the next position based on the chosen direction.
        Point nextPosition = getNextPosition(head, direction);

        // Create the inputs for our AI. These will be based on the *potential* next position.
        double[] inputs = new double[GameSettings.GENE_SIZE];

//        // Input 0: Is the next position a wall or body? (1.0 for clear, -1.0 for collision)
//        inputs[0] = isCollision(head, direction, snakeBody, boardSize) ? -1.0 : 1.0;
//
//        // Input 1: The angle to the food from the *next* position.
//        // This helps the snake prioritize moves that align it better with the food.
//        inputs[1] = getAngleToFood(nextPosition, food, direction);
//
//        // Input 2: Distance to food from the *next* position (normalized)
//        inputs[2] = getDistanceToFood(nextPosition, food) / (double)(GameSettings.screenWidth + GameSettings.screenHeight);
//
//        // Input 3: Distance to the nearest wall from the *next* position (normalized)
//        inputs[3] = getMinDistanceToWall(nextPosition, boardSize) / (double)GameSettings.screenWidth;
//
//        // Input 4: Distance to the nearest body part from the *next* position (normalized)
//        inputs[4] = getMinDistanceToSelf(nextPosition, snakeBody) / (double)GameSettings.screenWidth;

        inputs[0] = isCollision(head, direction, snakeBody, boardSize) ? -1.0 : 1.0;
        inputs[1] = isCollision(head, turnLeft(direction), snakeBody, boardSize) ? -1.0 : 1.0;
        inputs[2] = isCollision(head, turnRight(direction), snakeBody, boardSize) ? -1.0 : 1.0;

        double foodAngle = getAngleToFood(head, food, direction);
        inputs[3] = (Math.abs(foodAngle) < 45) ? 1.0 : 0.0;
        inputs[4] = (foodAngle < -45) ? 1.0 : 0.0;
        inputs[5] = (foodAngle > 45) ? 1.0 : 0.0;

        // 新增的輸入，讓 AI 能做出更全面的判斷
        inputs[6] = getDistanceToFood(head, food) / (double)(GameSettings.screenWidth + GameSettings.screenHeight);
        inputs[7] = getMinDistanceToWall(head, boardSize) / (double)GameSettings.screenWidth;
        inputs[8] = getMinDistanceToSelf(head, snakeBody) / (double)GameSettings.screenWidth;

        // Additional inputs can be added here, like 'free space' in front, left, or right.
        // (This would require a new 'flood fill' or 'raycasting' method)

        // Now, calculate the score using the genes.
        // The total score is the sum of (gene * input) for all inputs.
        double score = 0.0;
        for (int i = 0; i < GameSettings.GENE_SIZE; i++) {
            // We'll need to change the GENE_SIZE and gene meaning.
            // Let's assume you increase GENE_SIZE to at least 5 to match the new inputs.
            // For simplicity, let's match the number of genes to the number of inputs.
            // So, let's assume GENE_SIZE = 5.
            if (i < GameSettings.GENE_SIZE) {
                score += genes[i] * inputs[i];
            }
        }
        return score;
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

        if (angle > 180) angle -= 90;
        if (angle < -180) angle += 90;
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

    private Point getNextPosition(Point head, char direction) {
        int nextX = head.x;
        int nextY = head.y;
        int UNIT_SIZE = 25;

        switch (direction) {
            case 'U': nextY -= UNIT_SIZE; break;
            case 'D': nextY += UNIT_SIZE; break;
            case 'L': nextX -= UNIT_SIZE; break;
            case 'R': nextX += UNIT_SIZE; break;
        }
        return new Point(nextX, nextY);
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

        // Create a new Point object for the predicted next position.
        Point nextHead = new Point(nextX, nextY);

        // 檢查撞牆
//        if (nextX < 0 || nextX >= boardSize.width || nextY < 0 || nextY >= boardSize.height) {
//            return true;
//        }
        // Check for wall collision using the new point.
        if (nextHead.x < 0 || nextHead.x >= boardSize.width || nextHead.y < 0 || nextHead.y >= boardSize.height) {
            return true;
        }

        // 檢查撞到自己
//        for (int i = 1; i < snakeBody.size(); i++) {
//            if (nextX == snakeBody.get(i).x && nextY == snakeBody.get(i).y) {
//                return true;
//            }
//        }
        // Check for self-collision using the new point.
        // We iterate through the ENTIRE body list, as the head will move into
        // one of these spots. The snake's tail (last segment) will be moved.
        // The previous head position is now part of the body.
        // By checking against all body segments, including the previous head position,
        // we accurately detect if the new head position is already occupied.
        for (Point bodyPart : snakeBody) {
            if (nextHead.equals(bodyPart)) {
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

}