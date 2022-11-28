import { Router } from "express";
import { getGameState } from '../controllers/gameStateController';

let gameStateRouter = Router();

gameStateRouter.get("/map", getGameState);

export default gameStateRouter;