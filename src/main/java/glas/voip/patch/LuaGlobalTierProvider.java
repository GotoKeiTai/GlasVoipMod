package glas.voip.patch;

import se.krka.kahlua.vm.KahluaTable;
import zombie.Lua.LuaManager;

/**
 * Reads each player's chosen voice tier from a Lua global table populated by this mod's
 * (not-yet-written) Lua side: {@code _G.GlasVoipTiers[onlineId] = tier}. This is a
 * hand-synchronized contract with a Lua file outside this repo -- the compiler can't check
 * either end, so both the table name and the key/value shapes below must match exactly.
 * <p>
 * The key MUST be a Lua number, not a string: Lua has only one number type, always
 * represented as {@code java.lang.Double} on Kahlua's stack (confirmed by disassembling
 * {@code KahluaTableImpl}/{@code KahluaThread} from the actual game install), so
 * {@code GlasVoipTiers[onlineId] = tier} in Lua and {@code rawget((double) onlineId)} here
 * address the same entry. A Lua-side {@code GlasVoipTiers[tostring(onlineId)] = tier} would
 * silently never be found by this class -- indistinguishable from "mod not loaded yet."
 */
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
        if (!(tierValue instanceof Double doubleValue) || Math.rint(doubleValue) != doubleValue) {
            return null;
        }

        return doubleValue.intValue();
    }
}
