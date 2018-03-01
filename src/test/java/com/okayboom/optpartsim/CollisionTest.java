package com.okayboom.optpartsim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;

import org.junit.Test;
import org.quicktheories.WithQuickTheories;

import com.kastrull.fritz.physics.LinearPhysics;
import com.kastrull.fritz.physics.Physics;
import com.kastrull.fritz.primitives.Coord;
import com.okayboom.optpartsim.CollisionsGenerator.Collision;
import com.okayboom.optpartsim.CollisionsGenerator.P;

import lytharn.particles.main.CollisionDetector;
import lytharn.particles.main.Particle;
import lytharn.particles.main.Vector2D;

/** Test class for different implementations of collision detection. */
public class CollisionTest implements WithQuickTheories {

	/** Acceptable relative error (delta) when comparing doubles. */
	private static final double ACCEPTABLE_RELATIVE_ERROR = 1e-3;

	@Test
	public void collisionDetectionMevensson() {
		assertCollisions(collision -> {
			// type conversion of particles)
			eu.evensson.optpartsim.physics.Particle a = toMevenssonParticle(collision.alfa);
			eu.evensson.optpartsim.physics.Particle b = toMevenssonParticle(collision.beta);

			return a.collisionTime(b);
		});
	}

	/** Convert to particle of Mevensson-type. */
	eu.evensson.optpartsim.physics.Particle toMevenssonParticle(P p) {
		return new eu.evensson.optpartsim.physics.Particle(1, 0, p.pos, p.vel);
	}

	@Test
	public void collisionDetectionJockbert() {

		Physics physics = new LinearPhysics();

		assertCollisions(collision -> physics.collisionTime(
			toJockbertParticle(collision.alfa),
			toJockbertParticle(collision.beta)));
	}

	/** Convert to particle of Jockbert-type. */
	com.kastrull.fritz.primitives.Particle toJockbertParticle(P p) {
		Coord pos = Coord.c(p.pos.x(), p.pos.y());
		Coord vel = Coord.c(p.vel.x(), p.vel.y());
		return new com.kastrull.fritz.primitives.Particle(pos, vel);
	}

	@Test
	public void collisionDetectionLytharn() {
		assertCollisions(collision -> {
			Particle a = toLytharnParticle(collision.alfa);
			Particle b = toLytharnParticle(collision.beta);

			OptionalDouble result = CollisionDetector.timeToCollision(a, b);
			return convertOptional(result);
		});
	}

	/** Convert to particle of Lytharn-type. */
	lytharn.particles.main.Particle toLytharnParticle(P alfa) {
		Vector2D position = new Vector2D(alfa.pos.x(), alfa.pos.y());
		Vector2D velocity = new Vector2D(alfa.vel.x(), alfa.vel.y());
		double radius = 1.0;
		return new lytharn.particles.main.Particle(position, velocity, radius);
	}

	private static Optional<Double> convertOptional(OptionalDouble od) {
		return od.isPresent() ? Optional.of(od.getAsDouble()) : Optional.empty();
	}

	/**
	 * Asserts that collision function classifies the collision correctly or a
	 * series of different (randomly generated) test cases.
	 */
	void assertCollisions(Function<Collision, Optional<Double>> collisionTimeFn) {
		CollisionsGenerator generator = new CollisionsGenerator();

		qt()
			.forAll(generator.collisions())
			.checkAssert((Collision collision) -> {

				Optional<Double> maybeCollisionTime = collisionTimeFn.apply(collision);

				assertTrue(maybeCollisionTime.isPresent());

				double expected = collision.collisionTime;
				Double actual = maybeCollisionTime.get();

				assertEquals(expected, actual, expected * ACCEPTABLE_RELATIVE_ERROR);
			});
	}
}
