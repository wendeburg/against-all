import { Router } from "express";
import { getCityList, getSingleCity } from '../controllers/cityController';

let cityRouter = Router();

cityRouter.get("/", getCityList);

cityRouter.get("/:cityname", getSingleCity);

export default cityRouter;