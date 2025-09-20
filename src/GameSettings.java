import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class GameSettings {

    private static final String SETTINGS_FILE = "game_settings.properties";

    public static int gameSpeed;
    public static int screenWidth;
    public static int screenHeight;
    // 基因長度 = (輸入層 * 隱藏層) + (隱藏層 * 輸出層) + 隱藏層偏置 + 輸出層偏置
    // (22 * 16) + (16 * 3) + 16 + 3 = 352 + 48 + 16 + 3 = 355
    public static final int GENE_SIZE = 419;
    public static int UNIT_SIZE = 25; // 每個方塊的大小

    // 提供數個預設的速度選項 (單位：毫秒)
    public static final int[] GAME_SPEEDS = {150, 100, 75, 50, 25};

    // 提供數個預設的畫面比例選項 (寬, 高)
    public static final int[][] SCREEN_SIZES = {
            // 4:3 比例
            {800, 600},
            {1025, 775},
            {1200, 900},
            // 16:9 比例
            {1280, 720},
            {1920, 1080}
    };

    // 新增變數來儲存選項的索引，而不是直接儲存數值
    public static int speedIndex = 2; // 預設速度為第 3 個選項 (75ms)
    public static int sizeIndex = 0;  // 預設畫面為第 1 個選項 (800x600)
    public static int MAX_STEPS = 500;

    // 讀取設定
    public static void loadSettings() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(SETTINGS_FILE)) {
            props.load(fis);
            speedIndex = Integer.parseInt(props.getProperty("GameSpeed", "2"));
            sizeIndex = Integer.parseInt(props.getProperty("ScreenSize", "0"));
        } catch (IOException | NumberFormatException e) {
            System.err.println("Can't find the setting file, will use default setting.");
        }
        // 根據索引值更新實際的參數
        updateGameParameters();
    }

    // 寫入設定
    public static void saveSettings() {
        Properties props = new Properties();
        // 儲存索引值
        props.setProperty("GameSpeed", String.valueOf(speedIndex));
        props.setProperty("ScreenSize", String.valueOf(sizeIndex));

        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            props.store(fos, "Game Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 根據索引更新參數，以便在讀取和變更時使用
    public static void updateGameParameters() {
        gameSpeed = GAME_SPEEDS[speedIndex];
        screenWidth = SCREEN_SIZES[sizeIndex][0];
        screenHeight = SCREEN_SIZES[sizeIndex][1];
        MAX_STEPS = screenWidth + screenHeight;
    }
}