package MatheusAPI.s.AI_FinanceApp.user;

import java.util.EnumSet;
import java.util.Set;

import static MatheusAPI.s.AI_FinanceApp.user.AccPermissions.*;

public enum AccType {
    MEMBER(EnumSet.of(READ, WRITE, DELETE)),
    DEVELOPER(EnumSet.allOf(AccPermissions.class));

    private final Set<AccPermissions> defaultPermissions;

    AccType(Set<AccPermissions> defaultPermissions) {
        this.defaultPermissions = defaultPermissions;
    }

    public Set<AccPermissions> getDefaultPermissions() {
        return EnumSet.copyOf(defaultPermissions);
    }
}
