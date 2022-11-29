import { Router } from "express";
import { getGameState } from '../controllers/gameStateController';

let gameStateRouter = Router();

gameStateRouter.get("/", getGameState);

export default gameStateRouter;