import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;

public class MainAlgorithmManager {

    public static void main(String[] args) throws Exception {
        // Klasör isimleri
        String dataFolder = "data";
        String outputFolder = "output";
        int minUtility = 100;

        // Klasörleri oluştur
        new File(dataFolder).mkdir();
        new File(outputFolder).mkdir();

        // minUtility değerine özel klasör oluştur
        File specificOutputFolder = new File(outputFolder, String.valueOf(minUtility));
        specificOutputFolder.mkdir();

        // Veri dosyalarının isimlerini liste olarak tanımla
        List<String> inputFiles = Arrays.asList(
                "retail_utility.txt",
                "accidents_utility.txt",
                "chess_utility.txt",
                "mushroom_utility.txt",
                "foodmart_utility.txt"
        );

        // Çalışma dizinini yazdır
        System.out.println("Current working directory: " + System.getProperty("user.dir"));

        // Rapor dosyasını oluştur
        File reportFile = new File(specificOutputFolder, "report.txt");
        FileWriter reportWriter = new FileWriter(reportFile);
        reportWriter.write("MinUtility: " + minUtility + "\n");
        reportWriter.write("FileName,ExecutionTime(ms),FileSize(bytes)\n");

        // Her giriş dosyası için algoritmayı çalıştır
        for (String inputFile : inputFiles) {
            File inputFilePath = new File(dataFolder, inputFile);
            
            // Giriş dosyası mevcut değilse uyarı ver ve devam et
            if (!inputFilePath.exists()) {
                System.out.println("Input file not found: " + inputFilePath.getAbsolutePath());
                reportWriter.write(inputFile + ",ERROR: File Not Found,0\n");
                continue;
            }

            // Çıktı dosya ismini oluştur (girdi isminin sonuna 'Output' ekleyerek)
            String outputFileName = inputFile.replace(".txt", "Output.txt");
            File outputFilePath = new File(specificOutputFolder, outputFileName);

            // Algoritmayı başlat
            AlgoHUIMSA algo = new AlgoHUIMSA();
            System.out.println("Running algorithm on " + inputFile + " with minUtility: " + minUtility + "...");

            long startTime = System.currentTimeMillis();
            algo.runAlgorithm(inputFilePath.getAbsolutePath(), outputFilePath.getAbsolutePath(),minUtility);
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;

            System.out.println("Algorithm completed for " + inputFile + " in " + executionTime + " ms");

            // Çıktı dosyasının oluşup oluşmadığını kontrol et
            if (outputFilePath.exists()) {
                long fileSize = outputFilePath.length();
                System.out.println("Output file created: " + outputFilePath.getAbsolutePath());
                System.out.println("Output file size: " + fileSize + " bytes");
                reportWriter.write(inputFile + "," + executionTime + "," + fileSize + "\n");
            } else {
                System.out.println("Output file not created for " + inputFile);
                reportWriter.write(inputFile + ",ERROR: Output Not Created,0\n");
            }
        }
        
        // Rapor dosyasını kapat
        reportWriter.close();
        System.out.println("Report generated: " + reportFile.getAbsolutePath());
    }
}
