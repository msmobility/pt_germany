package skimCalculator;

import ch.sbb.matsim.analysis.skims.FloatMatrix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MyFloatMatrix<T> {

    final Map<T, Integer> id2index;
    private final int size;
    private final float[] data;

    public MyFloatMatrix(Set<T> zones, float defaultValue) {
        this.size = zones.size();
        this.id2index = new HashMap<>((int) (this.size * 1.5));
        this.data = new float[this.size * this.size];
        Arrays.fill(this.data, defaultValue);
        int index = 0;
        for (T t : zones) {
            this.id2index.put(t, index);
            index++;
        }
    }


    public float set(T from, T to, float value) {
        int index = getIndex(from, to);
        float oldValue = this.data[index];
        this.data[index] = value;
        return oldValue;
    }

    public float get(T from, T to) {
        int index = getIndex(from, to);
        return this.data[index];
    }

    public float add(T from, T to, float value) {
        int index = getIndex(from, to);
        float oldValue = this.data[index];
        float newValue = oldValue + value;
        this.data[index] = newValue;
        return newValue;
    }

    /**
     * @param from
     * @param to
     * @param factor
     * @return the new value
     */
    public float multiply(T from, T to, float factor) {
        int index = getIndex(from, to);
        float oldValue = this.data[index];
        float newValue = oldValue * factor;
        this.data[index] = newValue;
        return newValue;
    }

    /**
     * Multiplies the values in every cell with the given factor.
     *
     * @param factor the multiplication factor
     */
    public void multiply(float factor) {
        for (int i = 0; i < this.data.length; i++) {
            this.data[i] *= factor;
        }
    }

    private int getIndex(T from, T to) {
        int fromIndex = this.id2index.get(from);
        int toIndex = this.id2index.get(to);
        return fromIndex * this.size + toIndex;
    }

}
