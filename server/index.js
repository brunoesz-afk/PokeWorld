const express = require("express");
const http = require("http");
const { Server } = require("socket.io");

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});

const PORT = process.env.PORT || 3000;
const players = {};

app.get("/", (req, res) => {
    res.send("Servidor Multiplayer do PokeWorld está rodando online!");
});

io.on("connection", (socket) => {
    console.log(`[+] Jogador conectado: ${socket.id}`);

    socket.on("join", (data) => {
        players[socket.id] = {
            id: socket.id,
            name: data.name || "Treinador",
            mapName: "CITY",
            x: 0,
            y: 0,
            direction: 0,
            frameIndex: 1,
            skinColor: -1,
            isMounted: false,
            petSpecies: "",
            petX: 0,
            petY: 0,
            petDirection: 0,
            petFrameIndex: 1,
            isShiny: false
        };

        // Envia todos os jogadores atualmente conectados para o novo jogador
        socket.emit("currentPlayers", players);

        // Notifica outros jogadores sobre o novo jogador
        socket.broadcast.emit("playerJoined", players[socket.id]);
    });

    socket.on("move", (data) => {
        if (players[socket.id]) {
            players[socket.id] = {
                ...players[socket.id],
                ...data
            };
            // Retransmite os dados de movimento para os outros jogadores
            socket.broadcast.emit("playerMoved", players[socket.id]);
        }
    });

    socket.on("disconnect", () => {
        console.log(`[-] Jogador desconectado: ${socket.id}`);
        delete players[socket.id];
        io.emit("playerLeft", socket.id);
    });
});

server.listen(PORT, () => {
    console.log(`Servidor rodando na porta ${PORT}`);
});
