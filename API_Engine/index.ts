import https from 'https';
import fs from 'fs';
import path from 'path';

import express, { Express, Request, Response } from 'express';
import cors from 'cors';

import playerRouter from './routes/playerRoutes';
import mapRouter from './routes/mapRoutes';
import npcRouter from './routes/npcRoutes';
import cityRouter from './routes/cityRoutes';
import gameStateRouter from './routes/gameStateRoutes';
import { getDBClientAndGameState } from './controllers/utils';

const port = parseInt(process.argv[2]);

const app: Express = express();

// Middleware
app.use(cors())
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

// Routes
app.get("/", async function (req: Request, res: Response) {
    const { mongoClient, gameState } = await getDBClientAndGameState();

    try {
        if (gameState != null) {
            res.status(200).json({success: true, idpartida: gameState['idpartida']});
        }
        else {
            res.sendStatus(500);
        }
    }
    finally {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
});

app.use("/map", mapRouter);
app.use("/players", playerRouter);
app.use("/npcs", npcRouter);
app.use("/city", cityRouter);
app.use("/gamestate", gameStateRouter);

// Si no se encuentra el endpoint -> 404
app.use(function (req: Request, res: Response) {
    res.sendStatus(404);
});

const SSLPath = path.join(__dirname, 'secrets');

const SSLConfig = {
    key: fs.readFileSync(SSLPath + "/api-engine.0.key.pem"),
    cert: fs.readFileSync(SSLPath + "/api-engine.0.certificate.pem"),
    ca: fs.readFileSync(SSLPath + "/api-engine.CARoot.pem"),
}

https.createServer(SSLConfig, app).listen(port, () => {
    console.log(`⚡️[server]: Server is running at https://localhost:${port}`);
});