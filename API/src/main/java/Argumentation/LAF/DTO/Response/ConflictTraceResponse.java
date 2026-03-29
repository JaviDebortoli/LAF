package Argumentation.LAF.DTO.Response;

public class ConflictTraceResponse {
    private String leftLiteral;
    private String rightLiteral;
    private String[] leftDelta;
    private String[] rightDelta;
    private String winner;
    private String winnerReason;

    public String getLeftLiteral() {
        return leftLiteral;
    }

    public String getRightLiteral() {
        return rightLiteral;
    }

    public String[] getLeftDelta() {
        return leftDelta;
    }

    public String[] getRightDelta() {
        return rightDelta;
    }

    public String getWinner() {
        return winner;
    }

    public String getWinnerReason() {
        return winnerReason;
    }

    public void setLeftLiteral(String leftLiteral) {
        this.leftLiteral = leftLiteral;
    }

    public void setRightLiteral(String rightLiteral) {
        this.rightLiteral = rightLiteral;
    }

    public void setLeftDelta(String[] leftDelta) {
        this.leftDelta = leftDelta;
    }

    public void setRightDelta(String[] rightDelta) {
        this.rightDelta = rightDelta;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public void setWinnerReason(String winnerReason) {
        this.winnerReason = winnerReason;
    }
}
