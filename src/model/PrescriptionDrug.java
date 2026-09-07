package model;

public class PrescriptionDrug extends Medicine {
    private static final long serialVersionUID = 2L; // same version stamp
    private static final double DISCOUNT_RATE = 0.15; // discount for Rx drugs

    private String prescriptionLevel; // restricted "Gadwal"

    public PrescriptionDrug(String name, String manufacturer, double price,
                            int stock, String prescriptionLevel) {
        super(name, manufacturer, price, stock);
        this.prescriptionLevel = prescriptionLevel;
    }

    @Override
    public double getDiscountRate() { return DISCOUNT_RATE; }

    @Override
    public boolean requiresPrescription() { return true; }

    @Override
    public String getPrescriptionLevel() { return prescriptionLevel; }

    public void setPrescriptionLevel(String level) { this.prescriptionLevel = level; }
}