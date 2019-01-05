package example.math;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class Utils {

	public static FloatBuffer toFloatBuffer(float[] floats) {
		FloatBuffer buf = BufferUtils.createFloatBuffer(floats.length).put(floats);
		buf.flip();
		return buf;
	}

	public static IntBuffer toIntBuffer(int[] ints) {
		IntBuffer buf = BufferUtils.createIntBuffer(ints.length).put(ints);
		buf.flip();
		return buf;
	}

	public static IntBuffer toIntBuffer(List<Integer> ints) {
		IntBuffer buf = BufferUtils.createIntBuffer(ints.size());
		for (Integer value : ints) {
			buf.put(value);
		}
		buf.flip();
		return buf;
	}

	public static FloatBuffer toFloatBuffer(List<Float> ints) {
		FloatBuffer buf = BufferUtils.createFloatBuffer(ints.size());
		for (Float value : ints) {
			buf.put(value);
		}
		buf.flip();
		return buf;
	}

	public static ByteBuffer toByteBuffer(byte[] bytes) {
		ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length).put(bytes);
		buf.flip();
		return buf;
	}

	public static ByteBuffer toByteBuffer(String name) {
		byte[] bytes = name.getBytes();
		ByteBuffer buf = BufferUtils.createByteBuffer(bytes.length + 1);
		buf.put(bytes);
		buf.put((byte) 0);
		buf.flip();
		return buf;
	}

	public static List<Integer> range(int lower, int upper) {
		List<Integer> result = new ArrayList<>();
		for (int i = lower; i < upper; ++i) {
			result.add(i);
		}
		return result;
	}

	public static List<Integer> range(int upper) {
		return range(0, upper);
	}

	public static float clamp(float v, float l, float u) {
		if (v < l) {
			return l;
		}
		if (v > u) {
			return u;
		}
		return v;
	}

	public static float mix(float x1, float x2, float alpha) {
		return (1 - alpha) * x1 + alpha * x2;
	}

	public static float squared(float d) {
		return d * d;
	}

	public static float[] toDoubleArray3(List<Vector> vs) {
		float[] result = new float[vs.size() * 3];
		int idx = 0;
		for (Vector v : vs) {
			result[idx++] = v.x();
			result[idx++] = v.y();
			result[idx++] = v.z();
		}
		return result;
	}

	public static float[] toDoubleArray16(List<Matrix> vs) {
		float[] result = new float[vs.size() * 16];
		int idx = 0;
		for (Matrix v : vs) {
			for(int i=0;i<v.v.length;++i) {
				result[idx++] = v.v[i];
			}
		}
		return result;
	}

	public static float[] toDoubleArray16(Matrix ... vs) {
		float[] result = new float[vs.length * 16];
		int idx = 0;
		for (Matrix v : vs) {
			for(int i=0;i<v.v.length;++i) {
				result[idx++] = v.v[i];
			}
		}
		return result;
	}

	public static int[] toIntArray(List<Integer> is) {
		int[] result = new int[is.size()];
		int idx = 0;
		for(Integer i: is) {
			result[idx++] = i;
		}
		return result;
	}

	public static int[] toIntArray(Integer ... is) {
		int[] result = new int[is.length];
		int idx = 0;
		for(Integer i: is) {
			result[idx++] = i;
		}
		return result;
	}

	public static float[] toDoubleArray2(List<Vector> vs) {
		float[] result = new float[vs.size() * 2];
		int idx = 0;
		for (Vector v : vs) {
			result[idx++] = v.x();
			result[idx++] = v.y();
		}
		return result;
	}

	public static float[] toDoubleArray(List<Integer> vs) {
		float[] result = new float[vs.size()];
		int idx = 0;
		for (Integer v : vs) {
			result[idx++] = v;
		}
		return result;
	}

	public static float[] toDoubleArray(float ... vs) {
		float[] result = new float[vs.length];
		int idx = 0;
		for (float v : vs) {
			result[idx++] = v;
		}
		return result;
	}

	public static Quaternion lerp(float alpha, Quaternion p, Quaternion q) {
		return p.times(alpha).plus(q.times(1f - alpha));
	}

	public static Vector lerp(float alpha, Vector p, Vector q) {
		return p.times(alpha).plus(q.times(1f - alpha));
	}
}
