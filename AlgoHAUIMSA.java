// package ca.pfv.spmf.algorithms.frequentpatterns.HUIM_SA_HC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* This file is copyright (c) Saqib Nawaz, Philippe Fournier-Viger et al.
* 
* This file is part of the SPMF DATA MINING SOFTWARE
* (http://www.philippe-fournier-viger.com/spmf).
* 
* SPMF is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* 
* SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* SPMF. If not, see <http://www.gnu.org/licenses/>.
*/

/**
 *This is an implementation of the high utility itemset mining algorithm
 * based on Simulated annealing. 
 * 
 * More details can be found in this paper: <br/><br/>
 * 	Nawaz, M.S., Fournier-Viger, P., Yun, U., Wu, Y., Song, W. (2021). 
 * Mining High Utility Itemsets with Hill Climbing and Simulated Annealing. 
 * ACM Transactions on Management Information Systems
 * 
 * @author Saqib Nawaz
 */
public class AlgoHAUIMSA {
    /** Maksimum bellek kullanımı */
    double maxMemory = 0;
    /** Algoritmanın başlangıç zamanı */
    long startTimestamp = 0;
    /** Algoritmanın bitiş zamanı */
    long endTimestamp = 0;
    /** Toplam işlem sayısı */
    int transactionCount = 0;

    /** Popülasyon boyutu */
    final int pop_size = 30;
    /** Simüle Tavlama başlangıç sıcaklığı */
    double temperature = 100000;
    /** Minimum sıcaklık */
    double min_temp = .00001;
    /** Alpha */
    double alpha = 0.9993;

    /** Minimum Ortalama Fayda */
    double minAvgUtility;

    /** Öğelerin TWU değerlerini tutan harita */
    Map<Integer, Integer> mapItemToTWU;
    List<Integer> twuPattern;

    BufferedWriter writer = null;

    /** Kromozomları temsil eden sınıf */
    class ChroNode implements Comparable<ChroNode> {
        BitSet chromosome;
        double avgUtility;

        public ChroNode(int length) {
            chromosome = new BitSet(length);
        }

        /** Ortalama faydayı hesaplama */
        public void calculateAverageUtility(List<Integer> templist) {
            if (templist.size() == 0) return;
            int totalUtility = 0;
            int itemCount = chromosome.cardinality(); // Seçili öğe sayısı

            for (int p : templist) {
                int sum = 0;
                for (int i = 0; i < twuPattern.size(); i++) {
                    if (chromosome.get(i)) {
                        sum += mapItemToTWU.getOrDefault(twuPattern.get(i), 0);
                    }
                }
                totalUtility += sum;
            }
            if (itemCount > 0) {
                this.avgUtility = (double) totalUtility / itemCount; // Ortalama fayda
            }
        }

        @Override
        public int compareTo(ChroNode o) {
            return Double.compare(o.avgUtility, this.avgUtility);
        }
    }

    List<ChroNode> population = new ArrayList<>();
    List<ChroNode> subPopulation = new ArrayList<>();
    List<ChroNode> highAvgUtilityItemsets = new ArrayList<>();

    public AlgoHAUIMSA(double minAvgUtility) {
        this.minAvgUtility = minAvgUtility;
    }

    public void runAlgorithm(String input, String output) throws IOException {
        maxMemory = 0;
        startTimestamp = System.currentTimeMillis();
        writer = new BufferedWriter(new FileWriter(output));
        mapItemToTWU = new HashMap<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.isEmpty() || line.charAt(0) == '#' || line.charAt(0) == '%' || line.charAt(0) == '@') {
                continue;
            }
            String[] split = line.split(":");
            String[] items = split[0].split(" ");
            int transactionUtility = Integer.parseInt(split[1]);

            for (String itemStr : items) {
                int item = Integer.parseInt(itemStr);
                mapItemToTWU.put(item, mapItemToTWU.getOrDefault(item, 0) + transactionUtility);
            }
        }
        reader.close();

        twuPattern = new ArrayList<>(mapItemToTWU.keySet());
        Collections.sort(twuPattern);

        initializePopulation();

        double T = temperature;
        while (T > min_temp) {
            subPopulation = neighbor();
            T *= alpha;
            updatePopulation();
        }

        writeOut();
        checkMemory();
        writer.close();
        endTimestamp = System.currentTimeMillis();
    }

    private void initializePopulation() {
        for (int i = 0; i < pop_size; i++) {
            ChroNode node = new ChroNode(twuPattern.size());
            int count = (int) (Math.random() * twuPattern.size());

            for (int j = 0; j < count; j++) {
                int index = (int) (Math.random() * twuPattern.size());
                node.chromosome.set(index);
            }
            node.calculateAverageUtility(twuPattern);
            population.add(node);

            if (node.avgUtility >= minAvgUtility) {
                highAvgUtilityItemsets.add(node);
            }
        }
    }

    private List<ChroNode> neighbor() {
        for (ChroNode node : population) {
            int index = (int) (Math.random() * twuPattern.size());
            node.chromosome.flip(index);
            node.calculateAverageUtility(twuPattern);

            if (node.avgUtility >= minAvgUtility) {
                highAvgUtilityItemsets.add(node);
            }
        }
        return subPopulation;
    }

    private void updatePopulation() {
        population.addAll(subPopulation);
        Collections.sort(population);
        while (population.size() > pop_size) {
            population.remove(population.size() - 1);
        }
        subPopulation.clear();
    }

    private void writeOut() throws IOException {
        for (ChroNode node : highAvgUtilityItemsets) {
            writer.write(node.chromosome + " #AVG_UTILITY: " + node.avgUtility + "\n");
        }
    }

    private void checkMemory() {
        double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
        if (currentMemory > maxMemory) {
            maxMemory = currentMemory;
        }
    }

    public void printStats() {
        System.out.println("=============  HAUI-SA ALGORITHM - STATS =============");
        System.out.println(" Toplam süre ~ " + (endTimestamp - startTimestamp) + " ms");
        System.out.println(" Bellek kullanımı ~ " + maxMemory + " MB");
        System.out.println(" Yüksek Ortalama Faydalı Küme Sayısı: " + highAvgUtilityItemsets.size());
        System.out.println("=======================================================");
    }
}
