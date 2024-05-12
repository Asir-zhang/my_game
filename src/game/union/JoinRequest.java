package game.union;

public class JoinRequest {
    long player;
    ApproveStatus approve;      // 是否正在被审批

    public JoinRequest(long player) {
        this.player = player;
    }

    public ApproveStatus getApproveStatus() {
        return approve;
    }

    public void setApproveStatus(ApproveStatus newStatus) {
        this.approve = newStatus;
    }

    public enum ApproveStatus {
        // 未处理，正在处理，已处理
        UNHANDLED,
        HANDLING,
        HANDLED
    }
}
