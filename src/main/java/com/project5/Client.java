package com.project5;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.Cipher;
import javafx.application.Platform;

/**
 * This class is used to create a client object
 */
public class Client {

	public volatile ArrayList<String> resultList = new ArrayList<>(); // list of files
	public volatile HashMap<String, String> resultMap = new HashMap<>();
	private volatile Socket socket = null; // the client socket
	private volatile ObjectInputStream objectInputStream;
	private volatile ObjectOutputStream objectOutputStream;
	private InteractController interactController; // controls interacts between the user and UI
	public Boolean checkedUsername = false; // has client username been checked against others
	public Boolean usernameOK = false; // is the client username valid
	private volatile KeyPair keyPair;
	private volatile PublicKey serverKey;

	/**
	 * This is the constructor for the Client class
	 * @param socket
	 * @param interactController
	 */
	public Client(Socket socket, InteractController interactController) {
		try {

			startDownloadServer();

			// initialize instance variables
			this.socket = socket;
			this.interactController = interactController;

			// initialize the input and output streams
			keyPair = generateKeyPair();
			objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
			objectOutputStream.writeObject(keyPair.getPublic());
			System.out.println("Sent Public Key");

			// receive the server's public key
			objectInputStream = new ObjectInputStream(socket.getInputStream());
			serverKey = (PublicKey) objectInputStream.readObject();

			System.out.println("Received Public Key");
			System.out.println();

			// Create a File object representing the directory
			File directory = new File("localFiles");
			// Check if the directory exists
			if (!directory.exists()) {
				// Create the directory
				directory.mkdirs();
			}
		} catch (Exception e) {
			e.printStackTrace();
			closeAllSreams();
		}

	}

	/**
	 * This method is used to send a message to the server
	 * @param message
	 */
	public void sendMessage(String message) {
		try {
			// encrypt the message with the server's public key
			byte[] encryptedData = encryptWithPublicKey(message.getBytes());
			objectOutputStream.writeObject(encryptedData);
		} catch (Exception e) {
			e.printStackTrace();
			closeAllSreams();
		}
	}

	/**
	 * This method is used to receive a message from the server
	 */
	public void receiver() {
		new Thread(new Runnable() {

			@Override
			public void run() {

				try {
					// message from server
					String msg;
					// while the socket is connected
					while (socket.isConnected()) {
						// read the message from the server
						byte[] messageBytes = (byte[]) objectInputStream.readObject();
						msg = new String(decryptWithPrivateKey(messageBytes));
						System.out.println(msg);
						// if the message is null or disconnect message, close all streams
						if (msg == null || msg.equals("##DISCONNECT")) { // close streams upon disconnect from server
							closeAllSreams();
							break;
						}
						// if the message is a username taken message, show error message
						if (msg.equals("##USERNAMETAKEN")) {
							checkedUsername = true; // username checked
							Platform.runLater(() -> interactController
									.showErrorDialog("Username is taken. Please try a different username.")); // error
																												// message
																												// if
																												// username
																												// taken
						}
						if (msg.equals("##USERNAMEOK")) {
							checkedUsername = true; // username is checked
							usernameOK = true; // username is not taken
						}
						if (msg.startsWith("##SEARCH")) {
							searchForFiles(msg); // add user to list of clients
						}
						if (msg.startsWith("##RESULT")) {
							handleResults(msg); // add user to list of clients
						}
						if (msg.startsWith("##DOWNLOAD")) {
							handleDownloadRequest(msg); // add user to list of clients
						}
					}
				} catch (Exception e) {
					closeAllSreams();
				}
			}
		}).start();
	}

	/**
	 * This method is used to search for files in the localFiles directory
	 * 
	 * @param message
	 */
	private void searchForFiles(String message) {
		// split the message into parts
		String[] parts = message.split(",", 3);
		String searcher = parts[1];
		String searchText = parts[2];
		// convert the search text to lower case
		searchText = searchText.toLowerCase();
		File directory = new File("localFiles");
		File[] fileList = directory.listFiles();

		// loop through the files in the directory
		for (File file : fileList) {
			String fileName = file.getName();
			fileName = fileName.toLowerCase();
			// if the file name contains the search text, send the result to the server
			if (fileName.contains(searchText)) {
				String searchResult = file.getName();
				sendMessage("##RESULT," + searcher + "," + searchResult);
			}
		}

	}

	/**
	 * This method is used to handle the results of the search
	 * 
	 * @param message
	 */
	private void handleResults(String message) {
		// split the message into parts
		String[] parts = message.split(",", 3);
		String result = parts[2];
		// if the result is not already in the list, add it to the list
		if (!resultList.contains(result)) {
			resultList.add(result);
			resultMap.put(result, parts[1]);
		}
		// update the file list
		interactController.updateFileList(resultList);
	}

	/**
	 * This method is used to handle the download request
	 * 
	 * @param message
	 */
	private void handleDownloadRequest(String message) {
		// split the message into parts
		String[] parts = message.split(",", 5);
		String fileName = parts[1];
		String downloaderAddress = parts[2];
		// convert the downloader port to an integer
		int downloaderPort = Integer.parseInt(parts[3]);
		String messageKey = parts[4];
		// upload the file
		uploadFile(fileName, downloaderAddress, downloaderPort, messageKey);
	}

	/**
	 * This method is used to disconnect the client from the server
	 */
	public void disconnect() {
		try {
			// close the object output stream
			if (objectOutputStream != null) {
				sendMessage("##DISCONNECT");
			} // send disconnect message to server
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeAllSreams();
		}
	}

	/**
	 * This method is used to close all streams
	 */
	public void closeAllSreams() {
		System.out.println("Server disconnected");
		// close the object input stream
		try {
			if (objectInputStream != null)
				objectInputStream.close();
			if (objectOutputStream != null)
				objectOutputStream.close();
			if (socket != null)
				socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * This method is used to generate a message key
	 * 
	 * @return
	 */
	private String generateMessageKey() {
		// hexadecimal values
		String[] hexadecimal = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
		String messageKey = "";
		// loop through 10 times
		for (int i = 0; i < 10; i++) {
			Random random = new Random();
			// create random MAC address from hex values
			messageKey = messageKey + hexadecimal[random.nextInt(16)];
		}
		return messageKey;
	}

	// === Downloading and Uploading code from here ===

	public volatile ServerSocket serverSocket;
	public volatile int downloadPort;
	public volatile Boolean pauseDownload = false;
	public volatile String messageKey;
	public volatile String downloadingFile;

	/**
	 * This method is used to start the download server
	 */
	public void startDownloadServer() {
		downloadPort = 5000;
		// loop until a port is found
		while (true) {
			try {
				serverSocket = new ServerSocket(downloadPort);
				break;
			} catch (Exception e) {
				downloadPort++;
			}
		}
		System.out.println("Download server started on port: " + downloadPort);
	}

	/**
	 * This method is used to start the download process
	 * @param selectedFile
	 */
	public void startDownloader(String selectedFile) {
		// Generate a message key
		messageKey = generateMessageKey();
		downloadingFile = selectedFile;
		System.out.println("Generated key: " + messageKey);
		downloadFile();

		// Code that gets the hamachi address
		String hamachiAddress = socket.getLocalAddress().getHostAddress();

		System.out.println("Hamachi Address: " + hamachiAddress);

		// Send the download request to the server
		String message = "##DOWNLOAD," + resultMap.get(selectedFile) + "," + selectedFile + ","
				+ hamachiAddress + "," + downloadPort + "," + messageKey;
		sendMessage(message);

	}

	
	/**
	 * This method is used to download the file
	 */
	private void downloadFile() {
		// Start a new thread to download the file
		new Thread(() -> {
			while (true) {
				try {
					// Accept the connection from the downloader
					Socket downloadSocket = serverSocket.accept();
					System.out.println("Accepted connection from " + downloadSocket.getRemoteSocketAddress());

					try (DataInputStream input = new DataInputStream(downloadSocket.getInputStream())) {
						// Read the file name and size
						// String fileName = input.readUTF();
						long fileSize = input.readLong();
						String messageKeyReceived = input.readUTF();

						System.out.println("Received key: " + messageKeyReceived);

						// Check if the message key is correct
						if (messageKey.equals(messageKeyReceived)) {
							String receiving = "Downloading file: " + downloadingFile + " with " + fileSize + " bytes";
							System.out.println(receiving);
							try {
								// Create a file output stream
								FileOutputStream fileOutputStream = new FileOutputStream(
										"localFiles/" + downloadingFile);
								// Receive the file contents in chunks
								byte[] buff = new byte[4096];
								int amountRead;
								long totalRead = 0;

								// Update the progress bar
								while ((amountRead = input.read(buff)) > 0) {

									fileOutputStream.write(buff, 0, amountRead);
									totalRead += amountRead;
									interactController.updateProgressBarDownload(totalRead, fileSize);
									if (totalRead == fileSize) {
										break;
									}
									while (pauseDownload) {
										Thread.sleep(50);
									}
								}
								// Close the file output stream
								fileOutputStream.close();
								// client.pasueButton.setDisable(true);
								interactController.updateProgressBarDownload(fileSize, fileSize);
								System.out.println("File downloaded: " + downloadingFile);
							} catch (Exception e) {
								System.out.println("Failed to download file contents: ");
								e.printStackTrace();
							}
							// Enable the download button
							interactController.buttonDownload.setDisable(false);
							interactController.showDialog("Download Complete: " + downloadingFile);

						} else {
							System.out.println(
									"Incorrect message-key!!: " + messageKeyReceived + " , expected: " + messageKey);
						}

					}

				} catch (SocketException e) {
					System.out.println("Error Downloading File.");
				} catch (IOException e) {
					System.out.println("Error occured while downloading File.");
				}
			}
		}).start();

	}

	/**
	 * This method is used to upload the file
	 * @param fileName
	 * @param downloaderAddress
	 * @param downloaderPort
	 * @param messageKey
	 */
	public void uploadFile(String fileName, String downloaderAddress, int downloaderPort, String messageKey) {

		new Thread(() -> {
			try {
				// Open a socket connection to the receiver
				Socket uploadSocket = new Socket(downloaderAddress, downloaderPort);
				DataOutputStream output = new DataOutputStream(uploadSocket.getOutputStream());

				File file = new File("localFiles/" + fileName);

				// Send file size and message-key
				output.writeLong(file.length());
				output.writeUTF(messageKey);

				// Send file contents
				FileInputStream fileInputStream = new FileInputStream(file);
				byte[] buff = new byte[4096];
				int readAmount;
				long totalRead = 0;
				long fileLength = file.length();

				// Update the progress bar
				while ((readAmount = fileInputStream.read(buff)) > 0) {

					output.write(buff, 0, readAmount);
					totalRead += readAmount;
					// Update the progress of the file
					interactController.updateProgressBarUpload(totalRead, fileLength);
				}

				// Close the input and output streams and the socket
				fileInputStream.close();
				output.close();
				uploadSocket.close();

				// Remove the file from the list view
				// Update status label
				System.out.println("File uploaded successfully.\n");

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Error while uploading.\n");
			}
		}).start();

	}

	// === RSA Encryption and Decryption code from here ===

	/**
	 * This method is used to generate a key pair
	 * @return
	 * @throws NoSuchAlgorithmException
	 */
	public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		// Generate a key pair
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		// Initialize the key pair generator
		keyPairGenerator.initialize(2048); // Key size
		// Return the key pair
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * This method is used to encrypt data with the public key
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public byte[] encryptWithPublicKey(byte[] data) throws Exception {
		// Create a cipher object
		Cipher cipher = Cipher.getInstance("RSA");
		// Initialize the cipher object
		cipher.init(Cipher.ENCRYPT_MODE, serverKey);
		return cipher.doFinal(data);
	}

	/**
	 * This method is used to decrypt data with the private key
	 * @param encryptedData
	 * @return
	 * @throws Exception
	 */
	public byte[] decryptWithPrivateKey(byte[] encryptedData) throws Exception {
		// Create a cipher object
		Cipher cipher = Cipher.getInstance("RSA");
		// Initialize the cipher object
		cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
		return cipher.doFinal(encryptedData);
	}

}
