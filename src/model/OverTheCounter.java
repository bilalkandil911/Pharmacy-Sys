package model;

public class OverTheCounter extends Medicine {
    private static final long serialVersionUID = 3L;
    private static final double DISCOUNT_RATE = 0.05; // discount for OTC drugs

    private String category;

    public OverTheCounter(String name, String manufacturer, double price,
                          int stock, String category) {
        super(name, manufacturer, price, stock); //getting the parent constr
        this.category = category; // adding the attribute of the class
    }

    @Override
    public double getDiscountRate() { return DISCOUNT_RATE; }

    // no need to ovr the func requiresPers() it's alr false

    public String getCategory()           { return category; } // pain relief , Cold and flu , digestion
    public void setCategory(String cat)   { this.category = cat; }
}