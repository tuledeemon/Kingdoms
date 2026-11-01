package com.celik.kingdoms;

/** Damage policy belongs only to TNT directly lit with the artifact. */
public interface ArtifactTnt {
    void kingdoms$policy(double multiplier,double maximum);
    float kingdoms$damage(float vanilla);
    static float damage(float vanilla,double multiplier,double maximum){return (float)Math.min(maximum,Math.max(0,(double)vanilla*multiplier));}
}
