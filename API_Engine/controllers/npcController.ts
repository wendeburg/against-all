import { Request, Response } from "express";
import { getDBClientAndGameState } from "./utils";

async function getNPCList(req: Request, res: Response) {
    const { mongoClient, gameStateObj, errorMessage } = await getDBClientAndGameState();

    try {
        if (gameStateObj != null) {

            const npcs = gameStateObj['npcs'];

            for (const key in npcs) {
                delete npcs[key].efectoFrio;
                delete npcs[key].efectoCalor;
                delete npcs[key].isNPC;
                delete npcs[key].token;
            }

            res.status(200).json({success: true, npcs: npcs});
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

async function getSingleNPC(req: Request, res: Response) {
    const { mongoClient, gameStateObj, errorMessage } = await getDBClientAndGameState();

    try {
        if (gameStateObj != null) {
            const npc = gameStateObj['npcs'][req.params.npcid];

            if (npc == null) {
                res.status(200).json({success: false, message: "No information found for the requested NPC."});
            }
            else {
                res.status(200).json({success: true, npc: npc});
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
    getNPCList,
    getSingleNPC
}