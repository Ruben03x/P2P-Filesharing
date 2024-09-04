# Project5 README

## Overview

Project5 is a Java-based client-server application that securely transfers files between clients using socket communication and encryption.

## Requirements

- Java Development Kit (JDK) 8 or later
- Apache Maven

## Directory Structure

```
Project5/
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── project5/
│   │               ├── Client.java
│   │               ├── ClientService.java
│   │               ├── InteractController.java
│   │               └── Server.java
├── pom.xml
└── README.md
```

## Build Instructions

### Prerequisites

Ensure you have Maven installed and configured properly. Check your Maven installation by running:

```sh
mvn -v
```

### Commands

Use the `Makefile` for building and running the project.

```makefile
MVN = mvn
MVN_FLAGS = -B

.PHONY: clean compile run-client run-server

build:
	$(MVN) $(MVN_FLAGS) clean install

compile:
	$(MVN) $(MVN_FLAGS) compile

run-client:
	$(MVN) $(MVN_FLAGS) javafx:run

run-server:
	$(MVN) $(MVN_FLAGS) exec:java@exec-java1

clean:
	$(MVN) $(MVN_FLAGS) clean
```

### Build the Project

```sh
make build
```

### Compile the Project

```sh
make compile
```

### Run the Client

```sh
make run-client
```

### Run the Server

```sh
make run-server
```

### Clean the Project

```sh
make clean
```

## Classes Overview

### Client.java

- **Constructor:** Initializes the client socket, streams, and key pairs.
- **sendMessage(String message):** Encrypts and sends a message.
- **receiver():** Listens for messages from the server.
- **disconnect():** Sends a disconnect message and closes streams.
- **startDownloadServer():** Starts a server socket for file downloads.
- **startDownloader(String selectedFile):** Initiates file download.
- **uploadFile(String fileName, String downloaderAddress, int downloaderPort, String messageKey):** Uploads a file.

### ClientService.java

- **setCurrentClient(Client client):** Sets the current client instance.
- **getCurrentClient():** Retrieves the current client instance.
- **setCurrentController(InteractController controller):** Sets the current controller instance.
- **getCurrentController():** Retrieves the current controller instance.

### Server.java

- **Constructor:** Initializes the server socket.
- **main(String[] args):** Starts the server and listens for connections.
- **startServerSocket():** Accepts client connections and starts new threads.

### InteractController.java

- **handleSignIn(ActionEvent event):** Manages sign-in.
- **handleSearch(ActionEvent event):** Manages file search.
- **handleDownload(ActionEvent event):** Manages file download.
- **handlePause(ActionEvent event):** Manages pause/resume of downloads.
- **updateFileList(ArrayList<String> users):** Updates the file list view.
- **updateProgressBarDownload(long bytesRead, long fileSize):** Updates download progress bar.
- **updateProgressBarUpload(long bytesRead, long fileSize):** Updates upload progress bar.
- **showErrorDialog(String message):** Displays an error dialog.
- **showDialog(String message):** Displays an information dialog.
- **getSelectedFile():** Retrieves the selected file from the list view.
- **initialize():** Initializes the controller.