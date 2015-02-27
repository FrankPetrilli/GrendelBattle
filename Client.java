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
		Thread.sleep(300);
		System.out.println();
		// Get the socket to send through from our listening background thread.
		socket = ListeningThread.socket;
		Thread.sleep(200);
		// Register the name
		registerName(name);
		isEnemyAlive = true;
		while (isEnemyAlive) {
			isEnemyAlive();
			Thread.sleep(300);
			System.out.print("Input: ");
			input = console.nextLine().trim();
			byte[] sendBuffer = input.getBytes();
			byte[] recBuffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, IPADDRESS, PORT);
			Debug.print(DEBUG, "Sending packet...");
			socket.send(packet);
			Debug.print(DEBUG, "Packet sent.");
			isEnemyAlive();
		}
		socket.close();

	}

	private static boolean isEnemyAlive() throws Exception {
		byte[] sendBuffer = "1".getBytes();
		DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, IPADDRESS, PORT);
		//System.out.println("Sending request for isAlive");
		socket.send(packet);
		Thread.sleep(75);
		//System.out.println("Enemy Alive: " + isEnemyAlive);
		return isEnemyAlive;
	}

	private static void registerName(String name) throws Exception {
		byte[] sendBuffer = ("2" + name).getBytes();
		DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, IPADDRESS, PORT);
		Debug.print(DEBUG, "Registering your name with the server...");
		socket.send(packet);
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
				int damage = Integer.parseInt(getData(packet));
				player.takeDamage(damage);
				System.out.println("You take " + damage + " damage!"); 
				System.out.println("You now have " + player.getHitPoints() + "HP");
				break;
			case 1:
				updateGrendel(packet);
				break;
			case 2:
				endGame();
				break;
			default:
				Debug.print(DEBUG, "Don't know how to handle packet: " + result);
		}
	}

	private static void endGame() {
		System.out.println("Grendel has died!");
		System.exit(0);
	}

	private static String getData(DatagramPacket packet) {
		String data = new String(packet.getData());
		return data.substring(1).trim();
	}

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
			//System.out.println("Received packet: " + packetContents);
			Client.processPacket(packet);
			// TODO: Kill the parent thread.
		}
	}

	public static void main(String[] args) {
		Debug.print(DEBUG, "[listener] Spawning thread...");
		(new Thread(new ListeningThread())).start();
	}
}
