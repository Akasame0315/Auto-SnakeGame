import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SettingsFrame extends JFrame {
    private GamePanel gamePanel; // 新增：用來儲存 GamePanel 的參考

    public SettingsFrame(GamePanel gamePanel) {
        this.gamePanel = gamePanel; // 接收 GamePanel 實例

        this.setTitle("Setting");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(false);
        this.setLocationRelativeTo(null);

        // 視窗關閉事件監聽器
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                // 如果是透過點擊視窗的 "X" 來關閉，則繼續遊戲
                if (gamePanel.paused) {
                    gamePanel.resumeGame();
                }
            }
        });

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 速度設定
        JLabel speedLabel = new JLabel("Speed (ms):");
        JTextField speedField = new JTextField(String.valueOf(GameSettings.gameSpeed));
        panel.add(speedLabel);
        panel.add(speedField);

        // 寬度設定
        JLabel widthLabel = new JLabel("Width:");
        JTextField widthField = new JTextField(String.valueOf(GameSettings.screenWidth));
        panel.add(widthLabel);
        panel.add(widthField);

        // 高度設定
        JLabel heightLabel = new JLabel("Height:");
        JTextField heightField = new JTextField(String.valueOf(GameSettings.screenHeight));
        panel.add(heightLabel);
        panel.add(heightField);

        JPanel buttonPanel = new JPanel(new FlowLayout());

        // 確認按鈕
        JButton applyButton = new JButton("Save and Restart");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    // 更新 GameSettings 的值
                    GameSettings.gameSpeed = Integer.parseInt(speedField.getText());
                    GameSettings.screenWidth = Integer.parseInt(widthField.getText());
                    GameSettings.screenHeight = Integer.parseInt(heightField.getText());

                    // 儲存設定
                    GameSettings.saveSettings();
                    // 呼叫 GamePanel 的方法來重新啟動遊戲
                    gamePanel.applyAndRestart();
                    // 關閉當前視窗
                    dispose();

                    // 重新啟動遊戲
                    restartGame();

                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(null, "Invalid Type Param！", "ERROR", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // 取消調整設定參數
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            dispose(); // 關閉視窗，windowClosed 會觸發繼續遊戲
        });

        // 將面板和按鈕新增到視窗
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);

        this.add(panel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.pack();
        this.setVisible(true);
    }

    private void restartGame() {
        // 關閉所有現有視窗
        for (Window window : Window.getWindows()) {
            window.dispose();
        }
        // 重新啟動遊戲
        new SnakeGame();
    }
}