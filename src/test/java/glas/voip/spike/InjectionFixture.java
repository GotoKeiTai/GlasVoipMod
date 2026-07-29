package glas.voip.spike;

public class InjectionFixture {

    private static float minDistance = 1.0f;
    private static float maxDistance = 100.0f;

    float computeSomething(int a, int b, int c, int d, int e, int f, int g, Object speaker) {
        float max = maxDistance;
        float min = minDistance;
        return max - min;
    }
}
