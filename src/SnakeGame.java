import javax.swing.JFrame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

public class SnakeGame extends JFrame {

    public SnakeGame() {
        // 設定視窗標題
        this.setTitle("Auto SnakeGame");
        // 設定關閉視窗時的行為
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 移除視窗標題和邊框，讓它變為全螢幕
        this.setUndecorated(true);
        // 設定為最大化，填滿整個螢幕
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
//        // 讓視窗顯示在螢幕中央
        this.setLocationRelativeTo(null);

        // 建立並新增 GamePanel
        GamePanel gamePanel = new GamePanel();
        this.add(gamePanel);
        // 顯示視窗
        this.setVisible(true);
    }

    public static void main(String[] args) {
        // 在啟動前先載入設定
        GameSettings.loadSettings();
        // 使用 SwingUtilities.invokeLater 確保 GUI 在正確的線程上啟動
        javax.swing.SwingUtilities.invokeLater(() -> new SnakeGame());
    }
}