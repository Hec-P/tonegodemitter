/*
 * Copyright (c) 2009-2012 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package emitter.influencers;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.util.SafeArrayList;
import java.io.IOException;
import emitter.Interpolation;
import emitter.particle.ParticleData;

/**
 *
 * @author t0neg0d
 */
public class RotationInfluencer implements ParticleInfluencer {
	private SafeArrayList<Vector3f> speeds = new SafeArrayList(Vector3f.class);
	private SafeArrayList<Interpolation> interpolations = new SafeArrayList(Interpolation.class);
	private boolean initialized = false;
	private boolean enabled = true;
	private boolean cycle = false;
	
	private Vector3f speedFactor = Vector3f.ZERO.clone();
	private boolean useRandomDirection = true;
	private boolean useRandomSpeed = true;
	private boolean direction = true;
	
	private boolean useRandomStartRotationX = false;
	private boolean useRandomStartRotationY = false;
	private boolean useRandomStartRotationZ = false;
	private float blend;
	private float fixedDuration = 0f;
	
	private Vector3f startRotation = new Vector3f();
	private Vector3f endRotation = new Vector3f();
	
	public void update(ParticleData p, float tpf) {
		if (enabled) {
			if (speeds.size() > 1) {
				p.rotationInterval += tpf;
				if (p.rotationInterval >= p.rotationDuration)
					updateRotation(p);

				blend = p.rotationInterpolation.apply(p.rotationInterval/p.rotationDuration);
				
				p.rotationSpeed.interpolate(p.startRotationSpeed, p.endRotationSpeed, blend);
			}
			p.angles.addLocal(p.rotationSpeed.mult(tpf));
		}
	}

	private void updateRotation(ParticleData p) {
	//	startRotation.set(p.rotationSpeed);
	//	endRotation.set(speeds.getArray()[p.rotationIndex]);
		p.rotationIndex++;
		if (!cycle) {
			if (p.rotationIndex == speeds.size()-1)
				p.rotationIndex = 0;
		} else {
			if (p.rotationIndex == speeds.size())
				p.rotationIndex = 0;
		}
		
		getRotationSpeed(p, p.rotationIndex, p.startRotationSpeed);
		
		int index = p.rotationIndex+1;
		if (index == speeds.size())
			index = 0;
		
		getRotationSpeed(p, index, p.endRotationSpeed);
		
		p.rotationInterpolation = interpolations.getArray()[p.rotationIndex];
		p.rotationInterval -= p.rotationDuration;
	}
	
	public void initialize(ParticleData p) {
		if (!initialized) {
			if (speeds.isEmpty()) {
				addRotationSpeed(new Vector3f(0,10,0));
			}
			initialized = true;
		}
		p.rotationIndex = 0;
		p.rotationInterval = 0f;
		p.rotationDuration = (cycle) ? fixedDuration : p.startlife/((float)speeds.size()-1);
		
		if (useRandomDirection) {
			p.rotateDirectionX = FastMath.rand.nextBoolean();
			p.rotateDirectionY = FastMath.rand.nextBoolean();
			p.rotateDirectionZ = FastMath.rand.nextBoolean();
		}
		
		getRotationSpeed(p, p.rotationIndex, p.startRotationSpeed);
		p.rotationSpeed.set(p.startRotationSpeed);
		if (speeds.size() > 1) {
			getRotationSpeed(p, p.rotationIndex+1, p.endRotationSpeed);
		}
		
		p.rotationInterpolation = interpolations.getArray()[p.rotationIndex];
		
		if (useRandomStartRotationX || useRandomStartRotationY || useRandomStartRotationZ) {
			p.angles.set(
				useRandomStartRotationX ? FastMath.nextRandomFloat()*FastMath.TWO_PI : 0,
				useRandomStartRotationY ? FastMath.nextRandomFloat()*FastMath.TWO_PI : 0,
				useRandomStartRotationZ ? FastMath.nextRandomFloat()*FastMath.TWO_PI : 0
			);
		} else {
			p.angles.set(0,0,0);
		}
	}
	
	private void getRotationSpeed(ParticleData p, int index, Vector3f store) {
		store.set(speeds.getArray()[index]);
		if (useRandomSpeed) {
			store.set(
				FastMath.nextRandomFloat()*store.x,
				FastMath.nextRandomFloat()*store.y,
				FastMath.nextRandomFloat()*store.z
			);
		}
		if (useRandomDirection) {
			store.x = p.rotateDirectionX ? store.x : -store.x;
			store.y = p.rotateDirectionY ? store.y : -store.y;
			store.z = p.rotateDirectionZ ? store.z : -store.z;
		}
	}
	
	public void reset(ParticleData p) {
		p.angles.set(0,0,0);
	}
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled;
	}
	
	public void addRotationSpeed(Vector3f speeds) {
		addRotationSpeed(speeds, Interpolation.linear);
	}
	
	public void addRotationSpeed(Vector3f speed, Interpolation interpolation) {
		this.speeds.add(speed.clone());
		this.interpolations.add(interpolation);
	}
	
	public void removeRotation(int index) {
		this.speeds.remove(index);
		this.interpolations.remove(index);
	}
	
	public void removeAll() {
		this.speeds.clear();
		this.interpolations.clear();
	}
	
	public Vector3f[] getRotations() {
		return this.speeds.getArray();
	}
	
	public Interpolation[] getInterpolations() {
		return this.interpolations.getArray();
	}
	
	/**
	 * Allows the influencer to choose a random rotation direction per axis as the particle is emitted.
	 * @param useRandomDirection boolean
	 */
	public void setUseRandomDirection(boolean useRandomDirection) {
		this.useRandomDirection = useRandomDirection;
	}
	
	/**
	 * Returns if the influencer currently selects a random rotation direction per axis as the particle is emitted.
	 * @return boolean
	 */
	public boolean getUseRandomDirection() {
		return this.useRandomDirection;
	}
	
	/**
	 * Allows the influencer to select a random rotation speed from 0 to the provided maximum speeds per axis
	 * @param useRandomSpeed boolean
	 */
	public void setUseRandomSpeed(boolean useRandomSpeed) {
		this.useRandomSpeed = useRandomSpeed;
	}
	
	/**
	 * Returns if the influencer currently to selects random rotation speeds per axis
	 * @return boolean
	 */
	public boolean getUseRandomSpeed() {
		return this.useRandomSpeed;
	}
	
	public void setUseRandomStartRotation(boolean xRotation, boolean yRotation, boolean zRotation) {
		useRandomStartRotationX = xRotation;
		useRandomStartRotationY = yRotation;
		useRandomStartRotationZ = zRotation;
	}
	
	public boolean getUseRandomStartRotationX() { return this.useRandomStartRotationX; }
	public boolean getUseRandomStartRotationY() { return this.useRandomStartRotationY; }
	public boolean getUseRandomStartRotationZ() { return this.useRandomStartRotationZ; }
	
	/**
	 * Forces the rotation direction to always remain constant per particle
	 * @param direction boolean 
	 */
	public void setDirection(boolean direction) { this.direction = direction; }
	
	/**
	 * Returns if the rotation direction will always remain constant per particle
	 * @return 
	 */
	public boolean getDirection() { return this.direction; }
	
	public void write(JmeExporter ex) throws IOException {
		OutputCapsule oc = ex.getCapsule(this);
        oc.write(speedFactor, "speedFactor", Vector3f.ZERO);
	}

	public void read(JmeImporter im) throws IOException {
		InputCapsule ic = im.getCapsule(this);
		speedFactor = (Vector3f) ic.readSavable("speedFactor", Vector3f.ZERO.clone());
	}
	
	@Override
	public ParticleInfluencer clone() {
		try {
			RotationInfluencer clone = (RotationInfluencer) super.clone();
			return clone;
		} catch (CloneNotSupportedException e) {
			throw new AssertionError();
		}
	}

	public Class getInfluencerClass() {
		return RotationInfluencer.class;
	}
}