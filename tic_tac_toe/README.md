# Learning Tic-Tac-Toe

This project contains:

- A Java Spring Boot backend that manages games and trains a simple reinforcement-learning agent.
- A React + Vite frontend that is built into the Spring Boot application and served by it.
- File-based persistence so the AI keeps what it learns between runs.

## How the learning works

The AI uses a lightweight Q-learning style update:

- Each time the AI makes a move, it stores the board state and move chosen.
- When the game ends, the result becomes a reward:
  - `+1.0` for an AI win
  - `-1.0` for a human win
  - `+0.35` for a draw
- The reward is pushed backward through the AI's move history with a discount factor.
- Learned state values are saved in `backend/data/q-values.json`.

This is intentionally small and easy to understand, but it still improves over time as the user keeps playing.

## Run as one application

```bash
cd backend
mvn spring-boot:run
```

This single command:

- installs frontend dependencies
- builds the React UI
- copies the built files into Spring Boot static resources
- starts one application on `http://localhost:8080`

Open `http://localhost:8080` in the browser to play the game.

## Build a runnable jar

```bash
cd backend
mvn clean package
java -jar target/tictactoe-learning-0.0.1-SNAPSHOT.jar
```

The packaged jar serves both the API and the UI on `http://localhost:8080`.

## API endpoints

- `POST /api/game/new` creates a new game session.
- `POST /api/game/{sessionId}/move` plays the human move and triggers the AI response.
- `GET /api/game/{sessionId}` returns the current board.
- `GET /api/game/stats` returns overall learning stats.
