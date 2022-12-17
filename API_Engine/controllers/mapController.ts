import { Request, Response } from "express";
import { getDBClientAndGameState } from "./utils";

async function getMap(req: Request, res: Response) {
    const { mongoClient, gameStateObj } = await getDBClientAndGameState();

    try {
        if (gameStateObj != null) {
            res.status(200).json({success: true, map: gameStateObj['mapa'] });
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
}

export {
    getMap
}