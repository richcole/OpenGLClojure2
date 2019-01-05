package example.math;

import org.lwjgl.opengl.GL11;

import java.util.Arrays;

public class Vector {

	float v[] = new float[4];

	public static final Vector U1 = new Vector(1, 0, 0, 1);
	public static final Vector U2 = new Vector(0, 1, 0, 1);
	public static final Vector U3 = new Vector(0, 0, 1, 1);

	public static final Vector M1 = new Vector(-1,  0,   0, 1);
	public static final Vector M2 = new Vector( 0,  -1,  0, 1);
	public static final Vector M3 = new Vector( 0,  0,  -1, 1);

	public static final Vector RIGHT = U1;
	public static final Vector UP    = U2;
	public static final Vector FWD   = M3;

	public static final Vector Z = new Vector(0, 0, 0, 1);
	public static final Vector ONES = new Vector(1, 1, 1, 1);

	public static final Vector BL = new Vector( 0,  0,  0, 1);
	public static final Vector BR = new Vector( 1,  0,  0, 1);
	public static final Vector TL = new Vector( 0,  1,  0, 1);
	public static final Vector TR = new Vector( 1,  1,  0, 1);

	static {
		if (!UP.cross(RIGHT).equals(FWD)) {
			throw new RuntimeException("Not right handed " + RIGHT.cross(UP) + " " + FWD);
		}
	}

	public Vector(float x1, float x2, float x3, float x4) {
		v[0] = x1;
		v[1] = x2;
		v[2] = x3;
		v[3] = x4;
	}

	public Vector(float x1, float x2, float x3) {
		v[0] = x1;
		v[1] = x2;
		v[2] = x3;
		v[3] = 1;
	}

	public Vector() {
	}

	public Vector(Vector o) {
		for (int i = 0; i < 4; ++i) {
			v[i] = o.v[i];
		}
	}

	public Vector(float[] v) {
		this.v = v;
	}

	public Vector times(float s) {
		return new Vector(v[0] * s, v[1] * s, v[2] * s, v[3]);
	}

	public Vector project() {
		return new Vector(v[0] / v[3], v[1] / v[3], v[2] / v[3], 1.0f);
	}

	public Vector project(int i) {
		Vector result = new Vector(0, 0, 0, 1);
		result.set(i, v[i] / v[3]);
		return result;
	}

	public Vector normalize() {
		return new Vector(v[0], v[1], v[2], unscaledLength());
	}

	public float unscaledLength() {
		return (float) Math.sqrt(unscaledLengthSquared());
	}

	public float unscaledLengthSquared() {
		return v[0] * v[0] + v[1] * v[1] + v[2] * v[2];
	}

	public float length() {
		return unscaledLength() / v[3];
	}

	public Vector cross(Vector o) {
		return Matrix.skew(this).times(o);
	}

	public String toString() {
		StringBuffer b = new StringBuffer();
		b.append("[ ");
		for (int i = 0; i < 3; ++i) {
			b.append(String.format("%2.2f", v[i] / v[3]));
			if (i != 2) {
				b.append(", ");
			}
		}
		b.append(" ]");
		return b.toString();
	}

	public boolean withinDelta(Vector o, float delta) {
		for (int i = 0; i < 3; ++i) {
			if (Math.abs(v[i] / v[4] - o.v[i] / o.v[4]) > delta) {
				return false;
			}
		}
		return true;
	}

	public Vector minus() {
		return times(-1);
	}

	public void glTranslate() {
		GL11.glTranslated(v[0] / v[3], v[1] / v[3], v[2] / v[3]);
	}

	public void glRotate(float theta) {
		GL11.glRotated(theta * 180 / Math.PI, v[0] / v[3], v[1] / v[3], v[2] / v[3]);
	}

	public Vector times(Matrix m) {
		Vector r = new Vector();
		for (int j = 0; j < 4; ++j) {
			for (int k = 0; k < 4; ++k) {
				r.v[j] += v[k] * m.v[k * 4 + j];
			}
		}
		return r;
	}

	public Vector scaleTo(float s) {
		if (s < 1e-6) {
			return new Vector(0, 0, 0, 1);
		} else {
			Vector r = new Vector(this);
			r.v[3] = unscaledLength() / s;
			return r;
		}
	}

	public Vector plus(Vector o) {
		Vector r = new Vector();
		for (int i = 0; i < 3; ++i) {
			r.v[i] = v[i] / v[3] + o.v[i] / o.v[3];
		}
		r.v[3] = 1.0f;
		return r;
	}

	public float x() {
		return v[0] / v[3];
	}

	public float y() {
		return v[1] / v[3];
	}

	public float z() {
		return v[2] / v[3];
	}
	
	public float getValue(int d) {
	  return v[d] / v[3];	  
	}

	public Vector minus(Vector o) {
		Vector r = new Vector();
		for (int i = 0; i < 3; ++i) {
			r.v[i] = v[i] / v[3] - o.v[i] / o.v[3];
		}
		r.v[3] = 1.0f;
		return r;
	}

	public float lengthSquared() {
		return unscaledLengthSquared() / (v[3] * v[3]);
	}

	public float get(int i) {
		return v[i];
	}

	public void set(int i, float value) {
		v[i] = value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(v);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vector other = (Vector) obj;
		if (!Arrays.equals(v, other.v))
			return false;
		return true;
	}

	public float dot(Vector o) {
		return (v[0] * o.v[0] + v[1] * o.v[1] + v[2] * o.v[2]) / (v[3] * o.v[3]);
	}

	public float theta(Vector u1, Vector u2) {
		float x1 = dot(u1) / (length() * u1.length());
		float x2 = dot(u2) / (length() * u2.length());
		return (float) Math.atan2(x2, x1);
	}

	public float[] toDoubleArray() {
		return v;
	}

	public Vector elementTimes(Vector o) {
		return new Vector(v[0] * o.x(), v[1] * o.y(), v[2] * o.z(), v[3]);
	}

	public Vector divide(Vector o) {
		return new Vector(v[0] / o.v[0], v[1] / o.v[1], v[2] / o.v[2], v[3] / o.v[3]);
	}

	public Vector floor() {
		return new Vector((float) Math.floor(x()), (float) Math.floor(y()), (float) Math.floor(z()), 1f);
	}

	public Vector round() {
		return new Vector(Math.round(x()), Math.round(y()), Math.round(z()), 1);
	}

	public boolean lessThan(Vector other) {
		return x() < other.x() && y() < other.y() && z() < other.z();
	}

	public boolean greaterThan(Vector other) {
		return x() > other.x() && y() > other.y() && z() > other.z();
	}

	public boolean lessThanOrEqual(Vector other) {
		return x() <= other.x() && y() <= other.y() && z() <= other.z();
	}

	public boolean greaterThanOrEqual(Vector other) {
		return x() >= other.x() && y() >= other.y() && z() >= other.z();
	}

	public Vector scaleBy(Vector d) {
		return new Vector(x() * d.x(), y() * d.y(), z() * d.z(), 1.0f);
	}

  public float distSquared(Vector v2) {
    float dx = x() - v2.x();
    float dy = y() - v2.y();
    float dz = z() - v2.z();
    return dx * dx + dy * dy + dz * dz;
  }

  public float dist(Vector v2) {
    float dx = x() - v2.x();
    float dy = y() - v2.y();
    float dz = z() - v2.z();
    return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  public static Vector UNIT(int dim) {
    Vector r = new Vector(0.0f, 0.0f, 0.0f, 1.0f);
    r.v[dim] = 1.0f;
    return r;
  }

	public Vector abs() {
		return new Vector(Math.abs(x()), Math.abs(y()), Math.abs(z()), 1.0f);
	}
}
