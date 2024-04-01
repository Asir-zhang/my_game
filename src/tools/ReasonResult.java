package tools;

public class ReasonResult {
    public boolean result = false;

    String reason = "";

    public static ReasonResult SUCCESS = new ReasonResult(true, "");

    private ReasonResult(boolean result, String reason, Object... args) {
        this.result = result;
        String formattedMessage = reason.replace("{}", "%s");
        this.reason = String.format(formattedMessage, args);
    }

    public static ReasonResult failure(String reason, Object... args) {
        return new ReasonResult(false, reason, args);
    }

    public String getReason() {
        return reason;
    }

    public ReasonResult setReason(String reason) {
        this.reason = reason;
        return this;
    }
}
