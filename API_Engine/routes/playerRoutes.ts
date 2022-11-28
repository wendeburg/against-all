import { Router } from "express";
import { getPlayerList, getSinglePlayer } from '../controllers/playerController';


let playerRouter = Router();

playerRouter.get("/", getPlayerList);

playerRouter.get("/:playerid", getSinglePlayer);

export default playerRouter;