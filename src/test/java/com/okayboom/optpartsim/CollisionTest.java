package com.okayboom.optpartsim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;
import org.quicktheories.WithQuickTheories;

import com.kastrull.fritz.physics.LinearPhysics;
import com.kastrull.fritz.physics.Physics;
import com.kastrull.fritz.primitives.Coord;
import com.okayboom.optpartsim.CollisionsGenerator.Collision;
import com.okayboom.optpartsim.CollisionsGenerator.P;

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

	com.kastrull.fritz.primitives.Particle toJockbertParticle(P p) {
		Coord pos = Coord.c(p.pos.x(), p.pos.y());
		Coord vel = Coord.c(p.vel.x(), p.vel.y());
		return new com.kastrull.fritz.primitives.Particle(pos, vel);
	}

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
