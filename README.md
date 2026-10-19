# Shatranj (Chess Application)

**Course:** Comprehensive Design of Data Structures  
**Group:** Group 1  
**Language:** Java (JDK 17)  
**UI Framework:** JavaFX (FXML & CSS)

## Project Description
Shatranj is a fully functional chess application built with JavaFX. It features a Minimax AI with Alpha-Beta pruning, adjustable difficulty levels, premove capability, a real-time clock, and a fully separated MVC architecture.

## Requirements
To run this application, you must have the following installed:
* **Java Development Kit (JDK):** Version 17 or higher.
* **Apache Maven:** Version 3.6 or higher.
* **IDE:** IntelliJ IDEA

## Dependencies
The project uses Maven to manage dependencies automatically. The required libraries are defined in the `pom.xml` file:
* `org.openjfx:javafx-controls:17.0.6`
* `org.openjfx:javafx-fxml:17.0.6`
* `org.openjfx:javafx-maven-plugin:0.0.8`

## How to Run the Application

### Method 1: Using IntelliJ IDEA
1. Open IntelliJ IDEA.
2. Click **File → Open** and select the root folder of this project (the folder containing `pom.xml`).
3. Wait for IntelliJ to index the files. If prompted, click "Load Maven Changes" or click the **Reload All Maven Projects** button in the Maven tool window on the right side.
4. Open the Maven tool window (usually on the right side of the screen).
5. Expand the chess project → expand Plugins → expand javafx.
6. Double-click `javafx:run` to launch the application.

### Method 2: Using the Terminal
1. Open your terminal or command prompt.
2. Navigate to the root directory of the project (where `pom.xml` is located).
3. Run the following command:
   ```bash
   mvn clean javafx:run# SHATRANJ-Chess-App
A learning project: building a chess app from scratch.
