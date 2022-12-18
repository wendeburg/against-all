import { Request, Response } from "express";
import { getDBClientAndGameState } from "./utils";

async function getGameState(req: Request, res: Response) {
    const { mongoClient, gameStateObj, errorMessage } = await getDBClientAndGameState();

    try {
        if (gameStateObj != null) {
            res.status(200).json({success: true, gamefinished: gameStateObj['gamefinished'], winners: gameStateObj['winners']});
        }
        else {
            res.status(500).json({errorMessage: errorMessage});
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
