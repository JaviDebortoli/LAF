package Argumentation.LAF.DTO.Response;

public class NarrationMetaResponse {
    private String model;
    private String promptVersion;
    private String generatedAt;

    public String getModel() {
        return model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }
}
