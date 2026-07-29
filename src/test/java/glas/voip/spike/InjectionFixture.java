package glas.voip.spike;

public class InjectionFixture {

    private static float minDistance = 1.0f;
    private static float maxDistance = 100.0f;

    /**
     * Mirrors the real target method's shape: the adjacent maxDistance-then-minDistance pair
     * (here fed straight into combine(), matching how the real code feeds both values
     * directly as consecutive smoothstep() arguments with no intervening instruction), plus a
     * second, non-adjacent read of maxDistance later in the same method -- the injector must
     * only match the first.
     */
    float computeSomething(int a, int b, int c, int d, int e, int f, int g, Object speaker) {
        float combined = combine(maxDistance, minDistance);
        if (combined > maxDistance) {
            return -1.0f;
        }
        return combined;
    }

    private static float combine(float x, float y) {
        return x - y;
    }
}
