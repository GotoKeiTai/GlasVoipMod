package se.krka.kahlua.vm;

// Hand-written stub, NOT decompiled game code — only the single method this project's
// production code actually calls (see LuaGlobalTierProvider.java). Used exclusively as a
// compileOnly dependency when the real game's classes aren't available (CI, no Project
// Zomboid install). Never bundled into the shipped jar, never executed: at runtime the JVM
// loads the real zombie.Lua.LuaManager/se.krka.kahlua.vm.KahluaTable from the running game.
public interface KahluaTable {
    Object rawget(Object key);
}
