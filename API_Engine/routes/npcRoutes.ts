import { Router } from "express";
import { getNPCList, getSingleNPC } from '../controllers/npcController';


let playerRouter = Router();

playerRouter.get("/", getNPCList);

playerRouter.get("/:npcid", getSingleNPC);

export default playerRouter;