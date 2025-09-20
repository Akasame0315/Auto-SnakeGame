import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NeuralNetwork {

    private Random random;

    // 輸入層到隱藏層的權重矩陣
    private double[][] weightsInputToHidden;
    // 隱藏層到輸出層的權重矩陣
    private double[][] weightsHiddenToOutput;

    // 隱藏層的偏置陣列
    private double[] biasHidden;
    // 輸出層的偏置陣列
    private double[] biasOutput;

    // 神經網路的結構參數
    private final int numInputs;
    private final int numHidden;
    private final int numOutputs;

    // 儲存神經網路的基因組 (所有權重和偏置)
    private double[] genome;

    public NeuralNetwork(int numInputs, int numHidden, int numOutputs) {
        this.numInputs = numInputs;
        this.numHidden = numHidden;
        this.numOutputs = numOutputs;
        this.random = new Random();

        // 初始化權重和偏置
        weightsInputToHidden = new double[numInputs][numHidden];
        weightsHiddenToOutput = new double[numHidden][numOutputs];
        biasHidden = new double[numHidden];
        biasOutput = new double[numOutputs];

        // 計算基因組的總長度
        int genomeSize = (numInputs * numHidden) + (numHidden * numOutputs) + numHidden + numOutputs;
        genome = new double[genomeSize];
    }

    // 接收遊戲的輸入數據，並計算出輸出決策。
    public double[] predict(double[] inputs) {
        if (inputs.length != numInputs) {
            throw new IllegalArgumentException(
                    "Expected input length " + numInputs + " but got " + inputs.length);
        }

        double[] finalOutputs = computeOutputs(inputs);

        // 3. 找出最大值對應的索引，即為決策
        // 使用Softmax激活函數，讓輸出更像概率分布
        double sum = 0;
        for (int i = 0; i < finalOutputs.length; i++) {
            finalOutputs[i] = Math.exp(finalOutputs[i]);
            sum += finalOutputs[i];
        }

        for (int i = 0; i < finalOutputs.length; i++) {
            finalOutputs[i] /= sum;
        }
        
        return finalOutputs;
    }

    // 將一個基因組（double[] 陣列）載入到神經網路中，設定它的權重和偏置。
    public void setGenome(double[] newGenome) {
        this.genome = java.util.Arrays.copyOf(newGenome, newGenome.length);
        int index = 0;

        // 從基因組中載入輸入層到隱藏層的權重
        for (int i = 0; i < numInputs; i++) {
            for (int j = 0; j < numHidden; j++) {
                weightsInputToHidden[i][j] = genome[index++];
            }
        }

        // 從基因組中載入隱藏層的偏置
        for (int i = 0; i < numHidden; i++) {
            biasHidden[i] = genome[index++];
        }

        // 從基因組中載入隱藏層到輸出層的權重
        for (int i = 0; i < numHidden; i++) {
            for (int j = 0; j < numOutputs; j++) {
                weightsHiddenToOutput[i][j] = genome[index++];
            }
        }

        // 從基因組中載入輸出層的偏置
        for (int i = 0; i < numOutputs; i++) {
            biasOutput[i] = genome[index++];
        }
    }

    // ============== 改進的隱藏層計算 ==============
    // ReLU (Rectified Linear Unit) 啟動函數、線性激活
    private double[] computeOutputs(double[] inputs) {
        double[] hiddenOutputs = new double[numHidden];

        // 隱藏層使用ReLU
        for (int i = 0; i < numHidden; i++) {
            double sum = 0;
            for (int j = 0; j < numInputs; j++) {
                sum += inputs[j] * weightsInputToHidden[j][i];
            }
            sum += biasHidden[i]; //在加權求和的結果上，再加入一個偏差值 biasHidden[i]。這個偏差就像是神經元的“基礎活化度”，即使輸入都是零，它也會產生一個非零的輸出。
            hiddenOutputs[i] = Math.max(0, sum);
        }

        // 輸出層使用線性激活（配合後續的Softmax）
        double[] finalOutputs = new double[numOutputs];
        for (int i = 0; i < numOutputs; i++) {
            double sum = 0;
            for (int j = 0; j < numHidden; j++) {
                sum += hiddenOutputs[j] * weightsHiddenToOutput[j][i];
            }
            sum += biasOutput[i];
            finalOutputs[i] = sum;
        }
        return finalOutputs;
    }

    public double[] getGenome() {
        return genome;
    }

    public double[] getFinalOutputs(double[] inputs) {
        if (inputs.length != numInputs) {
            throw new IllegalArgumentException(
                    "Expected input length " + numInputs + " but got " + inputs.length);
        }

        return computeOutputs(inputs);
    }

    /**
     * 將當前的神經網路（基因組）儲存到檔案中
     *
     * @param filePath 檔案路徑
     */
    public void saveToFile(String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (double gene : this.genome) {
                writer.write(String.valueOf(gene));
                writer.newLine();
            }
            System.out.println("model save to: " + filePath);
        } catch (IOException e) {
            System.err.println("Have error when save model: " + e.getMessage());
        }
    }


    /**
     * 從檔案載入基因組來建立一個神經網路
     * 這是一個靜態工廠方法
     * @param filePath 檔案路徑
     * @param numInputs 輸入層節點數
     * @param numHidden 隱藏層節點數
     * @param numOutputs 輸出層節點數
     * @return 一個載入了權重的新 NeuralNetwork 物件，如果失敗則回傳 null
     */
    public static NeuralNetwork loadFromFile(String filePath, int numInputs, int numHidden, int numOutputs) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            NeuralNetwork nn = new NeuralNetwork(numInputs, numHidden, numOutputs);
            List<Double> loadedGenome = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                loadedGenome.add(Double.parseDouble(line));
            }

            if (loadedGenome.size() != nn.getGenome().length) {
                System.err.println("錯誤：檔案中的基因組長度與網路結構不符！");
                return null;
            }

            double[] genomeArray = loadedGenome.stream().mapToDouble(d -> d).toArray();
            nn.setGenome(genomeArray); // 非常重要的一步：將讀取的基因組設定到網路中
            System.out.println("模型已成功從 " + filePath + " 載入。");
            return nn;

        } catch (IOException | NumberFormatException e) {
            System.err.println("載入模型時發生錯誤: " + e.getMessage());
            return null;
        }
    }
}