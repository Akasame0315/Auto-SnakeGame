// AI 決策介面
public interface AI_Decision_Maker {

    /**
     * 根據當前遊戲狀態，決定蛇的下一步移動方向
     * * @param snakeBody 蛇的身體座標列表
     * @param food      食物的座標
     * @param boardSize 遊戲板的寬高
     * @return 決定的移動方向 (例如 'U', 'D', 'L', 'R')
     */
    char decideDirection(java.util.List<java.awt.Point> snakeBody, java.awt.Point food, java.awt.Dimension boardSize);
}