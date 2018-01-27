package com.okayboom.optpartsim;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.quicktheories.WithQuickTheories;
import org.quicktheories.api.Pair;
import org.quicktheories.core.Gen;

import eu.evensson.optpartsim.physics.Particle;
import eu.evensson.optpartsim.physics.Vector;

/** Generator for collisions. */
public class CollisionsGenerator implements WithQuickTheories {

	// Almost a quarter of a revolution, minus a 100th (to be nice and for
	// numeric safety).
	private static final double DIR_MAX_DEVIATION_FROM_IMPACT_NORMAL = Math.PI / 2.0 - Math.PI / 50;

	private static final double TIME_MAX = 1e4;
	private static final double TIME_MIN = 1e-4;

	private static final double SPEED_MAX = 1e2;
	private static final double SPEED_MIN = 1e-2;

	private static final double IMPACT_POINT_MAX = 1e6;

	/** Rudimentary data class for a particle */
	static class P {
		final Vector pos;
		final Vector vel;

		P(Vector pos, Vector vel) {
			this.pos = pos;
			this.vel = vel;
		}

		@Override
		public String toString() {
			return "Particle(pos=" + pos + ", vel=" + vel + ")";
		}
	}

	/** Container class for all test data in a collision. */
	static class Collision {
		final double collisionTime;
		final P alfa;
		final P beta;
		private String extraInfo;

		Collision(double atTime, P alfa, P beta, String extraInfo) {
			this.collisionTime = atTime;
			this.alfa = alfa;
			this.beta = beta;
			this.extraInfo = extraInfo;
		}

		@Override
		public String toString() {
			return "Collision("
					+ "\ntime=" + collisionTime +
					", \nalfa=" + alfa +
					", \nbeta=" + beta +
					", \ninfo=" + extraInfo
					+ ")";
		}
	}

	Gen<Vector> vectors(double max) {
		Gen<Double> cartesians = doubles().between(-max, max);
		return cartesians.zip(cartesians, Vector::vector);
	}

	Gen<Vector> impactPoints() {
		return vectors(IMPACT_POINT_MAX);
	}

	Gen<Double> speeds() {
		return doubles().between(SPEED_MIN, SPEED_MAX);
	}

	Gen<Pair<Double, Double>> biSpeeds() {
		return speeds().zip(speeds(), Pair::of);
	}

	Gen<Double> durations() {
		return doubles().between(TIME_MIN, TIME_MAX);
	}

	Gen<Double> normals() {
		return doubles().between(-Math.PI, Math.PI);
	}

	Gen<Double> directions() {
		double maxDev = DIR_MAX_DEVIATION_FROM_IMPACT_NORMAL;
		return doubles().between(-maxDev, maxDev);
	}

	Gen<Pair<Double, Double>> biDirections() {
		return directions().zip(directions(), Pair::of);
	}

	Gen<Pair<Double, Double>> dirAndSpeed() {
		return directions().zip(speeds(), Pair::of);
	}

	Gen<Pair<Pair<Double, Double>, Pair<Double, Double>>> biDirAndSpeed() {
		return dirAndSpeed().zip(dirAndSpeed(), Pair::of);
	}

	Gen<Pair<Vector, Double>> impactPosAndNormals() {
		return impactPoints().zip(normals(), Pair::of);
	}

	static String label(String description, Object o) {
		return description + ": " + o;
	}

	static String all(Object... os) {
		return Arrays.asList(os).stream()
			.map(o -> o.toString())
			.collect(Collectors.joining("\n\t", "{\n\t", "\n}"));
	}

	/* Negate radians, i.e. add half rotation. */
	double radNeg(double rad) {
		return rad + Math.PI;
	}

	Gen<Collision> collisions() {
		// calculation backwards from collision moment
		return impactPosAndNormals().zip(
			biDirAndSpeed(),
			durations(),
			(impactPosAndNorm, biDirAndSpeed, duration) -> {

				// value expansion
				Vector impactPos = impactPosAndNorm._1;
				Double impactNormal = impactPosAndNorm._2;

				double alfaDir = biDirAndSpeed._1._1;
				double alfaSpeed = biDirAndSpeed._1._2;
				double betaDir = biDirAndSpeed._2._1;
				double betaSpeed = biDirAndSpeed._2._2;

				// calculations
				Vector alfaCollisionPos = particlePos(impactPos, impactNormal);
				Vector betaCollisionPos = particlePos(impactPos, radNeg(impactNormal));

				P alfa = toStartState(duration, impactPos, impactNormal, alfaDir, alfaSpeed);
				P beta = toStartState(duration, impactPos, radNeg(impactNormal), betaDir, betaSpeed);

				return new Collision(duration, alfa, beta,
					all(
						label("inpact position...", impactPos),
						label("inpact normal.rad.", impactNormal),
						label("alfa dirAndSpeed..", biDirAndSpeed._1),
						label("beta dirAndSpeed..", biDirAndSpeed._2),
						label("time..............", duration),
						label("alfa collisionPos.", alfaCollisionPos),
						label("beta collisionPos.", betaCollisionPos)));
			});
	}

	private Vector particlePos(Vector impactPoint, double collisionPlaneNormal) {
		return impactPoint
			.add(Vector.polar(Particle.RADIUS, collisionPlaneNormal));
	}

	private P toStartState(double duration, Vector impactPoint, double collisionPlaneNormal, double normRelativeDir,
			double speed) {

		double negDir = collisionPlaneNormal + normRelativeDir;
		double dir = negDir + Math.PI;

		double distance = duration * speed;
		Vector negativeOffset = Vector.polar(distance, negDir);

		Vector collisionPos = particlePos(impactPoint, collisionPlaneNormal);
		Vector startPosition = collisionPos.add(negativeOffset);

		Vector velocity = Vector.polar(speed, dir);
		return new P(startPosition, velocity);
	}
}
