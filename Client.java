import java.io.*;
import java.net.*;
import java.util.*;

public class Client {

	public static boolean DEBUG = false;
	private static final int PORT = 1234;
	private static InetAddress IPADDRESS;
	private static DatagramSocket socket;
	private static Person player;
	private static boolean isEnemyAlive;
	private static int enemyHealth;

	public static void main(String[] args) throws Exception {
		// Get user information
		player = new Person("Beowulf", 100, 5);
		Scanner console = new Scanner(System.in);
		System.out.print("Server name: ");
		IPADDRESS = InetAddress.getByName(console.nextLine().trim());
		System.out.print("Your name: ");
		String name = console.nextLine().trim();
		// Trim down to prevent overflow.
		if (name.length() > 1000) {
			name = name.substring(0, 1000);
		}
		String input = "";
		// Create background listener.
		ListeningThread listener = new ListeningThread();
		listener.main(new String[0]);

		UpdateThread updater = new UpdateThread();
		updater.main(new String[0]);

		Thread.sleep(300);
		System.out.println();
		printIntro();
		// Get the socket to send through from our listening background thread.
		socket = ListeningThread.socket;
		Thread.sleep(200);
		// Register the name
		registerName(name);
		isEnemyAlive = true;
		while (isEnemyAlive) {
			playGame(console);
		}
		socket.close();

	}

	private static void printIntro() {
		System.out.println("Enter s to attack with sword, or h to attack with hands.");
		System.out.println("Your sword does " + (player.getAttackPoints() + 5) + " damage, and your hands do " + player.getAttackPoints() + " with a random factor from -2 to +6");
		System.out.println("Your sword also has a 12.5% chance of breaking.");

	}

	private static void playGame(Scanner console) throws Exception {
		boolean attackWithSword = false;
		if (!isEnemyAlive()) {
			endGame();
		}
		Thread.sleep(500);
		System.out.print("Input: ");
		String inputPhrase = console.nextLine().trim();
		if (inputPhrase.length() > 0) {
			if (inputPhrase.charAt(0) == 's') {
				attackWithSword = true;
			}
		}

		int damage;
		if (attackWithSword) {
			damage = player.attackWithSword();
		} else {
			damage = player.attack();
		}
		String weapon = (attackWithSword) ? "sword" : "hands";
		System.out.println("You attack with your " + weapon + " for " + damage);
		byte[] sendBuffer = ("0" + Integer.toString(damage)).getBytes();
		DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, IPADDRESS, PORT);
		Debug.print(DEBUG, "Sending packet...");
		socket.send(packet);
		Debug.print(DEBUG, "Packet sent.");
		Thread.sleep(100);
		System.out.println("Grendel's HP is now: " + enemyHealth);
		if (!isEnemyAlive()) {
			endGame();
		}
	}

	public static void requestHealth() throws IOException {
		byte[] sendBuffer = "4".getBytes();
		DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, IPADDRESS, PORT);
		socket.send(packet);
	}

	@Deprecated
	private static boolean isEnemyAlive() throws Exception {
		byte[] sendBuffer = "1".getBytes();
		DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, IPADDRESS, PORT);
		socket.send(packet);
		Thread.sleep(75);
		return isEnemyAlive;
	}

	private static void registerName(String name) throws Exception {
		byte[] sendBuffer = ("2" + name).getBytes();
		DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, IPADDRESS, PORT);
		Debug.print(DEBUG, "Registering your name with the server...");
		socket.send(packet);
	}

	private static void killUser() {
		byte[] sendBuffer = "3".getBytes();
		DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, IPADDRESS, PORT);
		Debug.print(DEBUG, "Sending death notification to server.");
		try {
			socket.send(packet);
		} catch (Exception e) {
			Debug.print(DEBUG, "Failed to send death notification.");
		}
	}


	public static void processPacket(DatagramPacket packet) {
		String result = new String(packet.getData()).trim();
		int packetType = 999;
		try {
			packetType = Integer.parseInt(Character.toString(result.charAt(0)));
		} catch (Exception e) {
			packetType = 999;
		}
		if (result.length() == 0) {
			return;
		}
		switch (packetType) {
			case 0:
				// Take damage
				int damage = Integer.parseInt(getData(packet));
				player.takeDamage(damage);
				
				System.out.println("You take " + damage + " damage!"); 
				System.out.println("You now have " + player.getHitPoints() + "HP");
				if (!player.isAlive()) {
					System.out.println("You've died!");
					killUser();
					socket.close();
					System.exit(0);
				}
				break;
			case 1:
				// Response to isAlive
				updateGrendel(packet);
				break;
			case 2:
				// Game is over broadcast
				endGame();
				break;
			case 3:
				// Response to prompt for alive.
				updateEnemyHealth(packet);
			default:
				// Unknown server->client packet
				Debug.print(DEBUG, "Don't know how to handle packet: " + result);
		}
	}

	private static void updateEnemyHealth(DatagramPacket packet) {
		enemyHealth = Integer.parseInt(getData(packet));
		if (enemyHealth <= 0) {
			isEnemyAlive = false;
		}
		//System.out.println("Grendel's HP is now: " + enemyHealth);
		
	}

	private static void endGame() {
		System.out.println("Grendel has died!");
		System.exit(0);
	}

	private static String getData(DatagramPacket packet) {
		String data = new String(packet.getData());
		return data.substring(1).trim();
	}

	@Deprecated
	private static void updateGrendel(DatagramPacket packet) {
		String data = getData(packet);
		isEnemyAlive = data.contains("1");
	}
}

class ListeningThread implements Runnable {
	public static boolean DEBUG = Client.DEBUG;
	public static DatagramSocket socket;
	public void run() {
		Debug.print(DEBUG, "[listener] Starting listening thread...");
		try {
			socket = new DatagramSocket();
		} catch (Exception e) {
			Debug.print(DEBUG, "[listener] creating socket failed.");
		}
		byte[] recBuffer;
		while (true) {
			recBuffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(recBuffer, recBuffer.length);
			try {
				socket.receive(packet);
			} catch (Exception e) {
				Debug.print(DEBUG, "[listener] receiving on socket failed.");
			}
			String packetContents = new String(packet.getData());
			Client.processPacket(packet);
		}
	}

	public static void main(String[] args) {
		Debug.print(DEBUG, "[listener] Spawning thread...");
		(new Thread(new ListeningThread())).start();
	}
}

class UpdateThread implements Runnable {
	public void run() {
		while (true) {
			try {
				Client.requestHealth();
				Thread.sleep(500);
			} catch (Exception e) {

			}
		}
	}

	public static void main(String[] args) {
		Debug.print(Client.DEBUG, "[updater] Spawning thread...");
		(new Thread(new UpdateThread())).start();
	}
}
