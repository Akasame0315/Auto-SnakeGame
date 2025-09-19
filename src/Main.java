import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameSettings.loadSettings();
            new Main().createAndShowGUI();
        });
    }

    private JFrame frame;
    private GamePanel gamePanel;
    private Timer gameTimer;

    public void createAndShowGUI() {
        frame = new JFrame("Auto SnakeGame");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); // 全螢幕顯示

        AI_Decision_Maker ai;
        File geneFile = new File("best_genes.txt");
        if (geneFile.exists() && geneFile.length() > 0) {
            System.out.println("Loading trained AI model from " + geneFile.getName());
            ai = new NeuralNetworkAI("best_genes.txt");
        } else {
            System.out.println("No trained AI found, using Traditional AI.");
            ai = new TraditionalAI();
        }

        // 創建 GamePanel，並傳入重啟遊戲的邏輯
        gamePanel = new GamePanel(GameSettings.screenWidth, GameSettings.screenHeight, ai, v -> restartGame());

        frame.add(gamePanel);
        frame.setVisible(true);
        gamePanel.requestFocusInWindow(); // 確保 GamePanel 取得焦點以監聽鍵盤

        // 遊戲主迴圈計時器
        gameTimer = new Timer(GameSettings.gameSpeed, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 只有在遊戲運行中且未暫停時才更新
                if (gamePanel.isRunning() && !gamePanel.isPaused()) {
                    gamePanel.updateGame();
                }
            }
        });
        gameTimer.start();
    }

    // 重啟遊戲的方法
    public void restartGame() {
        if (gameTimer != null) {
            gameTimer.stop();
        }
        if (frame != null) {
            frame.dispose(); // 關閉舊視窗
        }
        // 重新載入設定並創建新遊戲
        SwingUtilities.invokeLater(() -> {
            GameSettings.loadSettings();
            createAndShowGUI();
        });
    }
}