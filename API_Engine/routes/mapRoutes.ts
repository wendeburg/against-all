import { Router } from "express";
import { getMap } from '../controllers/mapController';

let mapRouter = Router();

mapRouter.get("/", getMap);

export default mapRouter;