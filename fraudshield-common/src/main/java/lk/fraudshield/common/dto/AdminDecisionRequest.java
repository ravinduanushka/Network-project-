package lk.fraudshield.common.dto;

public class AdminDecisionRequest {
    private String txId;
    private String account;
    private String decision;
    private String adminUser;

    public AdminDecisionRequest() {}

    public AdminDecisionRequest(String txId, String account, String decision, String adminUser) {
        this.txId = txId;
        this.account = account;
        this.decision = decision;
        this.adminUser = adminUser;
    }

    public String getTxId() {
        return txId;
    }

    public String getAccount() {
        return account;
    }

    public String getDecision() {
        return decision;
    }

    public String getAdminUser() {
        return adminUser;
    }
}
