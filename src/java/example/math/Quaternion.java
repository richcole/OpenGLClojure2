package example.math;

public class Quaternion extends Vector {

    public final static Quaternion ZERO = new Quaternion(0, 0, 0, 1);

    public Quaternion() {
        super();
    }

    public Quaternion(float x1, float x2, float x3, float x4) {
        super(x1, x2, x3, x4);
    }

    public Quaternion(Vector o) {
        super(o);
    }

    public Quaternion(float[] v) {
        this.v = v;
    }

    public Matrix toMatrix() {
        float qx = v[0];
        float qy = v[1];
        float qz = v[2];
        float qw = v[3];

        return new Matrix(
                1.0f - 2.0f*qy*qy - 2.0f*qz*qz, 2.0f*qx*qy - 2.0f*qz*qw, 2.0f*qx*qz + 2.0f*qy*qw, 0.0f,
                2.0f*qx*qy + 2.0f*qz*qw, 1.0f - 2.0f*qx*qx - 2.0f*qz*qz, 2.0f*qy*qz - 2.0f*qx*qw, 0.0f,
                2.0f*qx*qz - 2.0f*qy*qw, 2.0f*qy*qz + 2.0f*qx*qw, 1.0f - 2.0f*qx*qx - 2.0f*qy*qy, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        );
    }

    public Quaternion times(float s) {
        return new Quaternion(v[0]*s, v[1]*s, v[2]*s, v[3]*s);
    }
    
    public Vector rotate(Vector v) {
    	Quaternion p = new Quaternion(v.x(), v.y(), v.z(), 0);
    	Quaternion q_conj = this.conjugate();
    	Quaternion r = this.times(p).times(q_conj);
    	return new Vector(r.v[0], r.v[1], r.v[2], 1.0f);
    }

    public Quaternion plus(Quaternion o) {
        return new Quaternion(v[0] + o.v[0], v[1] + o.v[1], v[2] + o.v[2], v[3] + o.v[3]);
    }

    public Quaternion minus(Quaternion o) {
        return new Quaternion(v[0] - o.v[0], v[1] - o.v[1], v[2] - o.v[2], v[3] - o.v[3]);
    }

    public Quaternion conjugate() {
        float l = length();
        Quaternion r = new Quaternion(-v[0] / l, -v[1] / l, -v[2] / l, v[3] / l);
        return r;
    }

    public float theta() {
        return (float) Math.acos(v[3]);
    }

    public Quaternion power(float p) {
        float theta = theta();
        float ctheta = (float) Math.cos(p*theta);
        float stheta = (float) Math.sin(p*theta);
        return new Quaternion(v[0]*stheta, v[1]*stheta, v[2]*stheta, ctheta);
    }

    public Quaternion times(Quaternion o) {
        float w1 = v[3];
        float x1 = v[0];
        float y1 = v[1];
        float z1 = v[2];
        float w2 = o.v[3];
        float x2 = o.v[0];
        float y2 = o.v[1];
        float z2 = o.v[2];
        Quaternion r = new Quaternion(
                w1*x2 + x1*w2 + y1*z2 - z1*y2,
                w1*y2 - x1*z2 + y1*w2 + z1*x2,
                w1*z2 + x1*y2 - y1*x2 + z1*w2,
                w1*w2 - x1*x2 - y1*y2 - z1*z2
        );
        return r;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public float lengthSquared() {
        return v[0]*v[0] + v[1]*v[1] + v[2]*v[2] + v[3]*v[3];
    }

    public float dot(Quaternion o) {
        return v[0]*o.v[0] + v[1]*o.v[1] + v[2]*o.v[2] + v[3]*o.v[3];
    }

    public Quaternion scaleTo(float length) {
        float l = length;
        return new Quaternion(v[0]/l, v[1]/l, v[2]/l, v[3]);
    }
}
