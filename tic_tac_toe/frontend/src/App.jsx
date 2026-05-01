import { useEffect, useState } from 'react';

const API_BASE = '/api/game';
const EMPTY_BOARD = Array(9).fill(' ');

function getStatusText(game) {
  if (!game) {
    return 'Loading game';
  }

  if (game.status === 'HUMAN_WON') {
    return 'You won';
  }

  if (game.status === 'AI_WON') {
    return 'AI won';
  }

  if (game.status === 'DRAW') {
    return 'Draw';
  }

  return 'Game in progress';
}

function canPlayInCell(game, index, isSubmitting) {
  if (!game) {
    return false;
  }

  if (isSubmitting) {
    return false;
  }

  if (game.status !== 'IN_PROGRESS') {
    return false;
  }

  return game.board[index] === ' ';
}

function StatsCard({ label, value, highlight = false }) {
  const cardClassName = highlight ? 'stat-card accent' : 'stat-card';

  return (
    <article className={cardClassName}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function Board({ board, game, isLoading, isSubmitting, onMove }) {
  return (
    <div className="board">
      {board.map((cell, index) => {
        const disabled = isLoading || !canPlayInCell(game, index, isSubmitting);
        const cellClassName = cell === ' ' ? 'cell' : 'cell filled';

        return (
          <button
            key={index}
            className={cellClassName}
            disabled={disabled}
            onClick={() => onMove(index)}
          >
            {cell}
          </button>
        );
      })}
    </div>
  );
}

function App() {
  const [game, setGame] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    startNewGame();
  }, []);

  async function fetchJson(url, options) {
    const response = await fetch(url, options);

    if (!response.ok) {
      const problem = await response.json().catch(() => null);
      const message = problem?.detail || 'Something went wrong.';
      throw new Error(message);
    }

    return response.json();
  }

  async function startNewGame() {
    setIsLoading(true);
    setErrorMessage('');

    try {
      const newGame = await fetchJson(`${API_BASE}/new`, { method: 'POST' });
      setGame(newGame);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsLoading(false);
    }
  }

  async function playMove(position) {
    if (!canPlayInCell(game, position, isSubmitting)) {
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      const updatedGame = await fetchJson(`${API_BASE}/${game.sessionId}/move`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ position })
      });

      setGame(updatedGame);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setIsSubmitting(false);
    }
  }

  const board = game ? game.board : EMPTY_BOARD;
  const statusText = getStatusText(game);
  const messageText = isLoading ? 'Starting a new game...' : game?.message;
  const stats = game?.agentStats || {
    learnedStates: 0,
    gamesPlayed: 0,
    aiWins: 0,
    humanWins: 0,
    draws: 0
  };

  return (
    <main className="page-shell">
      <section className="hero-card">
        <div className="hero-copy">
          <p className="eyebrow">Adaptive Java + React demo</p>
          <h1>Play tic-tac-toe against an AI that learns from you.</h1>
          <p className="description">
            Each finished match updates the agent&apos;s board-state scores and saves them to disk,
            so the opponent gradually improves as you keep playing.
          </p>
        </div>

        <div className="panel">
          <div className="panel-header">
            <div>
              <p className="panel-label">Current match</p>
              <h2>{statusText}</h2>
            </div>

            <button
              className="secondary-button"
              onClick={startNewGame}
              disabled={isLoading || isSubmitting}
            >
              New game
            </button>
          </div>

          <p className="message">{messageText}</p>

          {errorMessage ? <p className="error-banner">{errorMessage}</p> : null}

          <Board
            board={board}
            game={game}
            isLoading={isLoading}
            isSubmitting={isSubmitting}
            onMove={playMove}
          />
        </div>
      </section>

      <section className="stats-grid">
        <StatsCard label="Learned states" value={stats.learnedStates} highlight />
        <StatsCard label="Games played" value={stats.gamesPlayed} />
        <StatsCard label="AI wins" value={stats.aiWins} />
        <StatsCard label="Your wins" value={stats.humanWins} />
        <StatsCard label="Draws" value={stats.draws} />
      </section>
    </main>
  );
}

export default App;
