package glas.voip.patch;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

public class LuaGlobalTierProvider implements PlayerTierProvider {

    static final String LUA_TABLE_NAME = "GlasVoipTiers";

    @Override
    public Integer getTierFor(short onlineId) {
        KahluaTable env = LuaManager.env;
        if (env == null) {
            return null;
        }

        Object tiersTable = env.rawget(LUA_TABLE_NAME);
        if (!(tiersTable instanceof KahluaTable)) {
            return null;
        }

        Object tierValue = ((KahluaTable) tiersTable).rawget((double) onlineId);
        if (!(tierValue instanceof Double)) {
            return null;
        }

        return ((Double) tierValue).intValue();
    }
}
