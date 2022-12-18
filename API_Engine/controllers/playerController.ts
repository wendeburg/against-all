import { INSPECT_MAX_BYTES } from "buffer";
import { Request, Response } from "express";
import { getDBClientAndGameState } from "./utils";

async function getPlayerList(req: Request, res: Response) {
    const { mongoClient, gameStateObj, errorMessage} = await getDBClientAndGameState();

    try {
        if (gameStateObj != null) {
            const players = gameStateObj['jugadores'];

            for (const key in players) {
                delete players[key].efectoFrio;
                delete players[key].efectoCalor;
                delete players[key].isNPC;
                delete players[key].token;
            }

            let resObj: any = {success: true, players: players};

            if (gameStateObj['movementsLogger'] !== undefined) {
                resObj["movementsLogger"] = gameStateObj['movementsLogger'];
            }

            res.status(200).json(resObj);
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

async function getSinglePlayer(req: Request, res: Response) {
    const { mongoClient, gameStateObj, errorMessage } = await getDBClientAndGameState();

    try {
        if (gameStateObj != null) {
            const player = gameStateObj['jugadores'][req.params.playerid];

            if (player == null) {
                res.status(200).json({success: false, message: "No information found for the requested player."});
            }
            else {
                res.status(200).json({success: true, player: player});
            }
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
    getPlayerList,
    getSinglePlayer,
}
