import java.awt.*;
import java.util.List;
import java.util.Random;

// 初始遊戲演算判斷
public class TraditionalAI implements AI_Decision_Maker {

    private static final int UNIT_SIZE = 25;
    // 隨機數生成器
    private static final Random random = new Random();

    @Override
    public char decideDirection(List<Point> snakeBody, Point food, Dimension boardSize) {
        // 獲取蛇頭和食物的座標
        Point head = snakeBody.get(0);
        int headX = head.x;
        int headY = head.y;
        int foodX = food.x;
        int foodY = food.y;

        // 檢查當前方向，並嘗試朝向食物移動
        char currentDirection = getCurrentDirection(snakeBody);

        // 優先朝著食物方向移動
        if (headX < foodX && currentDirection != 'L') {
            if (!isCollision(headX + UNIT_SIZE, headY, snakeBody, boardSize)) {
                return 'R';
            }
        }
        if (headX > foodX && currentDirection != 'R') {
            if (!isCollision(headX - UNIT_SIZE, headY, snakeBody, boardSize)) {
                return 'L';
            }
        }
        if (headY < foodY && currentDirection != 'U') {
            if (!isCollision(headX, headY + UNIT_SIZE, snakeBody, boardSize)) {
                return 'D';
            }
        }
        if (headY > foodY && currentDirection != 'D') {
            if (!isCollision(headX, headY - UNIT_SIZE, snakeBody, boardSize)) {
                return 'U';
            }
        }

        // 如果所有通往食物的路都被擋住了，隨機選擇一個不會碰撞的方向
        if (!isCollision(headX + UNIT_SIZE, headY, snakeBody, boardSize) && currentDirection != 'L') {
            return 'R';
        } else if (!isCollision(headX - UNIT_SIZE, headY, snakeBody, boardSize) && currentDirection != 'R') {
            return 'L';
        } else if (!isCollision(headX, headY + UNIT_SIZE, snakeBody, boardSize) && currentDirection != 'U') {
            return 'D';
        } else if (!isCollision(headX, headY - UNIT_SIZE, snakeBody, boardSize) && currentDirection != 'D') {
            return 'U';
        }

        // 如果真的沒路可走，隨便選一個方向 (這部分在你的原始程式碼中沒有，但這是必要的)
        return new char[]{'U', 'D', 'L', 'R'}[random.nextInt(4)];
        // return currentDirection;
    }

    // 從蛇身判斷當前移動方向
    private char getCurrentDirection(List<Point> snakeBody) {
        Point head = snakeBody.get(0);
        Point neck = snakeBody.get(1);
        if (head.x > neck.x) return 'R';
        if (head.x < neck.x) return 'L';
        if (head.y > neck.y) return 'D';
        if (head.y < neck.y) return 'U';
        return 'R'; // 預設
    }

    // 實作一個檢查碰撞的方法
    private boolean isCollision(int nextX, int nextY, List<Point> snakeBody, Dimension boardSize) {
        // 檢查是否撞牆
        if (nextX < 0 || nextX >= boardSize.width || nextY < 0 || nextY >= boardSize.height) {
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
}