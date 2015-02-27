import java.util.*;
class Person {

	private static final int MAX = 8;

	private String name;
	private int hitPoints;
	private int attackPoints;
	private Random r;
	private boolean hasSword;

	public Person(String name, int hitPoints, int attackPoints) {
		hasSword = true;
		this.hitPoints = hitPoints;
		this.name = name;
		this.attackPoints = attackPoints;
		r = new Random();
	}


	public String getName() {
		return name;
	}

	public int getHitPoints() {
		return hitPoints;
	}

	public int getAttackPoints() {
		return attackPoints;
	}

	public void takeDamage(int damage) {
		hitPoints -= damage;
		if (hitPoints < 0) {
			hitPoints = 0;
		}
	}

	public int attack() {
		int myAttack = attackPoints + (-2 + r.nextInt(MAX));
		if (myAttack < 0) {
			return 0;
		} else {
			return myAttack;
		}
	}

	public int attackWithSword() {
		if (hasSword && r.nextInt(8) == r.nextInt(8)) {
			hasSword = false;
			System.out.println("You broke your sword! Don't try and attack with it!");
			System.out.println("You do normal damage for this turn.");
			return attackPoints + 5;
		}
		if (hasSword) {
			return attackPoints + 5;
		} else {
			System.out.println("You already broke your sword! Doing 0 damage!");
			return 0;
		}
	}

	public boolean isAlive() {
		return hitPoints > 0;
	}

}
