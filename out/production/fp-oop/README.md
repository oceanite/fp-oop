# Java Messenger Application

## Overview

This is a real-time, multi-user messaging application built in Java, featuring a client-server architecture that allows users to communicate through direct messages, group broadcasts, and file sharing.

## System Architecture

The application consists of three main components:

1. **Server (Server.java)**: Manages client connections, message routing, and system logging
2. ** Client Handler (ClientHandler.java)**: Manages individual client connections and message processing
3. **Client (Client.java)**: Provides the user interface for sending and receiving messages

## Key Features

### Communication Modes

1. **Broadcast Messaging**

   - Send messages to all connected users
   - Real-time message distribution
   - Instant notification when users join or leave the server

2. Direct Messaging

   - Send private messages to specific users
   - Targeted communication without disturbing other users
   - Recipient-specific message routing

3. File Transfer

   - Send files directly to specific users
   - Supports various file types and sizes
   - User-friendly file selection and saving process

### User Interface

- Clean, intuitive Swing-based graphical interface
- Resizable message area
- Color-coded buttons for different actions
- Username-based identification

### Networking

- Localhost-based server connection
- Concurrent client handling
- Robust error management and connection handling

## Technical Details

### Server Capabilities

- Port 5000 for client connections
- Concurrent client management
- Server-side logging of events
- Client list management
- Timestamp tracking for connections and messages

### Logging

- Comprehensive logging to server_logs.txt
- Tracks:

  - User connections
  - Disconnections
  - Direct messages
  - File transfers

### User Workflow

1. Launch server application
2. Start client application
3. Enter unique username
4. Choose communication method:
   - Broadcast message
   - Direct message to specific user
   - Send file to specific user
   - List connected clients

### Technical Implementation Highlights

- Java Swing for GUI
- Socket programming for network communication
- Multithreading for concurrent connections
- Stream-based file transfer
- Error handling and graceful disconnection

### Potential Improvements

1. Encryption for message and file transfer
2. User authentication
3. Persistent message history
4. Support for multiple chat rooms
5. Enhanced file transfer with progress tracking

### Technical Requirements

- Java Development Kit (JDK) 8 or higher
- No external libraries required beyond standard Java libraries

### Security Considerations

- Currently operates on localhost
- No built-in encryption
- Recommended for local network or trusted environments

### Code Structure

- Package: `messenger.server` for server-side components
- Package: `messenger.client` for client-side components
- Modular design allowing easy extension and modification

### Limitations

- Single server instance
- No persistent user accounts
- Limited to text and file communication
- Localhost-only communication

### Development and Deployment

1. Compile server and client classes
2. Start server application
3. Launch multiple client instances
4. Begin messaging

### Sample Use Cases

- Local team communication
- Small office messaging
- Educational network programming demonstration
- Prototype for more advanced messaging systems

Licensing
Open-source project for educational and personal use.
Would you like me to elaborate on any specific aspect of the application? I can provide more technical details, explain the code structure, or discuss potential future enhancements.
