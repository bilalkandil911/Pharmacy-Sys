package model;

import java.io.Serializable;

public abstract class Medicine implements Serializable {
    private static final long serialVersionUID = 1L; // version stamp

    private String name;
    private String manufacturer;
    private double price;
    private int stock;

    public Medicine(String name, String manufacturer, double price, int stock) {
        this.name         = name;
        this.manufacturer = manufacturer;
        this.price        = price;
        this.stock        = stock;
    }

    public abstract double getDiscountRate();

    public double getFinalPrice() {
        return price * (1 - getDiscountRate());
    }

    public boolean requiresPrescription() {
        return false;
    }

    public String getPrescriptionLevel() {
        return "N/A";
    }

    // gets
    public String getName()         { return name; }
    public String getManufacturer() { return manufacturer; }
    public double getPrice()        { return price; }
    public int    getStock()        { return stock; }

    // sets
    public void setName(String name)                { this.name = name; }
    public void setManufacturer(String m)           { this.manufacturer = m; }
    public void setPrice(double price)              { this.price = price; }
    public void setStock(int stock)                 { this.stock = stock; }

    public String getType() {
        return (this instanceof PrescriptionDrug) ? "Rx" : "OTC"; // is this drug Rx if yes return "Rx" else return "OTC"
    }

    public String getExtra() {
        if (this instanceof PrescriptionDrug pd) return pd.getPrescriptionLevel();
        if (this instanceof OverTheCounter otc) return otc.getCategory();
        return "";
    }

    @Override
    public String toString() {
        return String.format("[%s] %-25s | %-15s | $%6.2f -> $%6.2f | Stock: %d | %s",
                getType(), name, manufacturer, price, getFinalPrice(), stock,
                requiresPrescription() ? "Prescription: " + getPrescriptionLevel() : "No prescription");
    }
}