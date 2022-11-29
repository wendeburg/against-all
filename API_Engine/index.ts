import express, { Express, Request, Response } from 'express';
import cors from 'cors';

import playerRouter from './routes/playerRoutes';
import mapRouter from './routes/mapRoutes';
import npcRouter from './routes/npcRoutes';
import cityRouter from './routes/cityRoutes';
import gameStateRouter from './routes/gameStateRoutes';

const port = parseInt(process.argv[2]);

const app: Express = express();

// Middleware
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

// Routes
app.use("/map", mapRouter);
app.use("/players", playerRouter);
app.use("/npcs", npcRouter);
app.use("/city", cityRouter);
app.use("/gamestate", gameStateRouter)

// Si no se encuentra el endpoint -> 404
app.use(function (req: Request, res: Response) {
  res.sendStatus(404);
});

app.listen(port, () => {
  console.log(`⚡️[server]: Server is running at http://localhost:${port}`);
});