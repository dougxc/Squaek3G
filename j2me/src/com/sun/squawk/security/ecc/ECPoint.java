package com.sun.squawk.security.ecc;


public class ECPoint {
    
    public int[] x;
    public int[] y;
    public int[] z;
    
    protected final ECCurveFp curve;
    protected final FFA ffa;
    
    private ECPoint(ECPoint p) {
        this.curve = p.curve;
        this.ffa = curve.getField().getFFA();
        x = ffa.acquireVar();
        y = ffa.acquireVar();
        z = ffa.acquireVar();
        ffa.copy(x, p.x);
        ffa.copy(y, p.y);
        ffa.copy(z, p.z);
    }
    
    public ECPoint(ECCurveFp curve, int[] x, int[] y) {
        this.curve = curve;
        this.ffa = curve.getField().getFFA();
        this.x = x;
        this.y = y;
        this.z = ffa.acquireVar();
        ffa.set(z, 1);
    }
    
    public ECPoint(ECCurveFp curve) {
        this.curve = curve;
        this.ffa = curve.getField().getFFA();
        this.x = ffa.acquireVar();
        this.y = ffa.acquireVar();
        this.z = ffa.acquireVar();
    }
    
    public Object clone() {
        return clonePoint();
    }
    
    public ECPoint clonePoint() {
        return new ECPoint(this);
    }
    
    public void release() {
        ffa.releaseVar(x);
        ffa.releaseVar(y);
        ffa.releaseVar(z);
    }
    
}
