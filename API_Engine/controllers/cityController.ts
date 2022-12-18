import { Request, Response } from "express";
import { getDBClientAndGameState } from "./utils";

async function getCityList(req: Request, res: Response) {
    const { mongoClient, gameStateObj, errorMessage } = await getDBClientAndGameState();

    try {
        if (gameStateObj != null) {
            res.status(200).json({success: true, cities: gameStateObj['ciudades']});
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

async function getSingleCity(req: Request, res: Response) {
    const { mongoClient, gameState, errorMessage } = await getDBClientAndGameState();

    try {
        if (gameState != null) {
            const temperatura = gameState['ciudades'][req.params.cityname];

            if (temperatura == null) {
                res.status(200).json({success: false, message: "No information found for the requested city."});
            }
            else {
                res.status(200).json({success: true, city: req.params.cityname, weather: temperatura});
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
    getCityList,
    getSingleCity
}