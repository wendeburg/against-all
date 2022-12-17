import { Request, Response } from "express";
import { getDBClientAndGameState } from "./utils";

async function getGameState(req: Request, res: Response) {
    const { mongoClient, gameStateObj } = await getDBClientAndGameState();

    try {
        if (gameStateObj != null) {
            res.status(200).json({success: true, gamefinished: gameStateObj['gamefinished'], winners: gameStateObj['winners']});
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
    getGameState
}
