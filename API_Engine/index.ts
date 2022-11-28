import express, { Express, Request, Response } from 'express';
import { connect, ConnectOptions } from 'mongoose';
import cors from 'cors';

import playerRouter from './routes/playerRoutes';
import mapRouter from './routes/mapRoutes';
import npcRouter from './routes/npcRoutes';
import cityRouter from './routes/cityRoutes';
import gameStateRouter from './routes/gameStateRoutes';

const port = process.argv[2];
const db_ip = process.argv[3];
const db_port = process.argv[4];

const app: Express = express();

// DB connection
(async function() {
  try {
    await connect("mongodb://" + db_ip + ":" + db_port as string, {
      useNewUrlParser: true,
      useUnifiedTopology: true
    } as ConnectOptions);
  }
  catch (error) {
    console.log("Database connection failed! - " + error);
  }
})();

// Middleware
app.use(cors);
app.use(express.json());
app.use(express.urlencoded({ extended: false }));

// Routes
app.use("/map", mapRouter);
app.use("/players", playerRouter);
app.use("/npcs", npcRouter);
app.use("/city", cityRouter);
app.use("/gamestate", gameStateRouter)

// Si no se encuentra el endpoint -> 404.
app.use(function (req: Request, res: Response) {
  res.sendStatus(404);
});

app.listen(port, () => {
  console.log(`⚡️[server]: Server is running at http://localhost:${port}`);
});