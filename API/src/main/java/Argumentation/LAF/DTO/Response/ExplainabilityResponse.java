package Argumentation.LAF.DTO.Response;

public class ExplainabilityResponse {
    private boolean enabled;
    private String status;
    private String message;

    public boolean isEnabled() {
        return enabled;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
