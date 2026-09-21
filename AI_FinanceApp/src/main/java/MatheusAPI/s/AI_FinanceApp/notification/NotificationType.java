package MatheusAPI.s.AI_FinanceApp.notification;

public enum NotificationType {
    GOAL_CONTRIBUTION,  // alguém aportou numa meta de grupo
    GOAL_MILESTONE,     // uma meta cruzou 50% / 75% / 100%
    MEMBER_JOINED,      // alguém entrou no grupo familiar via código de convite
    MEMBER_LEFT         // alguém saiu do grupo familiar por conta própria
}
