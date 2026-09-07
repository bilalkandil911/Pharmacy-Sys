package util;

import model.Medicine;
import model.OverTheCounter;
import model.PrescriptionDrug;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class FileManager {

    private final String filePath;

    public FileManager(String filePath) {
        this.filePath = filePath;
    }

    public List<Medicine> loadInventory() {
        List<Medicine> list = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("[FileManager] inventory.csv not found at: " + filePath);
            return list;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))){ // easier than reading char by char
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } // skipping header
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", -1); // trimming
                if (parts.length < 6) continue;
                String name         = parts[0].trim();
                String type         = parts[1].trim();
                String manufacturer = parts[2].trim();
                double price        = Double.parseDouble(parts[3].trim());
                int    stock        = Integer.parseInt(parts[4].trim());
                String extra        = parts[5].trim();
                if (type.equalsIgnoreCase("Rx")) {
                    list.add(new PrescriptionDrug(name, manufacturer, price, stock, extra));
                } else {
                    list.add(new OverTheCounter(name, manufacturer, price, stock, extra));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("[FileManager] Error reading file: " + e.getMessage());
        }
        return list;
    }

    public void saveInventory(List<Medicine> inventory) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("name,type,manufacturer,price,stock,extra");
            for (Medicine m : inventory) {
                pw.printf("%s,%s,%s,%.2f,%d,%s%n",
                        m.getName(), m.getType(), m.getManufacturer(),
                        m.getPrice(), m.getStock(), m.getExtra());
            }
        } catch (IOException e) {
            System.err.println("[FileManager] Error saving file: " + e.getMessage());
        }
    }
}