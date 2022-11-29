import { Request, Response } from "express";
import { getDBClientAndGameState } from "./utils";

async function getGameState(req: Request, res: Response) {
    const { mongoClient, gameState } = await getDBClientAndGameState();

    try {
        if (gameState != null) {
            res.status(200).json({success: true, gamestate: gameState['gamefinished']});
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
