package com.example.pokeworld.entities;

public class Mission {
    private String title;
    private String description;
    private String targetType; // Ex: "Slime", "Peixe", "Lobo Selvagem"
    private int requiredAmount;
    private int currentAmount;
    private int rewardGold;
    private boolean isCompleted;
    private boolean isRewarded;

    public Mission(String title, String description, String targetType, int requiredAmount, int rewardGold) {
        this.title = title;
        this.description = description;
        this.targetType = targetType;
        this.requiredAmount = requiredAmount;
        this.rewardGold = rewardGold;
        this.currentAmount = 0;
        this.isCompleted = false;
        this.isRewarded = false;
    }

    public void updateProgress(String type) {
        if (!isCompleted && type.equalsIgnoreCase(targetType)) {
            currentAmount++;
            if (currentAmount >= requiredAmount) {
                isCompleted = true;
            }
        }
    }

    public String getStatusText() {
        if (isRewarded) return "Finalizada";
        if (isCompleted) return "Concluída (Fale com o NPC)";
        return currentAmount + "/" + requiredAmount;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getRewardGold() { return rewardGold; }
    public boolean isCompleted() { return isCompleted; }
    public boolean isRewarded() { return isRewarded; }
    public void setRewarded(boolean rewarded) { isRewarded = rewarded; }
}
