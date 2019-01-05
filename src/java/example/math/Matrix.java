package example.math;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;

public class Matrix {

    public float v[] = new float[16];

	private FloatBuffer buf;

    public static final Matrix IDENTITY = Matrix.id();
    public static final Matrix BASIS = Matrix.rows(Vector.U1, Vector.U2, Vector.U3, Vector.Z);
    public static final Matrix ONES = Matrix.ones();
    public static final Matrix ZERO = IDENTITY.minus(IDENTITY);

    public Matrix(Matrix4f mat) {
        mat.transpose().get(v);
    }

    public Matrix times(Matrix m) {
        Matrix r = new Matrix();
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; j++) {
                int index = i * 4 + j;
                for (int k = 0; k < 4; ++k) {
                    r.v[index] += v[i * 4 + k] * m.v[k * 4 + j];
                }
            }
        }
        return r;
    }

    public Matrix(float... v) {
        if (v.length == 0) {
            this.v = new float[16];
        } else {
            this.v = v;
        }
    }

    public Matrix(Matrix o) {
        v = new float[16];
        for (int i = 0; i < 16; ++i) {
            v[i] = o.v[i];
        }
    }

    public static Matrix ones() {
        Matrix r = new Matrix();
        for (int i = 0; i < 16; ++i) {
            r.v[i] = 1;
        }
        return r;
    }

    public Vector times(Vector o) {
        Vector r = new Vector();
        for (int i = 0; i < 4; ++i) {
            for (int k = 0; k < 4; ++k) {
                r.v[i] += v[i * 4 + k] * o.v[k];
            }
        }
        return r;
    }

    public static Matrix skew(Vector v) {
        Matrix r = new Matrix();
        r.v[1] = -v.v[2];
        r.v[2] = v.v[1];
        r.v[4] = v.v[2];
        r.v[6] = -v.v[0];
        r.v[8] = -v.v[1];
        r.v[9] = v.v[0];
        r.v[15] = v.v[3];
        return r;
    }

    public static Matrix square(Vector v) {
        Matrix r = new Matrix();
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; j++) {
                r.v[i * 4 + j] += v.v[i] * v.v[j];
            }
        }
        return r;
    }

    public static Matrix frustum(float left, float right, float top, float bottom, float near, float far) {
        Matrix r = new Matrix();
        r.set(0, 0, 2 * near / (right - left));
        r.set(1, 1, 2 * near / (top - bottom));
        r.set(0, 2, (right + left) / (right - left));
        r.set(1, 2, (top + bottom) / (top - bottom));
        r.set(2, 2, (near + far) / (near - far));
        r.set(2, 3, 2 * far * near / (near - far));
        r.set(3, 2, -1);
        return r;
    }

    public static Matrix rows(Vector v1, Vector v2, Vector v3, Vector v4) {
        Matrix r = new Matrix();
        Vector v[] = new Vector[4];
        v[0] = v1;
        v[1] = v2;
        v[2] = v3;
        v[3] = v4;
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                r.v[i * 4 + j] = v[i].v[j];
            }
        }
        return r;
    }

    public static Matrix columns(Vector v1, Vector v2, Vector v3, Vector v4) {
        return Matrix.rows(v1, v2, v3, v4).transpose();
    }

    public Matrix transpose() {
        Matrix r = new Matrix();
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 4; ++j) {
                r.v[i * 4 + j] = v[j * 4 + i];
            }
        }
        return r;
    }

    public static Matrix id() {
        Matrix r = new Matrix();
        for (int i = 0; i < 4; ++i) {
            r.v[i * 4 + i] = 1;
        }
        return r;
    }

    public Matrix minus(Matrix o) {
        Matrix r = new Matrix();
        for (int i = 0; i < 16; ++i) {
            r.v[i] = v[i] - o.v[i];
        }
        return r;
    }

    public Matrix plus(Matrix o) {
        Matrix r = new Matrix();
        for (int i = 0; i < 16; ++i) {
            r.v[i] = v[i] + o.v[i];
        }
        return r;
    }

    public Matrix times(float d) {
        Matrix r = new Matrix();
        for (int i = 0; i < 16; ++i) {
            r.v[i] = v[i] * d;
        }
        return r;
    }

    public static Matrix rot(float theta, Vector x) {
        x = x.normalize();
        Matrix square = Matrix.square(x);
        Matrix r = square.plus(
        		Matrix.IDENTITY
        			.minus(square)
        			.times((float)Math.cos(theta))
        			.plus(Matrix.skew(x)
					.times((float)Math.sin(theta))));
        for (int i = 0; i < 4; ++i) {
            r.v[i * 4 + 3] = i == 3 ? 1 : 0;
            r.v[3 * 4 + i] = i == 3 ? 1 : 0;
        }
        return r;
    }

    public static Matrix rot2(float theta, Vector v) {
    	float c = (float) Math.cos(theta);
    	float s = (float) Math.sin(theta);
    	float t = 1 - c;
    	float x = v.x();
    	float y = v.y();
    	float z = v.z();

    	Matrix result = new Matrix();
    	result.set(0, 0, t*x*x + c);
    	result.set(0, 1, t*x*y-s*z);
    	result.set(0, 2, t*x*z+s*y);
    	result.set(0, 3, 0);
    	
    	result.set(1, 0, t*x*y + s*z);
    	result.set(1, 1, t*y*y + c);
    	result.set(1, 2, t*y*z - s*x);
    	result.set(1, 3, 0);

    	result.set(2, 0, t*x*z - s*y);
    	result.set(2, 1, t*y*z + s*x);
    	result.set(2, 2, t*z*z + c);
    	result.set(2, 3, 0);

    	result.set(3, 0, 0);
    	result.set(3, 1, 0);
    	result.set(3, 2, 0);
    	result.set(3, 3, 1);

    	return result;
    }

    public static Matrix translate(Vector x) {
        Matrix tr = new Matrix(Matrix.IDENTITY);
        for (int i = 0; i < 3; ++i) {
            tr.v[i * 4 + 3] = x.v[i];
        }
        return tr;
    }

    public static Matrix scale(float x) {
        Matrix tr = new Matrix(Matrix.ZERO);
        for (int i = 0; i < 4; ++i) {
            tr.v[i * 4 + i] = x;
        }
        tr.v[15] = 1.0f;
        return tr;
    }

    public static Matrix flip(int index) {
    	Matrix tr = new Matrix(Matrix.IDENTITY);
    	tr.set(index, index, -1);
    	return tr;
    }

    public static Matrix scale(Vector x) {
        Matrix tr = new Matrix(Matrix.ZERO);
        for (int i = 0; i < 4; ++i) {
            tr.v[i * 4 + i] = x.v[i];
        }
        return tr;
    }

    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("[ ");
        for (int i = 0; i < 16; ++i) {
            b.append(String.format("%2.2f", v[i]));
            if (i != 15) {
                if (i % 4 == 3) {
                    b.append("; ");
                } else {
                    b.append(", ");
                }
            }
        }
        b.append(" ]");
        return b.toString();
    }

    public boolean withinDelta(Matrix o, float delta) {
        for (int i = 0; i < 4; ++i) {
            for (int j = 0; j < 3; ++j) {
                if (Math.abs(v[i * 4 + j] / v[i * 4 + 3] - o.v[i * 4 + j] / o.v[i * 4 + 3]) > delta) {
                    return false;
                }
            }
        }
        return true;
    }

    public Vector row(int i) {
        Vector r = new Vector();
        for (int j = 0; j < 4; ++j) {
            r.v[j] = v[i * 4 + j];
        }
        return r;
    }

    public FloatBuffer toBuf() {
    	if ( buf == null ) {
	        buf = BufferUtils.createFloatBuffer(16);
	        writeToBuffer(buf);
    	}
        return buf;
    }

    public Vector col(int j) {
        Vector r = new Vector();
        for (int i = 0; i < 4; ++i) {
            r.v[i] = v[i * 4 + j];
        }
        return r;
    }

    public float get(int i, int j) {
        return v[i * 4 + j];
    }

    public float get(int i) {
        return v[i];
    }

    public void set(int i, int j, float value) {
        v[i * 4 + j] = value;
    }

    public void writeToBuffer(FloatBuffer tr) {
        tr.rewind();
        for (int i = 0; i < v.length; ++i) {
            tr.put((float) v[i]);
        }
        tr.flip();
    }


}
