package com.spite.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document("share_invites")
public class ShareInvite {

    @Id
    private String id;

    private String fromUsername;
    private String toUsername;
    private String type; // "exercise" | "workout"
    private List<String> itemIds;
    private List<String> itemNames; // snapshot za prikaz
    private String status; // "PENDING" | "ACCEPTED" | "DECLINED"
    private Instant createdAt = Instant.now();

    public ShareInvite() {}

    public ShareInvite(String fromUsername, String toUsername, String type,
                       List<String> itemIds, List<String> itemNames) {
        this.fromUsername = fromUsername;
        this.toUsername = toUsername;
        this.type = type;
        this.itemIds = itemIds;
        this.itemNames = itemNames;
        this.status = "PENDING";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }

    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<String> getItemIds() { return itemIds; }
    public void setItemIds(List<String> itemIds) { this.itemIds = itemIds; }

    public List<String> getItemNames() { return itemNames; }
    public void setItemNames(List<String> itemNames) { this.itemNames = itemNames; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
