package glas.voip.patch;

public interface PlayerTierProvider {
    Integer getTierFor(short onlineId);
}
