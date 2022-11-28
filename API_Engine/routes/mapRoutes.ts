import { Router } from "express";
import { getMap } from '../controllers/mapController';

let mapRouter = Router();

mapRouter.get("/map", getMap);

export default mapRouter;