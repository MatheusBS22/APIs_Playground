package MatheusAPI.s.AI_FinanceApp.goal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalProjection(
        long monthsNeeded,
        LocalDate projectedCompletionDate,
        boolean onTrack,
        BigDecimal monthlyDifference
) {}
