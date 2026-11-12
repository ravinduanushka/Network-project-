package lk.fraudshield.fraud.profile;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe store for UserBehaviourProfiles.
 */
public class ProfileStore {
    private final ConcurrentHashMap<String, UserBehaviourProfile> profiles = new ConcurrentHashMap<>();

    public UserBehaviourProfile getProfile(String accountNo) {
        if (accountNo == null) return null;
        return profiles.computeIfAbsent(accountNo, UserBehaviourProfile::new);
    }

    public void clear() {
        profiles.clear();
    }
}
