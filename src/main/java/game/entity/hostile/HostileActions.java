package game.entity.hostile;

import java.util.Random;

public interface HostileActions {
    public default boolean atk(int acc, Random rand){
        int x =  rand.nextInt()+1;
        if (x<=acc)return true;
        return false;
    }
}
