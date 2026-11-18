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
    res.send("Servidor Multiplayer do PokeWorld está rodando online com Co-op na Torre!");
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

        socket.emit("currentPlayers", players);
        socket.broadcast.emit("playerJoined", players[socket.id]);
    });

    socket.on("move", (data) => {
        if (players[socket.id]) {
            players[socket.id] = {
                ...players[socket.id],
                ...data
            };
            socket.broadcast.emit("playerMoved", players[socket.id]);
        }
    });

    // --- SISTEMA DE CO-OP NA TORRE ---
    socket.on("inviteTower", (data) => {
        const targetId = data.targetId;
        if (targetId) {
            io.to(targetId).emit("towerInvitation", {
                fromId: socket.id,
                fromName: players[socket.id] ? players[socket.id].name : "Treinador",
                floor: data.floor || 1
            });
        }
    });

    socket.on("acceptTower", (data) => {
        const hostId = data.hostId;
        if (hostId) {
            io.to(hostId).emit("towerAccepted", {
                friendId: socket.id,
                friendName: players[socket.id] ? players[socket.id].name : "Treinador"
            });
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
