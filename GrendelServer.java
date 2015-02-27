import java.io.*;
import java.net.*;
import java.util.*;

public class GrendelServer {

	public static boolean DEBUG = true;
	private static final int PORT = 1234;
	private static DatagramSocket socket;
	private static Person grendel;
	private static Map<InetAddress, String> users;
	private static Map<InetAddress, Integer> connectedPorts;
	private static Set<InetAddress> connectedHosts;
	private static Set<InetAddress> deadUsers;

	public static void main(String[] args) throws Exception {
		byte[] buffer;
		socket = new DatagramSocket(PORT);
		DatagramPacket packet;

		grendel = new Person("grendel", 100, 5);

		Debug.print(DEBUG, "::Server started::");
		Debug.print(DEBUG, "::Listening on " + PORT + "::");

		connectedHosts = new HashSet<InetAddress>();
		deadUsers = new HashSet<InetAddress>();
		users = new HashMap<InetAddress, String>();
		connectedPorts = new HashMap<InetAddress, Integer>();

		while (grendel.isAlive()) {
		//while (true) {
 			buffer = new byte[1024];
 			packet = new DatagramPacket(buffer, buffer.length);
			Debug.print(DEBUG, "Waiting for input...");
			socket.receive(packet);
			if (!connectedHosts.contains(packet.getAddress())) {
				Debug.print(DEBUG, "New connection from: " + packet.getAddress() + " on port " + packet.getPort());
				connectedHosts.add(packet.getAddress());
			}
			// Add latest port from user.
			connectedPorts.put(packet.getAddress(), packet.getPort());
			if (deadUsers.contains(packet.getAddress())) {
				System.out.println(getName(packet.getAddress()) + ", you've already been killed. You may not join the fight again.");
				continue;
			}
			Debug.print(DEBUG, "Packet received");

			int type = packetType(packet);
			System.out.println("Type is: " + type);
			if (type == 0) {
				damage(packet);
			} else if (type == 1) {
				status(packet);
			} else if (type == 2) {
				register(packet);
			} else if (type == 3) {
				killUser(packet);
			}

		}
		informDead(connectedHosts);
	}

	private static void emptyReply(DatagramPacket packet) throws IOException {
		byte[] outBuffer = new byte[0];
		packet.setData(outBuffer);
		packet.setLength(outBuffer.length);
		socket.send(packet);
	}


	private static void killUser(DatagramPacket packet) throws IOException {
		connectedHosts.remove(packet.getAddress());
		emptyReply(packet);
		System.out.println(getName(packet.getAddress()) + " was killed by Grendel!");
	}

	private static String getName(InetAddress address) {
		return users.get(address);
	}


	private static void informDead(Set<InetAddress> connectedHosts) throws IOException {
		for (InetAddress address : connectedHosts) {
			System.out.println("informing " + getName(address) + " of death");
			//byte[] outBuffer = "informDeath".getBytes();
			byte[] outBuffer = "2".getBytes();
			DatagramPacket packet = new DatagramPacket(outBuffer, outBuffer.length);
			packet.setAddress(address);
			packet.setPort(connectedPorts.get(address));
			socket.send(packet);
		}
	}

	private static void damage(DatagramPacket packet) throws IOException {
		// Calculate from packet
		int input = Integer.parseInt(getData(packet));
		grendel.takeDamage(input);
		System.out.println("Grendel takes " + input + " damage from " + getName(packet.getAddress()) + ". He's at: " + grendel.getHitPoints());
		if (!grendel.isAlive()) {
			emptyReply(packet);
			return;
		}
		// Prepare return attack
		String damageDone = new String("0" + Integer.toString(grendel.attack()));
		// Send
		byte[] outBuffer = damageDone.getBytes();
		packet.setData(outBuffer);
		packet.setLength(outBuffer.length);
		socket.send(packet);
		// Print
		System.out.println("Grendel returns " + damageDone + " damage to " + packet.getAddress());
	}

	private static void status(DatagramPacket packet) throws IOException {
		byte[] outBuffer;
		if (grendel.isAlive()) {
			outBuffer = "11".getBytes();
		} else {
			outBuffer = "10".getBytes();
		}
		packet.setData(outBuffer);
		packet.setLength(outBuffer.length);
		socket.send(packet);
	}

	private static void register(DatagramPacket packet) throws IOException {
		InetAddress user = packet.getAddress();
		if (!users.containsKey(user)) {
			Debug.print(DEBUG, "Registering " + user + " as " + getData(packet));
			users.put(user, getData(packet));
			Debug.print(DEBUG, "User registered.");
		}
		emptyReply(packet);
	}

	private static int packetType(DatagramPacket packet) {
		String data = new String(packet.getData());
		return Integer.parseInt(Character.toString(data.charAt(0)));
	}

	private static String getData(DatagramPacket packet) {
		String data = new String(packet.getData());
		return data.substring(1).trim();
	}
}
