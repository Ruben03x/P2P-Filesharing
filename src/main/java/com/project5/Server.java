package com.project5;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Scanner;

import javax.crypto.Cipher;

public class Server {

	private ServerSocket serverSocket;

	/**
	 * Constructor for the server
	 * 
	 * @param serverSocket The server socket
	 */
	public Server(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	/**
	 * Main method to start the server
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		ServerSocket serverSocket;
		Scanner input = new Scanner(System.in);
		System.out.print("Enter a port number: ");

		// Unsures valid and available port is entered
		while (true) {
			// Get the port number
			String stringPort = input.nextLine();
			try {
				// Try to create a server socket
				int port = Integer.parseInt(stringPort);
				serverSocket = new ServerSocket(port);
				break;
			} catch (Exception e) {
				System.out.println("Port " + stringPort + " not usable, Enter another:");
			}
		}
		input.close();
		// Start the server
		Server server = new Server(serverSocket);
		server.startServerSocket();
	}

	/**
	 * Starts the server socket and listens for incoming connections
	 */
	public void startServerSocket() {
		// Start the server
		try {
			System.out.println("Server is running");
			while (!serverSocket.isClosed()) {
				// Accept incoming connections
				Socket clientSocket = serverSocket.accept();
				// Create a new client manager
				ClientManager client_ = new ClientManager(clientSocket);
				// Start a new thread for the client
				Thread newThread = new Thread(client_);
				newThread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

/**
 * Represents a client manager that handles communication with a client.
 */
class ClientManager implements Runnable {

	public static ArrayList<ClientManager> clients = new ArrayList<>(); // list of client managers
	public static ArrayList<String> usernames = new ArrayList<>(); // list of client usernames
	private volatile KeyPair keyPair;
	private volatile PublicKey clientKey;
	private Socket clientSocket; // the current client's socket
	private volatile ObjectInputStream objectInputStream;
	private volatile ObjectOutputStream objectOutputStream;
	private String username; // a username
	public volatile OutputStream out; // An output stream

	/**
	 * Represents a client manager that handles communication with a client.
	 * 
	 * @param clientSocket The socket associated with the client.
	 * @param logListView  The ListView to display log messages.
	 * @param userListView The ListView to display connected users.
	 */
	public ClientManager(Socket clientSocket) {
		try {
			this.clientSocket = clientSocket;
			out = clientSocket.getOutputStream(); // the client output stream
			objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
			clientKey = (PublicKey) objectInputStream.readObject();
			System.out.println("Received Public Key");

			keyPair = generateKeyPair();
			objectOutputStream = new ObjectOutputStream(out);
			objectOutputStream.writeObject(keyPair.getPublic());
			System.out.println("Sent PublicKey");

			System.out.println("A new client is requesting to connect!");
			// Get the username
			while (true) {
				// Read the encrypted data
				byte[] encryptedData = (byte[]) objectInputStream.readObject();
				username = new String(decryptWithPrivateKey(encryptedData)); // read username

				// ensures client connecting has a unique username
				if (usernames.contains(username)) {
					sendMessage("##USERNAMETAKEN"); // communicates that username taken
				} else {
					sendMessage("##USERNAMEOK");
					System.out.println(username + " connected successfully"); // communicates that username is OK
					clients.add(this);
					usernames.add(username); // add client details to list

					break;

				}
			}

		} catch (Exception e) {
			System.out.println("Error initialising client");
		}
	}

	/**
	 * Run method for the client manager
	 */
	@Override
	public void run() {
		String msg;
		// Listen for incoming messages
		try {
			// Listen for incoming messages
			while (!clientSocket.isClosed()) {
				byte[] messageBytes = (byte[]) objectInputStream.readObject();
				msg = new String(decryptWithPrivateKey(messageBytes));
				// System.out.println(msg);
				if (msg != null && msg.equals("##DISCONNECT")) {
					closeAllStreams();
				}
				// Handle search requests
				if (msg.startsWith("##SEARCH")) {
					sendSearch(msg);
				}
				// Handle search results
				if (msg.startsWith("##RESULT")) {
					handleSearchResults(msg);
				}
				// Handle download requests
				if (msg.startsWith("##DOWNLOAD")) {
					handleDownloadRequest(msg);
				}
			}
		} catch (Exception e) {
			closeAllStreams();
		}
	}

	/**
	 * Sends a search request to all clients
	 * 
	 * @param message The message to send
	 */
	private void sendSearch(String message) {
		// Get the search text
		String[] parts = message.split(",", 2);
		String searchText = parts[1];
		System.out.println(username + " is is searching: " + searchText);
		// Send the search request to all clients
		for (ClientManager client_ : clients) {
			// Ensure the client is not the current client
			if (!client_.username.equals(username)) {
				// Send the search request
				client_.sendMessage("##SEARCH," + username + "," + searchText);
			}
		}
	}

	/**
	 * Handles search results
	 * 
	 * @param message The message to handle
	 */
	private void handleSearchResults(String message) {
		// Get the search results
		String[] parts = message.split(",", 3);
		// Send the search results to the client
		ClientManager client_ = findClientByUsername(parts[1]);
		// Send the search results
		client_.sendMessage("##RESULT," + username + "," + parts[2]);
	}

	/**
	 * Handles a download request
	 * 
	 * @param message The message to handle
	 */
	private void handleDownloadRequest(String message) {
		// Get the download request details
		String[] parts = message.split(",", 6);
		String uploader = parts[1];
		String fileName = parts[2];
		String downloaderAddress = parts[3];
		String downloaderPort = parts[4];
		String messageKey = parts[5];
		// Send the download request to the uploader
		System.out.println(username + " requested to download " + fileName);
		ClientManager client_ = findClientByUsername(uploader);
		client_.sendMessage(
				"##DOWNLOAD," + fileName + "," + downloaderAddress + "," + downloaderPort + "," + messageKey);
	}

	/**
	 * Finds a client by username
	 * 
	 * @param username The username to search for
	 * @return The client manager
	 */
	private ClientManager findClientByUsername(String username) {
		// Find the client by username
		for (ClientManager client : clients) {
			// Check if the client's username matches
			if (client.username.equals(username)) {
				return client;
			}
		}
		return null;
	}

	/**
	 * Sends a message to the client
	 * 
	 * @param message The message to send
	 */
	public void sendMessage(String message) {
		// Send the message
		try {
			// Encrypt the message
			byte[] encryptedData = encryptWithPublicKey(message.getBytes());
			objectOutputStream.writeObject(encryptedData);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Closes all streams
	 */
	public void closeAllStreams() {
		// Close all streams
		System.out.println(username + " disconnected");
		// Remove the client
		clients.remove(this);
		// Remove the username
		usernames.remove(username);
		try {

			if (objectInputStream != null)
				objectInputStream.close();
			if (objectOutputStream != null)
				objectOutputStream.close();
			if (clientSocket != null)
				clientSocket.close();
		} catch (IOException e) {

		}
	}

	/**
	 * Generates a key pair
	 * 
	 * @return The key pair
	 * @throws NoSuchAlgorithmException
	 */
	public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		// Generate a key pair
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		// Initialize the key pair generator
		keyPairGenerator.initialize(2048); // Key size
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Encrypts data with the public key
	 * 
	 * @param data The data to encrypt
	 * @return The encrypted data
	 * @throws Exception
	 */
	public byte[] encryptWithPublicKey(byte[] data) throws Exception {
		// Encrypt the data
		Cipher cipher = Cipher.getInstance("RSA");
		// Initialize the cipher
		cipher.init(Cipher.ENCRYPT_MODE, clientKey);
		return cipher.doFinal(data);
	}

	/**
	 * Decrypts data with the private key
	 * 
	 * @param encryptedData The data to decrypt
	 * @return The decrypted data
	 * @throws Exception
	 */
	public byte[] decryptWithPrivateKey(byte[] encryptedData) throws Exception {
		// Decrypt the data
		Cipher cipher = Cipher.getInstance("RSA");
		// Initialize the cipher
		cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
		return cipher.doFinal(encryptedData);
	}
}
