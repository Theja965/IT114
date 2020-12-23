package server;

import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;
import java.util.Scanner;

import server.Room;
import core.Countdown;
import core.Helpers;
import server.ServerThread;

public class Room implements AutoCloseable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2396927244891036163L;
	private static SocketServer server;// used to refer to accessible server functions
	private String name;
	private final static Logger log = Logger.getLogger(Room.class.getName());

	// Commands
	private final static String COMMAND_TRIGGER = "/";
	private final static String CREATE_ROOM = "createroom";
	private final static String JOIN_ROOM = "joinroom";
	private final static String ROLL = "roll";
	private final static String FLIP = "flip";
	private List<ServerThread> clients = new ArrayList<ServerThread>();
	static Dimension gameAreaSize = new Dimension(800, 800);

	public Room(String name, boolean delayStart) {
		this.name = name;
	}

	public Room(String name) {
		this.name = name;
		// set this for BaseGamePanel to NOT draw since it's server-side
	}

	public static void setServer(SocketServer server) {
		Room.server = server;
	}

	public String getName() {
		return name;
	}

	@SuppressWarnings("null")
	protected synchronized void addClient(ServerThread client) {
		client.setCurrentRoom(this);
		boolean exists = false;
		// since we updated to a different List type, we'll need to loop through to find
		// the client to check against
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			if (c == client) {
				exists = true;
				if (client == null) {
					log.log(Level.WARNING, "Client " + client.getClientName() + " player was null, creating");
					syncClient(c);

				}
				break;
			}
		}
		if (exists) {
			log.log(Level.INFO, "Attempting to add a client that already exists");
		} else {
			// create a player reference for this client
			// so server can determine position
			// add Player and Client reference to ClientPlayer object reference
			clients.add(client);
			syncClient(client);
			readFromFile(client.getClientName(), client);
			// this is a "merged" list of Clients (ServerThread) and Players (Player)
			// objects
			// that's so we don't have to keep track of the same client in two different
			// list locations

		}

	}
	public void readFromFile(String fileName, ServerThread client) {
		try (Scanner reader = new Scanner(fileName)) {
			File file = new File(fileName);
			String fullText = "";
			while (reader.hasNextLine()) {
				String nl = reader.nextLine();
				System.out.println("Next line: " + nl);
				fullText += nl;
				// Scanner.nextLine() returns the line but excludes the line separator
				// so just append it back so it'll show correctly in the console
				if (reader.hasNextLine()) {// just a check to not append an extra line ending at the end
					fullText += System.lineSeparator();
				}
			}
			System.out.println("Contents of " + fileName + ": ");
			System.out.println(fullText);
			List<String>myList = new ArrayList<String>(Arrays.asList(fullText.split(","))); 
			client.mutedClients = myList;	
		}
		catch(Exception e) {
			
		}
	}

	public void clientPersist(ServerThread client, String fileName){
		String myString= client.mutedClients.toString().replaceAll("\\[|\\]|,","");
		try (FileWriter fw = new FileWriter(fileName);) {
			fw.write(System.lineSeparator());
			fw.write(myString);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private synchronized void syncClient(ServerThread client) {
		if (client.getClientName() != null) {
			client.sendClearList();
			sendConnectionStatus(client, true, "joined the room" + getName());
			updateClientList(client);

		}
	}

	private synchronized void updateClientList(ServerThread client) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			if (c != client) {
				client.sendConnectionStatus(c.getClientName(), true, null);
			}
		}

	}

	protected synchronized void removeClient(ServerThread client) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			if (c == client) {
				iter.remove();
				log.log(Level.INFO, "Removed client " + client.getClientName() + " from " + getName());
			}
		}
		if (clients.size() > 0) {
			sendConnectionStatus(client, false, "left the room " + getName());
		} else {
			cleanupEmptyRoom();
		}
	}

	private void cleanupEmptyRoom() {
		// If name is null it's already been closed. And don't close the Lobby
		if (name == null || name.equalsIgnoreCase(SocketServer.LOBBY)) {
			return;
		}
		try {
			log.log(Level.INFO, "Closing empty room: " + name);
			close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void joinRoom(String room, ServerThread client) {
		server.joinRoom(room, client);
	}

	protected void joinLobby(ServerThread client) {
		server.joinLobby(client);
	}

	protected void createRoom(String room, ServerThread client) {
		if (server.createNewRoom(room)) {
			sendMessage(client, "Created a new room");
			joinRoom(room, client);
		}
	}

	private String processCommands(String message, ServerThread client) {
		String response = null;
		try {
			if (message.indexOf(COMMAND_TRIGGER) > -1) {
				String[] comm = message.split(COMMAND_TRIGGER);
				log.log(Level.INFO, message);
				String part1 = comm[1];
				String[] comm2 = part1.split(" ");
				String command = comm2[0];
				if (command != null) {
					command = command.toLowerCase();
				}
				String roomName;
				ServerThread cp = null;
				switch (command) {
				case FLIP:
					Random randomNum = new Random();
					int result;
					result = randomNum.nextInt(2);
					if (result == 0) {
						response = "<i>You flipped heads!</i>";
					} else {
						response = "<i>You flipped tails!</i>";
					}
					break;
				case ROLL:
					Random rand = new Random();
					int randNum1 = rand.nextInt(10);
					response = "<i>You got the number</i>" + " " + randNum1;
					break;
				case CREATE_ROOM:
					roomName = comm2[1];
					createRoom(roomName, client);
					break;
				case JOIN_ROOM:
					roomName = comm2[1];
					joinRoom(roomName, client);
					break;
				case "mute":
					String mutedDude = comm2[1];
					client.mute(mutedDude);
					clientPersist(client, client.getClientName());
					sendPrivateMessage(client, Arrays.asList(mutedDude), client.getClientName() + " has muted " + mutedDude);					
					break;
				case "unmute":
					String unmutedDude = comm2[1];
					client.unmute(unmutedDude);
					clientPersist(client, client.getClientName());
					sendPrivateMessage(client, Arrays.asList(unmutedDude), client.getClientName() + " has unmuted you " + unmutedDude);					
					break;
				case "pm":
					// TODO extract clients from message, save to array with
					String clientName = comm2[1];
					clientName = clientName.trim().toLowerCase();
					List<String> clients = new ArrayList<String>();
					clients.add(clientName);
					sendPrivateMessage(client, clients, message);
					response = null;
					break;
				default:
					// not a command, let's fix this function from eating messages
					response = message;
					break;
				}
			} else {
				// not a command, let's fix this function from eating messages
				// response = message;
				String alteredMessage = message;

				if (alteredMessage.indexOf("@@") > -1) {
					String[] s1 = alteredMessage.split("@@");
					String m = "";
					// m += s1[0];
					for (int i = 0; i < s1.length; i++) {
						if (i % 2 == 0) {
							m += s1[i];
						} else {
							m += "<b>" + s1[i] + "</b>";
						}
						System.out.println(s1[i]);
					}
					// m += s1[s1.length - 1];
					alteredMessage = m;
				}
				if (alteredMessage.indexOf("##") > -1) {
					String[] s1 = alteredMessage.split("##");
					String m = "";
					// m += s1[0];
					for (int i = 0; i < s1.length; i++) {
						if (i % 2 == 0) {
							m += s1[i];
						} else {
							m += "<i>" + s1[i] + "</i>";
						}
						System.out.println(s1[i]);

						}
						alteredMessage = m;
				}	
				if (alteredMessage.indexOf("und") > -1) {
					String[] s1 = alteredMessage.split("und");
					String m = "";
					// m += s1[0];
					for (int i = 0; i < s1.length; i++) {
						if (i % 2 == 0) {
							m += s1[i];
						} else {
							m += "<u>" + s1[i] + "</u>";
						}
						System.out.println(s1[i]);

						}
						alteredMessage = m;
				}
					response = alteredMessage;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	protected void sendConnectionStatus(ServerThread client, boolean isConnect, String message) {
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread c = iter.next();
			boolean messageSent = c.sendConnectionStatus(client.getClientName(), isConnect, message);
			if (!messageSent) {
				iter.remove();
			}
		}
	}

	/***
	 * Takes a sender and a message and broadcasts the message to all clients in
	 * this room. Client is mostly passed for command purposes but we can also use
	 * it to extract other client info.
	 * 
	 * @param sender  The client sending the message
	 * @param message The message to broadcast inside the room
	 */
	protected void sendMessage(ServerThread sender, String message) {
		log.log(Level.INFO, getName() + ": Sending message to " + clients.size() + " clients");
		String resp = processCommands(message, sender);
		if (resp == null) {

			// it was a command, don't broadcast
			return;
		}
		message = resp;
		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (!client.isMuted(sender.getClientName())) {
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
				}
			}
		}
	}

	protected void sendPrivateMessage(ServerThread sender, List<String> dest, String message) {

		Iterator<ServerThread> iter = clients.iterator();
		while (iter.hasNext()) {
			ServerThread client = iter.next();
			if (dest.contains(client.getClientName().toLowerCase())) {
				boolean messageSent = client.send(sender.getClientName(), message);
				if (!messageSent) {
					iter.remove();
				}
				break;
			}

		}
	}

	/**
	 * Broadcasts this client/player direction to all connected clients/players
	 * 
	 * @param sender
	 * @param dir
	 */
	/**
	 * Broadcasts this client/player position to all connected clients/players
	 * 
	 * @param sender
	 * @param pos
	 */
	public List<String> getRooms(String search) {
		return server.getRooms(search);
	}

	/***
	 * Will attempt to migrate any remaining clients to the Lobby room. Will then
	 * set references to null and should be eligible for garbage collection
	 */
	@Override
	public void close() throws Exception {
		int clientCount = clients.size();
		if (clientCount > 0) {
			log.log(Level.INFO, "Migrating " + clients.size() + " to Lobby");
			Iterator<ServerThread> iter = clients.iterator();
			Room lobby = server.getLobby();
			while (iter.hasNext()) {
				ServerThread client = iter.next();
				lobby.addClient(client);
				iter.remove();
			}
			log.log(Level.INFO, "Done Migrating " + clients.size() + " to Lobby");
		}
	}
}
