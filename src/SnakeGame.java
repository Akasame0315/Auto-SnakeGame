import javax.swing.*;
import java.awt.*;
import javax.swing.JOptionPane; // 需要引入

public class SnakeGame extends JFrame {
    private JFrame frame;
    private GamePanel gamePanel;

    public SnakeGame() {
        frame = new JFrame();
        // 設定視窗標題
        frame.setTitle("Auto SnakeGame(With AI)");
        // 設定關閉視窗時的行為
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 移除視窗標題和邊框，讓它變為全螢幕
        this.setUndecorated(true);
        // 設定為最大化，填滿整個螢幕
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setResizable(false);
//        frame.setPreferredSize(new Dimension(GameSettings.screenWidth, GameSettings.screenHeight));
        // 讓視窗顯示在螢幕中央
        frame.setLocationRelativeTo(null);
        frame.pack();
        // 顯示視窗
        frame.setVisible(true);
    }

    // Add a method to set the game panel with the trained AI
    public void setGamePanel(NeuralNetwork trainedBrain) {
        // Remove the old panel if it exists
        if (gamePanel != null) {
            frame.remove(gamePanel);
        }

        // Create a new GamePanel with the trained brain
        gamePanel = new GamePanel(trainedBrain);
        gamePanel.setPreferredSize(new Dimension(GameSettings.screenWidth, GameSettings.screenHeight));

        frame.add(gamePanel);
        frame.pack(); // Re-pack the frame to fit the new panel
        frame.repaint();
        gamePanel.requestFocusInWindow();
    }

    public static void main(String[] args) {
        // 在啟動前先載入設定
        GameSettings.loadSettings();

        // 使用 SwingUtilities.invokeLater 確保 GUI 在正確的線程上啟動
        SwingUtilities.invokeLater(() -> {
            // --- 載入訓練好的 AI 模型 ---
            final String MODEL_FILE = "best_snake_model.txt";
            // 網路結構必須和訓練時完全一樣
            NeuralNetwork bestBrain = NeuralNetwork.loadFromFile(MODEL_FILE, 6, 8, 3);
            // 如果模型載入失敗，顯示錯誤訊息並結束程式
            if (bestBrain == null) {
                JOptionPane.showMessageDialog(null,
                        "找不到或無法載入AI模型檔案: " + MODEL_FILE + "\n請先執行 Train.java 來產生模型。",
                        "錯誤", JOptionPane.ERROR_MESSAGE);
                System.exit(1); // 結束程式
            }

            // 建立主視窗
            JFrame frame = new JFrame();
            frame.setTitle("Snake AI Training");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // 建立一個空的 GamePanel，它會在訓練結束後被更新
            GamePanel gamePanel = new GamePanel(null);
            frame.add(gamePanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // 讓 GamePanel 獲得焦點
            gamePanel.requestFocusInWindow();

            // 啟動一個新執行緒來運行訓練，這樣 GUI 就不會凍結
            new Thread(() -> {
                // 建立訓練管理器
                PopulationManager manager = new PopulationManager(500, 0.05);
                manager.initializePopulation(6, 8, 3);
                System.out.println("AI training...");

                // 執行多個訓練世代
                for (int generation = 0; generation < 100; generation++) { // Start with a smaller number of generations for testing
                    manager.runGeneration();
                    System.out.println("Generation " + (generation + 1) + " finished. Best fitness: " + manager.getBestFitness());
                    manager.createNextGeneration();
                }

                // 使用 SwingUtilities.invokeLater 來更新 GUI
                SwingUtilities.invokeLater(() -> {
                    gamePanel.setBrain(bestBrain);
                    gamePanel.initGame(); // 啟動遊戲
                });
            }).start();
        });
    }
}