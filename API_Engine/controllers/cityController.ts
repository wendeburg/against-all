import { Request, Response } from "express";
import { getDBClientAndGameState } from "./utils";

async function getCityList(req: Request, res: Response) {
    const { mongoClient, gameState } = await getDBClientAndGameState();

    try {
        if (gameState != null) {
            res.status(200).json({success: true, cities: gameState['ciudades']});
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

async function getSingleCity(req: Request, res: Response) {
    const { mongoClient, gameState } = await getDBClientAndGameState();

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
    getCityList,
    getSingleCity
}