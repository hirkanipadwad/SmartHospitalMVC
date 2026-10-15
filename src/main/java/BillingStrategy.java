/** PRACTICAL 7: STRATEGY - calculate the payable amount without changing BillingService. */
interface BillingStrategy {
    long payableCents(long baseCents);
    String name();
}
class SelfPayStrategy implements BillingStrategy {
    public long payableCents(long baseCents) { return baseCents; }
    public String name() { return "Self-Pay"; }
}
class InsuranceStrategy implements BillingStrategy {
    public long payableCents(long baseCents) { return Math.round(baseCents * 0.30); }
    public String name() { return "Insurance"; }
}
class CorporateStrategy implements BillingStrategy {
    public long payableCents(long baseCents) { return Math.round(baseCents * 0.75); }
    public String name() { return "Corporate"; }
}
class SeniorCitizenStrategy implements BillingStrategy {
    public long payableCents(long baseCents) { return Math.round(baseCents * 0.70); }
    public String name() { return "Senior Citizen"; }
}
class BillingService {
    BillingStrategy choose(String key) {
        return switch (key) {
            case "Self-Pay" -> new SelfPayStrategy();
            case "Insurance" -> new InsuranceStrategy();
            case "Corporate" -> new CorporateStrategy();
            case "Senior Citizen" -> new SeniorCitizenStrategy();
            default -> throw new IllegalArgumentException("Unknown billing policy.");
        };
    }
}
