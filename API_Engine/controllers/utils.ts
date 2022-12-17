import { MongoClient } from 'mongodb';
import crypto from 'crypto';
import { readFileSync } from 'fs';
import path from 'path';

const db_ip = process.argv[3];
const db_port = process.argv[4];

async function getDBClientAndGameState() {
    try {
        const filePath = path.dirname(__filename).split(path.sep);
        filePath.pop();

        const secrets = readFileSync(filePath.reduce(
            (accumulador, currVal) => accumulador += currVal + "/"
        , "").slice(0, -1) + "/secrets/decryption_password").toString('ascii').split(":");

        const key = secrets[0];
        const iv = secrets[1];

        const mongoClient = new MongoClient("mongodb://" + db_ip + ":" + db_port);
        const database = mongoClient.db('against-all-db');
        const collection = database.collection('latest-map');
    
        const gameState = await collection.findOne({}, {sort:{_id:-1}});

        const decipher = crypto.createDecipheriv('des-ede3-cbc', key, iv);

        let datosDesencriptados = decipher.update(gameState!["matchData"], 'base64');
        datosDesencriptados += decipher.final();

        let gameStateObj = JSON.parse(datosDesencriptados.toString());

        return {mongoClient, gameStateObj}
    }
    catch (err) {
        console.log(err)
        console.log("❌[server]: An error has ocurred while connecting to the data base. The request could not be fulfilled.");
    }

    return {mongoClient: null, gameState: null}
}

export {
    getDBClientAndGameState
}