package game.entity.hostile;

import java.util.Random;

public interface HostileActions {

    /**
     * Resolves a hit-chance roll for this enemy's attack.
     * Generates a random integer in [1, Integer.MAX_VALUE] and returns true
     * if it is less than or equal to the accuracy threshold {@code acc}.
     * Higher acc values (e.g. 100) virtually guarantee a hit.
     *
     * @param acc  accuracy ceiling — the roll must be <= acc to hit
     * @param rand shared Random instance to avoid re-seeding per turn
     * @return true if the attack connects, false if it misses
     */
    default boolean atk(int acc, Random rand) {
        int x = rand.nextInt() + 1;
        return x <= acc;
    }
}
